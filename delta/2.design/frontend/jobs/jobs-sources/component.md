# Components — Jobs Sources

- 路由：`/jobs/sources`
- 状态：设计正文已完成

## 组件树

```text
SourcesPage (page)
├── SourcesTopBar (toolbar)
│   ├── BackToJobsLink → /jobs
│   ├── SourcesPageTitle
│   ├── ManualSyncButton → ManualRerunDialog (dialog)
│   └── RefreshRunsButton
├── SourcesSummaryStrip (panel)
│   └── SourceCard × N
│       ├── SourceNameLabel
│       ├── SourceRoleBadge          // PRIMARY / SUPPLEMENTAL / MANUAL
│       ├── SourceEnabledToggle
│       ├── LastRunStatusDot
│       └── SearchTermsSummary
├── SourcesFilterBar (toolbar)
│   ├── SourceFilter (select)
│   ├── RunStatusFilter (multi)
│   ├── TriggerTypeFilter (multi)
│   └── DateRangeFilter
├── SourcesMainSplit (layout)
│   ├── SourceRunListPane (panel)
│   │   ├── SourceRunListEmpty
│   │   └── SourceRunList
│   │       └── SourceRunRow × N
│   │           ├── RunStatusBadge
│   │           ├── TriggerTypeLabel
│   │           ├── RunSourceSiteLabel
│   │           ├── RunTimeLabel
│   │           └── RunCountsSummary
│   └── SourceRunDetailPane (panel)
│       ├── SourceRunDetailPlaceholder
│       └── SourceRunDetail (panel)
│           ├── RunHeader
│           ├── RunMetricsGrid
│           ├── ParametersSnapshotPanel
│           ├── DiagnosticsTable
│           │   └── DiagnosticGroupRow × N
│           │       └── DiagnosticItemRow × M
│           └── RunActionsBar
│               ├── RerunSameParamsButton
│               └── ViewNewJobsLink → /jobs
└── ManualRerunDialog (dialog)
    ├── SourceScopePicker
    ├── UseLastParametersCheckbox
    ├── RerunPreviewPanel
    └── RerunConfirmButton
```

## 组件职责

| ID | 类型 | 职责 |
|---|---|---|
| `SourcesPage` | page | 路由 `/jobs/sources`；`sourceRunId` URL 深链 |
| `SourcesSummaryStrip` | panel | Source 启停与 searchTerms 只读摘要 |
| `SourceCard` | card | `PATCH sources/{id}` 更新 `enabled` |
| `SourceRunList` | list | 分页/无限加载 SourceRun |
| `SourceRunRow` | row | 选中加载详情；RUNNING 轮询刷新 |
| `SourceRunDetail` | panel | 单 run 指标 + 诊断 |
| `DiagnosticsTable` | table | site×category 分组；展开脱敏 detail |
| `ManualRerunDialog` | dialog | UC-JOB-009 确认与提交 |
| `RerunSameParamsButton` | button | 预填 dialog 为当前 run 参数 |

## 状态与数据流

- `useSourcesQuery()`：概览条。
- `useSourceRunsQuery(filters)`：列表。
- `useSourceRunDetailQuery(sourceRunId)`：详情 + diagnostics。
- `useCreateSourceRunMutation()`：手动重跑；成功后选中新区 run。
- RUNNING/QUEUED：列表行 5s 轮询（设计默认），终态停止。

## PlantUML

见 [`component.puml`](component.puml)。
