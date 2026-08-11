# Components — Home

- 路由：`/`
- 状态：设计正文已完成

## 组件树

```text
HomePageShell
├── HomeTopBar
├── HomeDegradedBanner (conditional)
└── HomeMainColumn
    ├── HomeActionSection
    │   ├── HomeSectionHeader (action)
    │   ├── HomeActionList
    │   │   └── HomeActionCard × N
    │   ├── HomeBlockedSummary (conditional)
    │   └── HomeEmptyState (conditional)
    └── HomeNewJobsSection (conditional)
        ├── HomeSectionHeader (newJobs)
        └── HomeNewJobList
            └── HomeNewJobCard × N

HomeActionPriorityDrawer (portal)
HomeActionDecisionDialog (portal)
```

## 组件规格

### `HomePageShell`

| 属性 | 说明 |
|---|---|
| 职责 | 数据加载 orchestration、错误边界、Profile 守卫 |
| 状态 | `actionsLoading`, `newJobsLoading`, `degradedDomains[]` |
| 子组件 | TopBar、MainColumn、Drawer/Dialog |

### `HomeTopBar`

| 属性 | 说明 |
|---|---|
| ID | `home-top-bar` |
| 内容 | 页面标题「Home」、本地日期、刷新按钮、链接 Roadmap/Jobs |
| 事件 | `onRefresh` → 并行 refetch actions + new jobs |

### `HomeDegradedBanner`

| 属性 | 说明 |
|---|---|
| ID | `home-degraded-banner` |
| 显示条件 | 任一 API 部分失败且仍有缓存/部分数据 |
| 文案 | 列出不可用域（如「Job 服务暂不可用，Action 列表仍可用」） |

### `HomeActionSection` / `HomeSectionHeader`

| 属性 | 说明 |
|---|---|
| ID | `home-action-section` / `home-action-header` |
| 标题 | 「今日优先」+ `(n)` 计数 |

### `HomeActionList` / `HomeActionCard`

| 属性 | 说明 |
|---|---|
| ID | `home-action-list` / `home-action-card-{actionId}` |
| 卡片内容 | `title`, `sourceDomain` badge, `priorityBand` 色条, deadline 文案, pin icon, 主依据一行, status chip（STALE 警告） |
| 交互 | 主体 click → 导航源对象；「为何优先」→ Drawer；「⋯」→ DecisionMenu |
| 禁用 | `status=STALE` 时导航 disabled，显示「目标不可用，请刷新」 |

**源域 badge 色（设计默认）**：JOB `#2563EB`、ROADMAP `#059669`、CV `#7C3AED`、BEHAVIORAL `#D97706`

### `HomeActionDecisionMenu`

| 属性 | 说明 |
|---|---|
| ID | `home-action-decision-menu` |
| 项 | Pin/Unpin、Complete、Ignore、Restore（按 status 显隐） |
| 约束 | 无 Start/进行中 项 |

### `HomeActionPriorityDrawer`

| 属性 | 说明 |
|---|---|
| ID | `home-action-priority-drawer` |
| 宽度 | 400px（设计默认） |
| 内容 | `ActionPriorityEvidence[]` 分组列表 + 规则版本 + 源对象链接 |
| 操作 | 抽屉内可 unpin、ignore、跳转源对象 |

### `HomeActionDecisionDialog`

| 属性 | 说明 |
|---|---|
| ID | `home-action-decision-dialog` |
| 用途 | Complete/Ignore 二次确认；Ignore 可选 reason textarea |

### `HomeBlockedSummary`

| 属性 | 说明 |
|---|---|
| ID | `home-blocked-summary` |
| 内容 | BLOCKED Action 列表 + `blockingActionId` 链接（若可解析） |

### `HomeEmptyState`

| 属性 | 说明 |
|---|---|
| ID | `home-empty-state` |
| 文案 | 「暂无待办 Action」 |
| CTA | 按钮 → `/roadmap`、→ `/jobs` |

### `HomeNewJobsSection` / `HomeNewJobCard`

| 属性 | 说明 |
|---|---|
| ID | `home-new-jobs-section` / `home-new-job-card-{jobId}` |
| 卡片 | 标题、公司、地点、`rankTier` badge（仅展示）、`firstSeenAt` |
| 交互 | click → `/jobs?jobId=` |
| 约束 | 不展示 Shortlist 按钮；不修改 Job status |

## 跨页复用（设计默认）

| 组件 | 来源 |
|---|---|
| `RankTierBadge` | jobs 页共享（只读） |
| `SourceDomainBadge` | 本页 local |
| `ConfirmDialog` | 应用 shell 共享 |

## PlantUML

见 [`component.puml`](component.puml)。
