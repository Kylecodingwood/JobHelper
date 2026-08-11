# Style — Home

- 路由：`/`
- 状态：设计正文已完成；**视觉以原型 + [`../../ui-baseline.md`](../../ui-baseline.md) 为准**
- 令牌源：[`../../../prototype/shared.css`](../../../prototype/shared.css)

## 设计原则

- **清晰优先于密度**：Home 比 Jobs Inbox 更疏朗；Action 卡片是视觉焦点。
- **可解释性可见**：每条 Action 必须可见一行「为何现在」；system suggestion 用次要色 + 「建议」后缀。
- **不暗示自动行为**：New Job 区无 Shortlist/Apply 主按钮；仅「查看」语义。
- **实现**：字体 Figtree / Fraunces；纸感背景；品牌绿 `#0F6B4C`；与原型观感一致。
## 排版与间距

| 令牌 | 值（设计默认） |
|---|---|
| 主列 max-width | 960px |
| 页面水平 padding | 16px（mobile）/ 24px（≥768px） |
| Section 间距 | 32px |
| 卡片内 padding | 16px |
| 卡片间距 | 12px |
| 顶栏高度 | 48px |

## 颜色与语义

| 用途 | 色值 | 说明 |
|---|---|---|
| Action deadline 强调 | `#DC2626` | 逾期/今日 |
| User pin | `#F59E0B` | pin icon |
| System suggestion | `#6B7280` | 次要文案 + 「建议」 |
| STALE 警告 | `#B45309` bg `#FFFBEB` | 卡片左边框 |
| 源域 JOB | `#2563EB` | badge |
| 源域 ROADMAP | `#059669` | badge |
| New Job rank HIGH | `#15803D` | 只读 tier 点（与 Jobs / 原型一致；禁止紫色 HIGH） |

**priorityBand 左边框（4px）**：DEADLINE 红、BLOCKER 橙、USER_PIN 金、SYSTEM_SUGGESTION 灰、NORMAL 无。

## 组件样式要点

### `HomeActionCard`

- 白底、1px `#E5E7EB` 边框、圆角 8px
- hover：浅阴影；focus-visible：2px focus ring
- 主体区域 cursor pointer；菜单按钮独立 hit area

### `HomeNewJobCard`

- 紧凑：高度 **72px**（设计默认）
- 横向 scroll 时宽度 **280px** 固定卡片
- rankTier 仅小圆点 + tooltip，无「推荐申请」文案

### `HomeEmptyState`

- 插画/图标 + 双 CTA 按钮（secondary outline）

### `HomeDegradedBanner`

- 全宽 amber 底；可 dismiss（session 级，设计默认）

## 动效（设计默认）

| 场景 | 动效 |
|---|---|
| 列表刷新 | skeleton 3 条，200ms fade-in |
| Drawer | 右滑 200ms |
| 卡片移除（complete/ignore） | height collapse 150ms |

## 无障碍

- Action 卡片：`aria-label` 含 title + 主依据 + deadline
- 优先级抽屉：因素列表用有序列表语义
- 色条不单独传达信息（配合文案）

## 与全局 Nav

- Home 在 Nav 中为默认 landing（有 Profile 时）
- Nav 项：Home / Jobs / Roadmap / Profile（CV/Behavioral M2/M3 占位「即将推出」）
