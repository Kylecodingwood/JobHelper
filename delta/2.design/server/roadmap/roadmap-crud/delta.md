# roadmap-crud

- 对齐：[`../../../1.req/roadmap/usecase-desc.md`](../../../1.req/roadmap/usecase-desc.md) UC-RDM-001～007
- 分层：Application → Domain（模板解析、锚点、DAG、actionability）→ Repository
- 状态：**设计完成**

## 1. 模板服务

### 1.1 `RoadmapTemplateQueryService`

| 方法 | 说明 | UC |
|---|---|---|
| `listTemplates()` | SYSTEM + USER 模板及 activeVersion 摘要 | UC-RDM-001 |
| `getTemplateVersion(versionId)` | 含解析后的 TemplateTaskDefinition[] | UC-RDM-001 |
| `getSystemTemplateUpdates()` | 比当前 Roadmap 所用版本更新的 SYSTEM 版本 | UC-RDM-007 |

### 1.2 `RoadmapTemplateCommandService`

| 方法 | 说明 | UC |
|---|---|---|
| `parseMarkdownPreview(MarkdownParseCommand)` | 行级校验（RDM-MD-*）；返回任务 DAG 预览 | UC-RDM-001 |
| `saveUserTemplateVersion(SaveUserTemplateCommand)` | 预览确认后保存不可变 USER 版本 | UC-RDM-001 |
| `seedSystemTemplateIfAbsent()` | 启动时导入 `system-template-v1.md` | UC-RDM-001 |

**MarkdownParseCommand**：`name`, `markdownContent`  
**SaveUserTemplateCommand**：`name`, `markdownContent`, `previewAccepted: true`

## 2. Roadmap 查询

### 2.1 `RoadmapQueryService`

| 方法 | 说明 | UC |
|---|---|---|
| `getActiveRoadmap()` | ACTIVE Roadmap + tasks + dependencies + overrides | UC-RDM-003 |
| `getTask(taskId)` | 单任务详情含 blocker 摘要 | UC-RDM-003 |
| `getActionabilitySummary()` | 各任务 actionability + blockers（本地投影） | UC-RDM-006 |

## 3. 生成与重算

### 3.1 `RoadmapGenerationService`

| 方法 | 说明 | UC |
|---|---|---|
| `buildGeneratePreview(GeneratePreviewCommand)` | 首次基线候选任务 + anchorSnapshot + previewToken | UC-RDM-002 |
| `confirmGenerate(ConfirmGenerateCommand)` | 原子创建 Roadmap/Task/Dependency/GenerationRecord | UC-RDM-002 |
| `buildMergePreview(MergePreviewCommand)` | Profile 重算：待更新 SYSTEM 未完成任务 | UC-RDM-002、UC-PRO-003 |
| `confirmMerge(ConfirmMergeCommand)` | 应用 merge；发布 ROADMAP_RECOMPUTED | UC-RDM-002 |

**GeneratePreviewCommand**：

- `profileVersion`
- `systemTemplateVersionId`
- `userTemplateVersionIds[]`（可选）

**流程要点**：

1. `ProfileAnchorPort.resolveAnchors(profileVersion)` → anchorSnapshot
2. 合并模板：USER 覆盖同 `templateTaskKey` 的 SYSTEM 定义
3. `dueAt = anchor + relativeOffset`；缺失锚点 → `UNSCHEDULED`
4. 校验 DAG → `previewToken` + `inputHash`

**MergePreviewCommand / ConfirmMergeCommand**：

- 输入 `profileVersion` 或 Profile 重算链路的 `previewToken`
- **仅**选中 `origin=SYSTEM_TEMPLATE` 且 `status IN (TODO, IN_PROGRESS)` 且 `userEdited=false` 的任务（pin/完成/归档/用户任务排除）
- 确认后更新 `dueAt` 及可合并字段；写 GenerationRecord(type=MERGE)

幂等：相同 `inputHash` 重复 confirm 返回既有结果。

### 3.2 `RoadmapTemplateApplicationService`

| 方法 | 说明 | UC |
|---|---|---|
| `previewTemplateUpdate(fromId, toId)` | SafeAdd / Protected / Conflict 分类 | UC-RDM-007 |
| `applyTemplateUpdate(ApplyTemplateUpdateCommand)` | 仅 SafeAdd；写 TemplateApplication | UC-RDM-007 |

## 4. 任务 CRUD

### 4.1 `RoadmapTaskCommandService`

| 方法 | 说明 | UC |
|---|---|---|
| `createUserTask(CreateTaskCommand)` | origin=USER_CREATED | UC-RDM-003 |
| `updateTask(UpdateTaskCommand)` | 设置 userEdited=true | UC-RDM-003 |
| `startTask(taskId)` | TODO → IN_PROGRESS | UC-RDM-003 |
| `completeTask(CompleteTaskCommand)` | 校验标准+依赖；或 DependencyOverride | UC-RDM-003、004 |
| `restoreTask(taskId)` | COMPLETED/ARCHIVED → TODO | UC-RDM-003 |
| `archiveTask(taskId)` | → ARCHIVED | UC-RDM-003 |
| `pinTask(taskId, pinned)` | userPinned | UC-RDM-003 |

**CompleteTaskCommand**：

- `taskId`, `completionEvidence?`
- `dependencyOverride?: { reason }` — 依赖未满足时

完成后：`ActionabilityEngine.recalculate()` + Outbox `ROADMAP_TASK_COMPLETED` 等。

## 5. 领域事件追加

### 5.1 `RoadmapEventAppendService`

| 方法 | 说明 | UC |
|---|---|---|
| `appendFromDomainEvent(DomainEventEnvelope)` | 幂等 receipt；追加 CV/JOB/BEHAVIORAL 任务 | UC-RDM-005 |

**映射**：

| sourceDomain | origin | 示例触发 |
|---|---|---|
| CV | CV_EVENT | CV_REVIEW_HAS_OPEN_SUGGESTIONS |
| JOB | JOB_EVENT | JOB_STATUS_CHANGED → Applied |
| BEHAVIORAL | BEHAVIORAL_EVENT | BEHAVIORAL_ANSWER_GAP_DETECTED |

重复 `(sourceDomain, eventId)` → 返回既有 `resultTaskId`，不重复追加。

## 6. 可执行性引擎

### 6.1 `ActionabilityEngine`

| 方法 | 说明 |
|---|---|
| `recalculate(roadmapId)` | 全量重算 actionability |
| `recalculateForTask(taskId)` | 局部重算 |

**规则**：

- `COMPLETED`/`ARCHIVED` → 不参与 ACTIONABLE
- 未满足前置 → `BLOCKED` + blockingTaskIds
- `dueAt` 缺失且 origin 需锚点 → `UNSCHEDULED`
- 否则 → `ACTIONABLE`

变更时写 Outbox：`ROADMAP_TASK_ACTIONABLE` / `ROADMAP_TASK_BLOCKED`。

## 7. 对外 Port（供 Profile 调用）

| Port | 方法 |
|---|---|
| `RoadmapRecomputePort` | `buildMergePreview`, `confirmMerge` |
| `RoadmapGeneratePort` | `buildGeneratePreview`, `confirmGenerate` |

PlantUML：[`service-roadmap.puml`](service-roadmap.puml)
