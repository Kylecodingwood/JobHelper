# Components — Roadmap

- 路由：`/roadmap`
- 状态：设计正文已完成

## 组件树

```text
RoadmapPageShell
├── RoadmapTopBar
│   ├── RoadmapToolbar
│   │   ├── RoadmapTemplateButton → RoadmapTemplateDrawer
│   │   ├── RoadmapGenerateButton → RoadmapGeneratePreviewDialog
│   │   ├── RoadmapRecomputeButton → RoadmapRecomputePreviewDialog
│   │   └── RoadmapSystemUpdateButton → RoadmapSystemUpdateDialog
│   └── RoadmapHomeLink
├── RoadmapActionabilityStrip (optional)
├── RoadmapTimeline
│   └── RoadmapPhaseGroup × N
│       └── RoadmapTaskRow × N
├── RoadmapUnscheduledPanel
└── RoadmapEmptyState

RoadmapTaskDetailDrawer (portal)
RoadmapTaskCompleteDialog (portal)
RoadmapDependencyOverrideDialog (portal)
RoadmapTemplateDrawer (portal)
RoadmapMarkdownPreviewPanel (in drawer)
```

## 组件规格

### `RoadmapPageShell`

| 属性 | 说明 |
|---|---|
| 职责 | 加载 active roadmap + tasks；处理 `?taskId=` |
| 状态 | `roadmap`, `tasksByPhase`, `selectedTaskId`, `pendingRecompute` |

### `RoadmapToolbar` 按钮

| ID | 组件 | 说明 |
|---|---|---|
| `btn-roadmap-templates` | `RoadmapTemplateButton` | 打开模板抽屉 |
| `btn-roadmap-generate` | `RoadmapGenerateButton` | 无 roadmap 时 primary 强调 |
| `btn-roadmap-recompute` | `RoadmapRecomputeButton` | PENDING RecomputeRequest 时 badge |
| `btn-roadmap-template-update` | `RoadmapSystemUpdateButton` | 有新 SYSTEM 版本时 badge |

### `RoadmapTimeline` / `RoadmapPhaseGroup` / `RoadmapTaskRow`

| ID | 说明 |
|---|---|
| `roadmap-timeline` | 虚拟滚动（设计默认：任务 <200 时不虚拟化） |
| `roadmap-phase-{phaseKey}` | 可折叠；标题含任务计数 |
| `roadmap-task-row-{taskId}` | status 色点、title、actionability chip、due、pin、origin |

**actionability chip**：ACTIONABLE 实心绿；BLOCKED 灰+lock；UNSCHEDULED 虚线边框。

**origin badge**：SYSTEM_TEMPLATE / USER_TEMPLATE / USER_CREATED / CV_EVENT / JOB_EVENT / BEHAVIORAL_EVENT。

### `RoadmapTaskDetailDrawer`

| ID | `roadmap-task-detail-drawer` |
|---|---|
| 区块 | 描述、完成标准、依赖列表（含未满足高亮）、完成证据、编辑区 |
| 操作 | 开始、完成、归档、恢复、保存编辑 |
| 完成 | 触发 `RoadmapTaskCompleteDialog` 或依赖覆盖流 |

### `RoadmapDependencyOverrideDialog`

| ID | `roadmap-dependency-override-dialog` |
|---|---|
| 输入 | reason（必填）、确认 checkbox |
| 提交 | `POST .../dependency-overrides` |

### `RoadmapTemplateDrawer`

| ID | `roadmap-template-drawer` |
|---|---|
| Tab | 系统模板（只读）/ 我的模板 |
| 操作 | 粘贴 Markdown、上传 .md、`RoadmapMarkdownPreviewPanel` |
| 保存 | 仅创建 USER `RoadmapTemplateVersion` |

### `RoadmapGeneratePreviewDialog` / `RoadmapRecomputePreviewDialog`

| ID | `roadmap-generate-preview-dialog` / `roadmap-recompute-preview-dialog` |
|---|---|
| 内容 | anchorSnapshot、ruleVersion、任务预览表、warnings |
| 确认 | 分别调用 generate / recompute POST |

### `RoadmapSystemUpdateDialog`

| ID | `roadmap-system-update-dialog` |
|---|---|
| 内容 | SafeAdd / Protected / Conflict 三列表 |
| 确认 | 仅应用 SafeAdd |

### `RoadmapEmptyState`

| ID | `roadmap-empty-state` |
|---|---|
| CTA | 「生成 Roadmap」→ GeneratePreview；若无 Profile → 链至 `/profile` |

### `RoadmapHomeLink`

| ID | `roadmap-home-link` |
|---|---|
| 文案 | 「在 Home 查看优先 Action」→ `/` |

## PlantUML

见 [`component.puml`](component.puml)。
