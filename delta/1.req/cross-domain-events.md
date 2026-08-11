# 跨域领域事件契约

- 状态：已细化（M1–M3）
- 权威需求：[../SRS/SRS.md](../SRS/SRS.md)
- Action 消费说明：[action/workflow-desc.md](action/workflow-desc.md)
- Roadmap 追加说明：[roadmap/workflow-desc.md](roadmap/workflow-desc.md)

## 1. 目的

各业务域发布**最小**领域事件目录；Action 域（以及 Roadmap 的任务追加流程）按契约幂等消费，驱动 Home 待办与 Roadmap 追加任务。**Home Actions 由 Action 域唯一拥有**；Roadmap 不得维护独立的 Action 实体或 Home 待办投影。

## 2. 事件信封

所有跨域事件使用统一信封；`payload` 仅含摘要事实，**不得**复制 CV 正文、Behavioral 答案全文、PII 或其他敏感正文。

| 字段 | 类型 | 约束 |
|---|---|---|
| `eventId` | string/UUID | 发布方生成，全局唯一 |
| `sourceDomain` | `JOB` / `ROADMAP` / `CV` / `BEHAVIORAL` / `PROFILE` | 必填 |
| `eventType` | string | 必填；见 §4 目录 |
| `sourceObjectId` | UUID/string | 必填；事件主对象 ID |
| `sourceObjectVersion` | string/integer | 必填；乱序与幂等判断 |
| `occurredAt` | timestamp | 必填 |
| `payload` | JSON object | 最小必要摘要，如状态枚举、计数、引用 ID、规则版本；无敏感正文 |

**示例 payload 摘要**（非完整 schema）：

```json
{
  "status": "New",
  "gateDimension": "LOCATION",
  "openSuggestionCount": 2,
  "answerStatus": "READY_FOR_FEEDBACK",
  "evidenceLinkCount": 3,
  "ruleVersion": "gate-v2"
}
```

## 3. 幂等与消费

- 每个消费者以 **`sourceDomain + eventId`** 为唯一键；重复投递不得重复创建 Action、Roadmap 追加任务或生成证据。
- 消费者记录 `DomainEventReceipt`（或等价 receipt），状态含 `RECEIVED` / `PROCESSED` / `FAILED` / `IGNORED_STALE`。
- `sourceObjectVersion` 低于该对象已处理版本时，标记 `IGNORED_STALE`，**不得**回滚当前投影。
- 处理失败可安全重试；不得因重试覆盖用户 pin、ignore 或 complete 决定。

## 4. 最小事件目录

以下为 M1–M3 必需的事件类型；各域可在实现中扩展，但 Action/Roadmap 只依赖本目录中的类型做确定性处理。

### 4.1 Job → Action（及可选 → Roadmap）

| eventType | payload 摘要 | Action 响应 | 关闭条件（SOURCE_EVENT） |
|---|---|---|---|
| `JOB_CREATED` | `status=New`, `canonicalJobId` | 创建 `REVIEW_NEW_JOB` | — |
| `JOB_STATUS_CHANGED` | 新/旧 `status` | 更新或关闭 `REVIEW_NEW_JOB` | 用户决定：`Shortlisted` / `Ignored` / `Applied` / `Archived` |
| `JOB_GATE_NEEDS_CONFIRMATION` | `gateDimension`, `gateStatus` | 创建/更新 `RESOLVE_GATE` | Gate 可确定或用户覆盖决定 |
| `JOB_DECISION_RECORDED` | `decisionType`, `reason?` | 关闭相关 Job Actions | 与 Gate/状态事件一致 |
| `JOB_ARCHIVED` / `JOB_EXPIRED` | `reason` | 关闭或标记相关 Action `STALE` | — |

Roadmap 可选消费：`JOB_STATUS_CHANGED`（如 Applied）→ 追加 `JOB_EVENT` 来源任务；不创建 Home Action。

### 4.2 Roadmap → Action

| eventType | payload 摘要 | Action 响应 | 关闭条件（SOURCE_EVENT） |
|---|---|---|---|
| `ROADMAP_TASK_ACTIONABLE` | `taskId`, `dueAt?`, `actionability=ACTIONABLE` | 创建/更新 `COMPLETE_ROADMAP_TASK` | — |
| `ROADMAP_TASK_BLOCKED` | `taskId`, `blockingTaskIds[]` | 更新 blocker 事实；可能 `BLOCKED` | — |
| `ROADMAP_TASK_COMPLETED` | `taskId`, `completedAt` | 关闭 `COMPLETE_ROADMAP_TASK` | Task `COMPLETED` |
| `ROADMAP_TASK_ARCHIVED` | `taskId` | 关闭或 `STALE` | Task `ARCHIVED` |
| `ROADMAP_RECOMPUTED` | `roadmapId`, `ruleVersion`, `changedTaskIds[]` | 批量重算相关 Actions | 按各 Task 状态 |

### 4.3 CV → Action（及可选 → Roadmap）

| eventType | payload 摘要 | Action 响应 | 关闭条件（SOURCE_EVENT） |
|---|---|---|---|
| `CV_REVIEW_HAS_OPEN_SUGGESTIONS` | `reviewId`, `openSuggestionCount` | 创建/更新 `REVIEW_CV_SUGGESTIONS` | — |
| `CV_SUGGESTION_DECIDED` | `suggestionId`, `decision` | 更新开放计数 | 无开放建议 |
| `CV_REVIEW_NO_OPEN_SUGGESTIONS` | `reviewId` | 关闭 `REVIEW_CV_SUGGESTIONS` | 全部已决定或 defer 且不再开放 |
| `CV_REVIEW_ARCHIVED` | `reviewId` | 关闭或 `STALE` | — |

Roadmap 可选消费：`CV_REVIEW_HAS_OPEN_SUGGESTIONS` → 追加 CV 相关准备任务。

### 4.4 Behavioral → Action（及可选 → Roadmap）

| eventType | payload 摘要 | Action 响应 | 关闭条件（SOURCE_EVENT） |
|---|---|---|---|
| `BEHAVIORAL_ANSWER_GAP_DETECTED` | `answerId`, `gapCodes[]` | 创建/更新 `COMPLETE_BEHAVIORAL_ANSWER` | — |
| `BEHAVIORAL_ANSWER_STATUS_CHANGED` | `answerId`, `status`, `evidenceLinkCount` | 更新或关闭 | `READY_FOR_FEEDBACK` 或更后状态 **且** `evidenceLinkCount > 0` |
| `BEHAVIORAL_ANSWER_ARCHIVED` | `answerId` | 关闭或 `STALE` | — |

Roadmap 可选消费：缺口或反馈就绪 → 追加 Behavioral 准备任务。

### 4.5 Profile → Roadmap 重算触发（间接 → Action）

Profile 域**不直接**创建 Action；发布重算触发事件，由 Roadmap 消费后可能再发布 `ROADMAP_*` 事件，Action 二次消费。

| eventType | payload 摘要 | 主要消费者 | 间接 Action 影响 |
|---|---|---|---|
| `PROFILE_TIMELINE_CHANGED` | `profileVersion`, `changedAnchors[]` | Roadmap | 经 `ROADMAP_RECOMPUTED` / Task 事件 |
| `PROFILE_GOALS_CHANGED` | `profileVersion`, `goalIds[]` | Roadmap | 同上 |
| `PROFILE_SKILLS_CHANGED` | `profileVersion` | Roadmap（可选） | 同上 |

## 5. 扇出与职责边界

```text
源域事件
  ├─> Action 域：唯一维护 Home Actions（创建 / 更新 / SOURCE_EVENT 关闭）
  └─> Roadmap 域（可选）：从部分事件追加 RoadmapTask，不替代 Action
```

- **Action 始终拥有 Home 待办**：无论 Roadmap 是否追加任务，用户首页可执行项只来自 Action 投影。
- **Roadmap 不得拥有独立 Action 实体**；任务完成仍通过 `ROADMAP_TASK_*` 事件由 Action 消费并关闭 `COMPLETE_ROADMAP_TASK`。
- 同一 `eventId` 可被 Action 与 Roadmap **分别**消费；各自以 `sourceDomain + eventId` 在自身 consumer 范围内幂等。
- Job `New` 列表投影可与 Action 并存：Home 独立展示 `New` Job，与 Action 优先级排序分离（FR-ACT-003）。

## 6. Action 关闭模式（摘要）

| 模式 | 触发 | 说明 |
|---|---|---|
| `SOURCE_EVENT` | 上表关闭条件满足 | 追加 `ActionGenerationEvidence`，设置 `completionMode=SOURCE_EVENT` |
| `USER_CONFIRMED` | 用户 `COMPLETE` 或 `IGNORE` | 追加 `ActionDecision`；不伪造源对象状态 |

用户 `RESTORE` 仅在有实质源变化或显式恢复规则时重新开放；不得静默撤销 `IGNORE`/`COMPLETE`。

## 7. 关联 FR

| 契约要点 | FR |
|---|---|
| 统一 Action、源追溯 | FR-ACT-001、002 |
| 源操作后同步完成 | FR-ACT-005 |
| 领域事件追加 Roadmap | FR-RDM-003、012 |
| 跨域 Action 可追溯 | SRS §8 不变量 9 |
