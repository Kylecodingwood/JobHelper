# Layout — Roadmap

- 路由：`/roadmap`
- 状态：设计正文已完成；**布局已由原型验收**（[`../../../prototype/roadmap.html`](../../../prototype/roadmap.html)）
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)

## 布局目标

- **时间线为主**：全宽时间线占满主内容区；工具栏紧凑，不抢占垂直空间。
- **单页无子路由**：模板/导入/生成/更新均为 overlay（抽屉/对话框）。
- **阶段可读**：phase 分组 + 日期刻度；`UNSCHEDULED` 置底独立区。

## 区域划分

| 区域 ID | 名称 | 尺寸 | 职责 |
|---|---|---|---|
| `RoadmapPageShell` | 页面壳 | 100% × 100vh − Nav | 路由、深链 taskId |
| `RoadmapTopBar` | 顶栏 | 全宽 × 48px | 标题、工具栏按钮、Home 链接 |
| `RoadmapToolbar` | 工具栏 | 顶栏内联 | 模板/生成/重算/系统更新 小按钮 |
| `RoadmapActionabilityStrip` | 可执行性摘要 | 全宽 × 32px 可选 | 统计 ACTIONABLE/BLOCKED 计数 |
| `RoadmapTimeline` | 时间线 | 全宽 × 剩余高度 scroll | phase 分组 + 任务行 |
| `RoadmapUnscheduledPanel` | 未排期区 | 时间线底部 | `UNSCHEDULED` 任务 |
| `RoadmapEmptyState` | 空态 | 居中 | 无 Active Roadmap |

## 时间线内部

| 区域 ID | 说明 |
|---|---|
| `RoadmapPhaseGroup` | phase 标题 + 折叠 + due 范围 |
| `RoadmapDateAxis` | 左侧日期刻度（设计默认 80px 列） |
| `RoadmapTaskRow` | 任务行：status dot、title、badges、due |
| `RoadmapTaskDetailDrawer` | 右侧抽屉 overlay |

## 顶栏工具栏按钮（小按钮，非 Nav 项）

| 按钮 ID | 标签 | 打开 |
|---|---|---|
| `btn-roadmap-templates` | 模板 | `RoadmapTemplateDrawer` |
| `btn-roadmap-generate` | 生成 | `RoadmapGeneratePreviewDialog`（无 Roadmap 时常显） |
| `btn-roadmap-recompute` | 重算 | `RoadmapRecomputePreviewDialog`（有 PENDING 请求时 badge） |
| `btn-roadmap-template-update` | 更新 | `RoadmapSystemUpdateDialog`（有新 SYSTEM 版本时 badge） |

## 响应式

| 断点 | 行为 |
|---|---|
| ≥1024px | 时间线 max-width 1200px 居中；任务抽屉 520px |
| <1024px | 全宽；抽屉全屏 overlay |

## PlantUML

见 [`layout.puml`](layout.puml)。
