# Layout — Jobs Sources

- 路由：`/jobs/sources`
- 状态：设计正文已完成；**布局已由原型验收**（[`../../../prototype/jobs-sources.html`](../../../prototype/jobs-sources.html)）
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)

## 布局目标

- **运行可追溯**：Source 概览 + SourceRun 时间线 + 诊断详情同屏。
- **与 Inbox 区分**：本页偏运维/诊断密度，行高可略高于 Inbox（**设计默认 56px**），但仍避免大块空白。
- **左列表右详情**（与 Jobs Inbox 一致）：运行列表约 **45%**，详情约 **55%**（诊断表格需更宽）。

## 区域划分

| 区域 ID | 名称 | 占比 | 职责 |
|---|---|---|---|
| `SourcesPageShell` | 页面壳 | 100% | 路由、返回 `/jobs` |
| `SourcesTopBar` | 顶栏 | 全宽 × 48px | 标题、手动同步、刷新 |
| `SourcesSummaryStrip` | Source 概览条 | 全宽 × 72–96px | 各 Source 卡片：enabled、最近状态 |
| `SourcesFilterBar` | 筛选 | 全宽 × 40px | source、status、trigger、日期 |
| `SourcesMainSplit` | 主分栏 | 剩余高度 | |
| `SourceRunListPane` | 运行列表 | ~45% | SourceRun 行列表 |
| `SourceRunDetailPane` | 运行详情 | ~55% | 计数、参数快照、诊断 |

## Source 概览条

- 横向 scroll 卡片：FreeHire（PRIMARY）、各 JobSpy 站点、`manual_url`。
- 每卡：名称、`enabled` toggle、最近 run 状态点、上次成功时间。

## 运行列表行

- 列：状态 badge、`triggerType`、来源/站点、开始时间、duration、`createdCount/updatedCount/failedCount` 摘要。
- 选中行高亮；`RUNNING` 行显示 indeterminate progress（设计默认）。

## 运行详情区（自上而下）

1. **RunHeader**：runId、状态、起止时间、触发类型
2. **RunMetricsGrid**：request/received/valid/created/updated/skipped/failed、durationMs
3. **ParametersSnapshotPanel**：脱敏 JSON 折叠
4. **DiagnosticsTable**：按 site + category 分组，可展开行
5. **RunActionsBar**：「重跑相同参数」、链至 Inbox 新增职位

## 空态与错误

- 无 SourceRun：引导「等待首次每日同步或手动同步」
- 选中 `FAILED` run：详情顶告警条，仍展示全部诊断

## PlantUML

见 [`layout.puml`](layout.puml)。
