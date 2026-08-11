# action-model

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/action/entity.md`](../../../1.req/action/entity.md)、[`../../../1.req/cross-domain-events.md`](../../../1.req/cross-domain-events.md)
- 原则：**Home Actions 由 Action 域唯一拥有**；Roadmap 不得维护独立 Action 投影
- 图示：[`domain-action.puml`](domain-action.puml)

## 1. 包与表命名

| 逻辑实体 | PostgreSQL 表 | JPA 实体 |
|---|---|---|
| Action | `action_item` | `ActionItem` |
| TargetRef | `action_target_ref` | `ActionTargetRef`（嵌入或 1:1） |
| ActionGenerationEvidence | `action_generation_evidence` | `ActionGenerationEvidence` |
| ActionPriorityEvidence | `action_priority_evidence` | `ActionPriorityEvidence` |
| ActionDependency | `action_dependency` | `ActionDependency` |
| ActionDecision | `action_decision` | `ActionDecision` |
| DomainEventReceipt | `action_domain_event_receipt` | `DomainEventReceipt` |

## 2. 枚举

| 枚举 | 值 |
|---|---|
| `SourceDomain` | `JOB`, `ROADMAP`, `CV`, `BEHAVIORAL` |
| `ActionKind` | 见 §3 |
| `ActionStatus` | `OPEN`, `BLOCKED`, `COMPLETED`, `IGNORED`, `STALE` |
| `CompletionMode` | `SOURCE_EVENT`, `USER_CONFIRMED` |
| `PriorityBand` | `DEADLINE`, `BLOCKER`, `USER_PIN`, `SYSTEM_SUGGESTION`, `NORMAL` |
| `PriorityFactor` | `DEADLINE`, `BLOCKER`, `USER_PIN`, `SYSTEM_SUGGESTION` |
| `ActionOperation` | `PIN`, `UNPIN`, `COMPLETE`, `IGNORE`, `RESTORE` |
| `DependencyStatus` | `ACTIVE`, `RESOLVED`, `WAIVED` |
| `ReceiptStatus` | `RECEIVED`, `PROCESSED`, `FAILED`, `IGNORED_STALE` |
| `TargetType` | `CANONICAL_JOB`, `ROADMAP_TASK`, `CV_REVIEW`, `CV_SUGGESTION`, `BEHAVIORAL_ANSWER` |

## 3. actionKind（M1 受控枚举）

| actionKind | sourceDomain | targetType | 生成规则 ID（示例） |
|---|---|---|---|
| `REVIEW_NEW_JOB` | JOB | CANONICAL_JOB | `job-review-new-v1` |
| `RESOLVE_GATE` | JOB | CANONICAL_JOB | `job-resolve-gate-v1` |
| `COMPLETE_ROADMAP_TASK` | ROADMAP | ROADMAP_TASK | `rdm-complete-task-v1` |
| `REVIEW_CV_SUGGESTIONS` | CV | CV_REVIEW | `cv-review-suggestions-v1` |
| `COMPLETE_BEHAVIORAL_ANSWER` | BEHAVIORAL | BEHAVIORAL_ANSWER | `bhv-complete-answer-v1` |

**禁止**：`AUTO_APPLY`、`AUTO_SHORTLIST` 等会隐式改变 Job 状态的 kind。

## 4. 表结构

### 4.1 `action_item`

| 列 | 类型 | 约束 |
|---|---|---|
| `action_id` | UUID PK | |
| `source_domain` | SourceDomain | |
| `action_kind` | VARCHAR | |
| `title` | TEXT | 用户可读 |
| `status` | ActionStatus | |
| `active` | BOOLEAN | 关闭后 false |
| `deadline` | TIMESTAMPTZ? | 仅有事实依据 |
| `pinned` | BOOLEAN | 默认 false |
| `pinned_at` | TIMESTAMPTZ? | pinned 时必填 |
| `completion_mode` | CompletionMode? | COMPLETED 时必填 |
| `generation_rule_id` / `generation_rule_version` | VARCHAR | |
| `source_object_version` | VARCHAR | 幂等/乱序 |
| `priority_band` | PriorityBand | 派生 |
| `priority_sort_key` | VARCHAR | 确定性排序键 |
| `supersedes_action_id` | UUID? | 规则升级替代 |
| `version` | INT | 乐观锁 |
| `created_at` / `updated_at` / `completed_at` | TIMESTAMPTZ | |

**业务唯一索引**（partial unique）：

```sql
UNIQUE (source_domain, target_type, target_id, action_kind, generation_rule_version)
WHERE active = true
```

### 4.2 `action_target_ref`

| 列 | 类型 | 约束 |
|---|---|---|
| `action_id` | UUID PK/FK | 1:1 |
| `target_type` | TargetType | |
| `target_id` | UUID | |
| `route_hint` | VARCHAR? | 如 `/jobs/{id}` |
| `focus_key` | VARCHAR? | 如 `gate:WORK_AUTH` |

### 4.3 `action_generation_evidence`

追加式；同 eventId 不重复。

| 列 | 类型 |
|---|---|
| `generation_evidence_id` | UUID PK |
| `action_id` | UUID FK |
| `event_id` | VARCHAR |
| `event_type` | VARCHAR |
| `source_object_version` | VARCHAR |
| `rule_id` / `rule_version` | VARCHAR |
| `reason_code` | VARCHAR |
| `explanation` | TEXT |
| `facts` | JSONB |
| `created_at` | TIMESTAMPTZ |

唯一：`(action_id, event_id)` 或 consumer 范围 `(event_id, consumer)`。

### 4.4 `action_priority_evidence`

| 列 | 类型 |
|---|---|
| `priority_evidence_id` | UUID PK |
| `action_id` | UUID FK |
| `calculation_id` | UUID | 同次计算分组 |
| `factor` | PriorityFactor |
| `fact_value` | VARCHAR |
| `source_ref` | VARCHAR |
| `effective_order` | INT | 1=最高生效层 |
| `rule_version` | VARCHAR |
| `explanation` | TEXT |
| `calculated_at` | TIMESTAMPTZ |

**比较顺序固定**：deadline(1) → blocker(2) → user pin(3) → system suggestion(4)。

### 4.5 `action_dependency`

| 列 | 类型 |
|---|---|
| `dependency_id` | UUID PK |
| `blocked_action_id` | UUID FK |
| `blocking_action_id` | UUID FK? |
| `condition_ref` | VARCHAR? |
| `status` | DependencyStatus |
| `reason` | TEXT |
| `resolved_at` | TIMESTAMPTZ? |

约束：禁止自依赖；活动依赖 DAG 无环。

### 4.6 `action_decision`

| 列 | 类型 |
|---|---|
| `action_decision_id` | UUID PK |
| `action_id` | UUID FK |
| `operation` | ActionOperation |
| `reason` | TEXT? |
| `actor` | VARCHAR | `USER` |
| `based_on_version` | INT |
| `supersedes_decision_id` | UUID? |
| `created_at` | TIMESTAMPTZ |

### 4.7 `action_domain_event_receipt`

| 列 | 类型 |
|---|---|
| `receipt_id` | UUID PK |
| `event_id` | VARCHAR |
| `consumer` | VARCHAR | 固定 `action-domain` |
| `source_domain` | SourceDomain |
| `target_id` | VARCHAR | sourceObjectId |
| `source_version` | VARCHAR |
| `status` | ReceiptStatus |
| `error_code` | VARCHAR? |
| `received_at` / `processed_at` | TIMESTAMPTZ |

**幂等键**：`UNIQUE(consumer, source_domain, event_id)`。

## 5. 优先级投影

`priority_sort_key` 编码（示例）：

```
{bandOrder}-{deadlineEpoch|MAX}-{blockerScore|0}-{pinEpoch|0}-{suggestionScore|0}-{createdAt}-{actionId}
```

| bandOrder | PriorityBand |
|---|---|
| 1 | DEADLINE（逾期 < 未逾期） |
| 2 | BLOCKER |
| 3 | USER_PIN |
| 4 | SYSTEM_SUGGESTION |
| 5 | NORMAL |

同级：`deadline ASC` → `createdAt ASC` → `actionId ASC`。

## 6. 不变量

1. Action **不**直接修改 Job/Roadmap/CV/Behavioral 源状态。
2. 同一业务键最多一个 `active=true`。
3. 用户 `pin`/`ignore`/`complete` 不被系统重算静默撤销。
4. `deadline` 不得伪造；无事实则 NULL。
5. `BLOCKED` Action 不进 Home 默认可执行列表。
6. 目标不存在 → `STALE`，不 DELETE。

## 7. FR

FR-ACT-001～007；cross-domain-events §3～§6。
