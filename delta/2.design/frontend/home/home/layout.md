# Layout — Home

- 路由：`/`
- 状态：设计正文已完成；**布局已由原型验收**（[`../../../prototype/index.html`](../../../prototype/index.html)）
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)

## 布局目标

- **决策优先**：首屏可见「现在该做什么」与「有什么新职位」，避免堆叠长报告。
- **单列主内容**：全局 Nav 下居中内容区，最大宽度 **960px**，两侧留白（与原型一致）。
- **两区块垂直堆叠**：Action 区在上（主），New Job 网格在下（辅）。
- **实现要求**：React 页面结构须可与 `prototype/index.html` 分区一一对应，不得改成多列仪表盘。
## 区域划分

| 区域 ID | 名称 | 尺寸 | 职责 |
|---|---|---|---|
| `HomePageShell` | 页面壳 | 100% × min(100vh − Nav) | 路由容器、错误边界、路由守卫回调 |
| `HomeTopBar` | 顶栏 | 全宽 × 48px | 问候/日期、刷新、跳转 Roadmap/Jobs 快捷链 |
| `HomeDegradedBanner` | 降级条 | 全宽 × 条件 | 域不可用提示（UC-ACT-001 A2） |
| `HomeMainColumn` | 主列 | max-width 960px 居中 | 垂直 stack |
| `HomeActionSection` | Action 区 | 主列内 flex-1 | 标题 + 列表 + 阻塞摘要 |
| `HomeActionList` | Action 列表 | 自适应 | 优先 Action 卡片 stack |
| `HomeBlockedSummary` | 阻塞摘要 | 条件折叠 | 被 BLOCKED 项及 blocker 关系 |
| `HomeNewJobsSection` | New Job 区 | 主列底部 | 横向 scroll 或 compact 列表 |
| `HomeEmptyState` | 空态 | Action 区内 | 无 Action 时 CTA |

## Action 区内部

1. **SectionHeader**：「今日优先」+ 条数 + 刷新图标
2. **ActionCards**：每条 `HomeActionCard`，间距 **12px**（设计默认）
3. **BlockedSummary**（条件）：默认折叠，展开列出 BLOCKED 项与「先完成 X」提示

## New Job 区内部

- **SectionHeader**：「新职位」+ 「查看全部 → /jobs?status=NEW」
- **JobCards**：横向 scroll（设计默认）或 2 列 grid（≥768px）
- 无数据时不渲染整个 section

## 响应式

| 断点 | 行为 |
|---|---|
| ≥768px | 主列 960px；New Job 2 列 grid |
| <768px | 主列 100% − 16px padding；New Job 横向 scroll |

## PlantUML

见 [`layout.puml`](layout.puml)。
