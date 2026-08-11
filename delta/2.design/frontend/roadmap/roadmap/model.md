# Model — Roadmap

- 路由：`/roadmap`
- 状态：设计正文已完成
- 域实体权威：[`../../../../1.req/roadmap/entity.md`](../../../../1.req/roadmap/entity.md)

## 视图模型

### `RoadmapPageViewModel`

| 字段 | 类型 | 说明 |
|---|---|---|
| `roadmap` | `RoadmapSummaryVM?` | Active Roadmap |
| `tasksByPhase` | `Record<phase, RoadmapTaskRowVM[]>` | 时间线数据 |
| `unscheduledTasks` | `RoadmapTaskRowVM[]` | actionability=UNSCHEDULED |
| `actionabilityStats` | `{ actionable, blocked, unscheduled }` | 摘要条 |
| `pendingRecompute` | `RecomputeHintVM?` | 来自 Profile |
| `templateUpdateAvailable` | boolean | UC-RDM-007 |

### `RoadmapSummaryVM`

| 字段 | 类型 |
|---|---|
| `roadmapId` | UUID |
| `status` | `ACTIVE \| ARCHIVED` |
| `profileVersion` | integer |
| `ruleVersion` | string |
| `generatedAt` | ISO8601 |

### `RoadmapTaskRowVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `taskId` | UUID | |
| `logicalTaskKey` | string? | 模板任务键 |
| `title` | string | |
| `phase` | string | |
| `status` | `TODO \| IN_PROGRESS \| COMPLETED \| ARCHIVED` | **Roadmap 任务状态** |
| `actionability` | `ACTIONABLE \| BLOCKED \| UNSCHEDULED` | 派生，非 Action status |
| `dueAt` | ISO8601? | |
| `priority` | `CRITICAL \| HIGH \| MEDIUM \| LOW` | |
| `origin` | TaskOrigin enum | |
| `originRef` | `{ sourceDomain?, sourceObjectId? }` | 事件任务链接 |
| `userPinned` | boolean | |
| `userEdited` | boolean | |
| `blockingSummary` | string? | BLOCKED 时 |
| `hasDependencyOverride` | boolean | 完成带覆盖 |
| `version` | integer | PATCH 并发 |

### `RoadmapTaskDetailVM`

扩展 `RoadmapTaskRowVM` +

| 字段 | 类型 |
|---|---|
| `description` | string |
| `completionCriteria` | string |
| `completionEvidence` | string? |
| `dependencies` | `TaskDependencyVM[]` |
| `unmetDependencies` | `TaskDependencyVM[]` |

### `TaskDependencyVM`

| 字段 | 类型 |
|---|---|
| `predecessorTaskId` | UUID |
| `predecessorTitle` | string |
| `predecessorStatus` | TaskStatus |
| `satisfied` | boolean |

### `RoadmapTemplateVM`

| 字段 | 类型 |
|---|---|
| `templateId` | UUID |
| `name` | string |
| `templateType` | `SYSTEM \| USER` |
| `activeVersionId` | UUID |
| `latestVersion` | integer |

### `TemplatePreviewVM`

| 字段 | 类型 |
|---|---|
| `parsedTasks` | `{ templateTaskKey, title, phase, warnings[] }[]` |
| `parseErrors` | `{ line, message }[]` |
| `canConfirm` | boolean |

### `RoadmapGeneratePreviewVM`

| 字段 | 类型 |
|---|---|
| `anchorSnapshot` | `{ COURSE_START, GRADUATION, GRS, STAMP_EXPIRY }` |
| `ruleVersion` | string |
| `candidateTasks` | `RoadmapTaskPreviewRow[]` |
| `warnings` | string[] |

### `RoadmapRecomputePreviewVM`

| 字段 | 类型 |
|---|---|
| `mergeTasks` | `RoadmapTaskPreviewRow[]` |
| `protectedTaskIds` | UUID[] |
| `unchangedSummary` | string |

### `TemplateApplicationPreviewVM`

| 字段 | 类型 |
|---|---|
| `safeAdd` | `RoadmapTaskPreviewRow[]` |
| `protected` | `{ taskId, reason }[]` |
| `conflicts` | `{ templateTaskKey, reason }[]` |

## 枚举隔离说明

| 概念 | 枚举 | 域 |
|---|---|---|
| Roadmap Task 业务状态 | TODO/IN_PROGRESS/COMPLETED/ARCHIVED | Roadmap |
| 可执行性 | ACTIONABLE/BLOCKED/UNSCHEDULED | Roadmap 投影 |
| Home Action 状态 | OPEN/BLOCKED/COMPLETED/IGNORED/STALE | Action（不在本页编辑） |

## PlantUML

见 [`model.puml`](model.puml)。
