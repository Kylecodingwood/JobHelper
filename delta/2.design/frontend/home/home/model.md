# Model — Home

- 路由：`/`
- 状态：设计正文已完成
- 域实体权威：[`../../../../1.req/action/entity.md`](../../../../1.req/action/entity.md)

## 视图模型

### `HomePageViewModel`

| 字段 | 类型 | 说明 |
|---|---|---|
| `actions` | `HomeActionCardVM[]` | 优先 Action 列表 |
| `blockedActions` | `HomeActionCardVM[]` | BLOCKED 摘要（可选） |
| `newJobs` | `HomeNewJobCardVM[]` | New Job 条带 |
| `degradedDomains` | `SourceDomain[]` | 降级域列表 |
| `lastRefreshedAt` | ISO8601 | 上次成功刷新 |
| `actionLimit` | number | 默认 5 |

### `HomeActionCardVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `actionId` | UUID | |
| `title` | string | |
| `sourceDomain` | `JOB \| ROADMAP \| CV \| BEHAVIORAL` | |
| `actionKind` | string | 如 `REVIEW_NEW_JOB` |
| `status` | `OPEN \| BLOCKED \| COMPLETED \| IGNORED \| STALE` | **无 IN_PROGRESS** |
| `active` | boolean | |
| `pinned` | boolean | |
| `deadline` | ISO8601? | |
| `priorityBand` | `DEADLINE \| BLOCKER \| USER_PIN \| SYSTEM_SUGGESTION \| NORMAL` | |
| `primaryReasonSummary` | string | 最高生效因素一行摘要 |
| `targetRef` | `TargetRefVM` | 导航用 |
| `version` | integer | 乐观并发 |
| `blockingSummary` | string? | BLOCKED 时 |
| `canNavigate` | boolean | STALE=false |

### `TargetRefVM`

| 字段 | 类型 |
|---|---|
| `targetType` | string |
| `targetId` | string |
| `routeHint` | string? |
| `focusKey` | string? |

### `HomeActionPriorityVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `actionId` | UUID | |
| `calculationId` | UUID | |
| `calculatedAt` | ISO8601 | |
| `ruleVersion` | string | |
| `evidences` | `PriorityEvidenceVM[]` | 按 `effectiveOrder` 排序 |
| `isStale` | boolean | 依据是否过期 |

### `PriorityEvidenceVM`

| 字段 | 类型 |
|---|---|
| `factor` | `DEADLINE \| BLOCKER \| USER_PIN \| SYSTEM_SUGGESTION` |
| `factValue` | string |
| `explanation` | string |
| `effectiveOrder` | integer |
| `sourceLabel` | string |

### `HomeNewJobCardVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `jobId` | UUID | |
| `title` | string | |
| `companyName` | string | |
| `location` | string | |
| `rankTier` | `HIGH \| MEDIUM \| LOW` | 只读展示 |
| `status` | `NEW` | 固定 New 投影 |
| `firstSeenAt` | ISO8601 | |

### `ActionDecisionRequestVM`

| 字段 | 类型 | 约束 |
|---|---|---|
| `operation` | `PIN \| UNPIN \| COMPLETE \| IGNORE \| RESTORE` | 必填 |
| `reason` | string? | ignore 时可选（设计默认） |
| `basedOnVersion` | integer | 必填 |

## 状态与枚举（前端）

```text
ActionStatus: OPEN | BLOCKED | COMPLETED | IGNORED | STALE
（禁止 IN_PROGRESS）

PriorityBand: DEADLINE > BLOCKER > USER_PIN > SYSTEM_SUGGESTION > NORMAL
```

## 映射规则

| API DTO | VM 字段 | 规则 |
|---|---|---|
| `ActionSummaryDTO` | `HomeActionCardVM` | `primaryReasonSummary` 取 `priorityEvidences[0].explanation` 或服务端预计算字段 |
| `ActionSummaryDTO.status=BLOCKED` | 列表分区 | 主列表排除；进入 `blockedActions` |
| `TargetRefDTO` | `TargetRefVM` | 直接映射；`canNavigate = status !== STALE` |

## 本地 UI 状态（不持久化）

| 键 | 说明 |
|---|---|
| `expandedBlockedSummary` | 阻塞区折叠 |
| `priorityDrawerActionId` | 当前打开抽屉 |
| `pendingDecision` | 待确认操作 |

## PlantUML

见 [`model.puml`](model.puml)。
