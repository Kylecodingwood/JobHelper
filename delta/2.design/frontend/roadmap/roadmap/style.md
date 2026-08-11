# Style — Roadmap

- 路由：`/roadmap`
- 状态：设计正文已完成

## 设计原则

- **时间线叙事**：强调阶段与日期；用户一眼看到「何时做什么」。
- **工具栏轻量**：模板/生成/重算/更新为 **icon + 短标签** 小按钮，不用主导航 Tab。
- **Action 外置**：跨域优先级只在 Home；本页用 `RoadmapHomeLink` 引导，避免双待办列表。

## 排版

| 令牌 | 值（设计默认） |
|---|---|
| 时间线 max-width | 1200px |
| 日期轴宽度 | 80px |
| Phase 标题 | 18px semibold |
| 任务行高 | 44px compact / 56px comfortable（设计默认 compact） |
| 顶栏 | 48px |

## 颜色

| 语义 | 色 |
|---|---|
| ACTIONABLE | `#059669` |
| BLOCKED | `#9CA3AF` + lock icon |
| UNSCHEDULED | 虚线 `#D1D5DB` |
| TODO | `#6B7280` dot |
| IN_PROGRESS | `#2563EB` dot |
| COMPLETED | `#059669` check |
| ARCHIVED | `#9CA3AF` muted |
| due 临近/逾期 | `#DC2626` 日期文案 |
| 用户 pin | `#F59E0B` star |

**origin badge**：SYSTEM 蓝灰、USER 紫、USER_CREATED 青、事件任务 橙（JOB/CV/BEHAVIORAL 细分）。

## 时间线视觉

- Phase 间 **1px** 分隔 + 24px 间距
- 任务行左侧 status dot + 可选 due 对齐日期轴
- COMPLETED 行：0.6 opacity + 删除线标题（设计默认）
- 依赖覆盖完成：title 旁 「覆盖」chip amber

## 抽屉与对话框

| 组件 | 宽度/形式 |
|---|---|
| TemplateDrawer | 480px 右抽屉 |
| TaskDetailDrawer | 520px |
| Preview/Update Dialog | 640px 居中 modal |
| DependencyOverride | 400px modal |

## 空态

- 插图 + 「尚未生成 Roadmap」+ Primary「生成」+ Secondary「管理模板」

## 动效

- Phase 折叠 150ms
- 任务完成：行背景 brief flash green 200ms
- 模板 preview warnings：amber inline list

## 无障碍

- 时间线：`role="list"`；任务行 `aria-expanded` 当抽屉打开
- actionability 除颜色外必须有文本 label
