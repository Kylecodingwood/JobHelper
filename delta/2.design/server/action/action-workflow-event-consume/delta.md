# action-workflow-event-consume

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/cross-domain-events.md`](../../../1.req/cross-domain-events.md)、[`../../../1.req/action/workflow-desc.md`](../../../1.req/action/workflow-desc.md)
- 服务：[`../action-crud/delta.md`](../action-crud/delta.md)
- 图示：[`workflow-action-event-consume.puml`](workflow-action-event-consume.puml)
- 实现：Spring `@Transactional` listener 或 Outbox poller；consumer=`action-domain`

## 1. 职责

**Action 域唯一维护 Home Actions**。消费各源域最小事件目录，幂等创建/更新/关闭 Action；**Roadmap 可选并行消费同一 eventId，但不替代 Action**。

## 2. 事件信封

| 字段 | 用途 |
|---|---|
| `eventId` | 幂等键（+ consumer） |
| `sourceDomain` | JOB / ROADMAP / CV / BEHAVIORAL / PROFILE |
| `eventType` | 规则路由 |
| `sourceObjectId` | targetId |
| `sourceObjectVersion` | 乱序检测 |
| `occurredAt` | 审计 |
| `payload` | 最小摘要，无敏感正文 |

## 3. 幂等 Receipt 流程

```
1. INSERT receipt (RECEIVED) ON CONFLICT → 若已 PROCESSED 则 skip
2. IF sourceObjectVersion < lastProcessedVersion → IGNORED_STALE, return
3. 路由 eventType → ActionGenerationService
4. 写 ActionGenerationEvidence(eventId)
5. UPDATE receipt → PROCESSED
   失败 → FAILED + errorCode（可重试）
```

**约束**：
- 重复 `eventId` 不得重复创建 active Action。
- 低版本事件 **不回滚** 当前投影（不撤销 pin/ignore/complete）。
- 重试不得覆盖用户 `ActionDecision`。

## 4. M1 事件路由表

### 4.1 Job → Action

| eventType | payload 摘要 | Action 响应 | SOURCE_EVENT 关闭 |
|---|---|---|---|
| `JOB_CREATED` | status=New, canonicalJobId | 创建 `REVIEW_NEW_JOB` | — |
| `JOB_STATUS_CHANGED` | old/new status | 更新；关闭 REVIEW_NEW_JOB | 非 New |
| `JOB_GATE_NEEDS_CONFIRMATION` | gateDimension, gateStatus | 创建/更新 `RESOLVE_GATE` | Gate 确定或覆盖 |
| `JOB_DECISION_RECORDED` | decisionType, reason? | 关闭相关 Job Actions | 与 Gate/状态一致 |
| `JOB_ARCHIVED` / `JOB_EXPIRED` | reason | STALE 或 COMPLETED | — |

### 4.2 Roadmap → Action

| eventType | Action 响应 | 关闭 |
|---|---|---|
| `ROADMAP_TASK_ACTIONABLE` | `COMPLETE_ROADMAP_TASK` + deadline? | — |
| `ROADMAP_TASK_BLOCKED` | 更新 blocker；可能 BLOCKED | — |
| `ROADMAP_TASK_COMPLETED` | 关闭 COMPLETE_ROADMAP_TASK | Task COMPLETED |
| `ROADMAP_TASK_ARCHIVED` | STALE | — |
| `ROADMAP_RECOMPUTED` | 批量重算相关 Actions | 按 Task 状态 |

### 4.3 CV / Behavioral（M1 规则预留，M2/M3 启用）

按 cross-domain-events §4.3、§4.4 同表实现；consumer 路由已注册，无事件时不写 Action。

### 4.4 Profile

Profile **不直接**创建 Action；经 Roadmap `ROADMAP_*` 间接影响。

## 5. 生成与关闭细节

### REVIEW_NEW_JOB

- **创建**：`JOB_CREATED` 且 payload.status=New。
- **关闭 SOURCE_EVENT**：`JOB_STATUS_CHANGED` → Shortlisted/Ignored/Applied/Archived。
- **title 示例**：「查看新职位：{company} - {title}」
- **system suggestion**：rankTier=HIGH → 较低 suggestion 分（不自动 Shortlist）。

### RESOLVE_GATE

- **创建**：`JOB_GATE_NEEDS_CONFIRMATION`；focusKey=`gate:{gateDimension}`。
- **关闭**：Gate PASSED（重算事件）或 `JOB_DECISION_RECORDED` decisionType=GATE_OVERRIDE。
- **WORK_AUTH + 缺失 expectedStartDate**：explanation 引用 NEEDS_CONFIRMATION 事实。

### COMPLETE_ROADMAP_TASK

- **deadline**：payload.dueAt（有则写 action.deadline）。
- **BLOCKED**：`ROADMAP_TASK_BLOCKED` → ActionDependency + status=BLOCKED。

## 6. 优先级重算

每次 PROCESSED 后对受影响 actionId 调用 `ActionPriorityService.recalculate`（或 batch `recalculateAll`）。

顺序：**deadline → blocker → user pin → system suggestion**。

## 7. 失败与重试

| ReceiptStatus | 行为 |
|---|---|
| FAILED | 后台重试；指数退避 |
| IGNORED_STALE | 不处理 payload |
| PROCESSED | 重复投递 no-op |

处理失败时 **不得** DELETE Action 或 RESET pin/ignore。

## 8. 与 Home

- 消费完成后 Home 读 `GET /api/v1/home` 即得最新 Actions。
- New Jobs **不**经本 workflow；Job 域 `GET /api/v1/jobs/new` 独立投影。

## 9. FR

FR-ACT-001、002、005、006；cross-domain-events §3～§6。
