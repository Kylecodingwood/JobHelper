# Components — Jobs Inbox

- 路由：`/jobs`
- 状态：设计正文已完成

## 组件树

```text
JobsPage (page)
├── JobsTopBar (toolbar)
│   ├── JobsPageTitle
│   ├── JobsInboxCountBadge
│   ├── SaveUrlButton → SaveUrlDialog (dialog)
│   └── LinkToSourcesButton → /jobs/sources
├── JobsFilterBar (toolbar)
│   ├── JobStatusFilter (multi-select)
│   ├── RankTierFilter (multi-select)
│   ├── GateStatusFilter (multi-select)
│   ├── SourceCodeFilter (multi-select)
│   ├── ValidityStatusFilter (multi-select)
│   ├── PossibleDuplicateToggle (checkbox)
│   ├── ShowHiddenToggle (checkbox)        // Gate 失败 / 归档 / Expired
│   └── JobsFilterResetButton
├── JobsMainSplit (layout)
│   ├── JobsListPane (panel)
│   │   ├── JobsListEmptyState (empty)
│   │   ├── JobsListErrorState (error)
│   │   └── JobsVirtualList (list)
│   │       └── JobListRow (row) × N
│   │           ├── RankTierDot
│   │           ├── JobListRowPrimary      // title + company
│   │           ├── JobListRowLocation
│   │           ├── JobStatusChip
│   │           ├── ValidityIndicator
│   │           ├── JobListRowTime
│   │           └── JobListRowBadges       // duplicate, gate-failed-hidden hint
│   └── JobsDetailPane (panel)
│       ├── JobsDetailPlaceholder (empty)
│       └── JobDetailSidebar (sidebar)
│           ├── JobDetailHeader
│           │   ├── JobTitleBlock
│           │   ├── OpenApplyLinkButton
│           │   └── CloseDetailButton      // narrow overlay only
│           ├── JobStatusActionBar
│           │   ├── JobStatusButtons       // Shortlist / Ignore / Applied / Archive / Restore New
│           │   └── ValidityStatusBadge
│           ├── GateEvidenceSection
│           │   └── GateDimensionCard × 4  // LOCATION, WORK_AUTH, SENIORITY, LANGUAGE
│           ├── RankEvidenceSection
│           │   └── RankFactorCard × 5
│           ├── JobSourcesSection
│           │   └── RawPostingCard × N
│           ├── JobDuplicatePanel (conditional)
│           │   ├── DuplicateCompareColumns
│           │   ├── DuplicateResolveActions
│           │   └── DuplicateStatusConflictDialog (dialog, conditional)
│           ├── JobDecisionHistorySection
│           │   └── DecisionTimelineItem × N
│           └── GateOverrideForm (conditional)
│               ├── GateOverrideReasonInput
│               └── GateOverrideSubmitButton
└── (global toast / conflict banner via app shell)
```

## 组件职责

| ID | 类型 | 父级 | 职责 |
|---|---|---|---|
| `JobsPage` | page | — | 路由 `/jobs`；协调列表/详情 query、URL 同步、选中 `jobId` |
| `JobsTopBar` | toolbar | JobsPage | 页级操作：保存 URL、来源页入口、Inbox 计数 |
| `SaveUrlDialog` | dialog | JobsTopBar | UC-JOB-002 表单：url、userProvidedJd、note；校验与提交 |
| `JobsFilterBar` | toolbar | JobsPage | 筛选状态 lifted state；写入 URL search params |
| `ShowHiddenToggle` | checkbox | JobsFilterBar | 显式包含 `hiddenByDefault` 项 |
| `JobsListPane` | panel | JobsMainSplit | 列表加载态、虚拟滚动容器 |
| `JobsVirtualList` | list | JobsListPane | 无限滚动、`cursor` 追加、keyboard nav |
| `JobListRow` | row | JobsVirtualList | 单行高密度摘要；click 选中、double-click 外链 |
| `JobDetailSidebar` | sidebar | JobsDetailPane | 详情数据 `GET /jobs/{id}` + evidence + decisions |
| `GateEvidenceSection` | section | JobDetailSidebar | 展示 Gate 四维 outcome + 可展开 `RuleEvidence` |
| `RankEvidenceSection` | section | JobDetailSidebar | 五因素 POSITIVE/NEUTRAL/NEGATIVE + tier  badge |
| `JobStatusActionBar` | toolbar | JobDetailSidebar | 提交 `STATUS_CHANGE`；携带 `basedOnJobVersion` |
| `GateOverrideForm` | form | JobDetailSidebar | `GATE_OVERRIDE`；reason 必填校验 |
| `JobDuplicatePanel` | panel | JobDetailSidebar | UC-JOB-006 并列对比与 resolve |
| `DuplicateStatusConflictDialog` | dialog | JobDuplicatePanel | jobStatus 冲突时用户选择保留值 |
| `JobDecisionHistorySection` | section | JobDetailSidebar | 只读 `JobDecision` 时间线 |

## 状态与数据流

- **列表数据**：React Query（设计默认）`useJobsListQuery(filters, cursor)`。
- **详情数据**：`useJobDetailQuery(jobId)` 与列表选中联动；列表行可先用 list DTO 占位再 hydrate。
- **写操作**：mutation 成功后 invalidate list + detail；409 显示 `ConflictRetryBanner`。
- **URL 同步**：`jobId`、主要筛选字段映射到 query string，便于分享与刷新保持。

## PlantUML

见 [`component.puml`](component.puml)。
