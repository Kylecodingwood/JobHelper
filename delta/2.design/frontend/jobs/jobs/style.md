# Style — Jobs Inbox

- 路由：`/jobs`
- 状态：设计正文已完成
- 全局：Vite + React SPA；与 [`../../pages.md`](../../pages.md) 主导航一致

## 密度原则

- **Jobs 域最高信息密度**：紧凑行高、小字号、少留白；目标桌面首屏 **≥12 行** Job（1080p，Nav+筛选+顶栏后）。
- 列表行 **52px**（compact）；侧栏 section 间距 **8px**；chip 高度 **22px**。
- 正文 **13px**（设计默认），标题 **15px/600**；等宽仅用于 URL/ID 副本。

## 视觉层级

### Rank tier（High / Medium / Low）

| Tier | 列表 | 侧栏 | 语义 |
|---|---|---|---|
| `HIGH` | 左侧 6px 圆点 `#15803D`（green-700） | Badge 绿底白字 | ≥3 POSITIVE 且 0 NEGATIVE |
| `MEDIUM` | 圆点 `#CA8A04`（yellow-600） | Badge 琥珀底 | 默认中间档 |
| `LOW` | 圆点 `#B45309`（amber-700） | Badge 浅橙底 | ≥2 NEGATIVE |
| `UNRANKED` | 圆点 `#9CA3AF`（gray-400） | 灰 badge「未排名」 | Gate 未通过或未覆盖 |

**禁止**展示数值型「匹配分」或隐藏综合分；仅 tier + 五因素方向。

### Gate 状态

| GateStatus | 列表/侧栏 |
|---|---|
| `PASSED` | 无额外警告色 |
| `FAILED` | 默认 **不出现在列表**；`showHidden` 时行背景 `#FEF2F2`，标签「Gate 失败」 |
| `NEEDS_CONFIRMATION` | 琥珀 `#F59E0B` 边线 icon |
| `UNKNOWN` | 灰 `#6B7280` 问号 icon |

Gate 维度卡片：PASS 绿勾、FAIL 红叉、NEEDS_CONFIRMATION/UNKNOWN 琥珀/灰；证据折叠区 **12px** 说明文。

### Job 用户状态 chip

| Status | 色 |
|---|---|
| `NEW` | 蓝 `#2563EB` |
| `SHORTLISTED` | 绿 `#16A34A` |
| `IGNORED` | 灰 `#6B7280` |
| `APPLIED` | 紫 `#7C3AED` |
| `ARCHIVED` | 灰框线 |

### 有效性

- `ACTIVE`：无 icon 或 subtle green dot
- `EXPIRED`：默认隐藏；显式筛选时删除线标题 + 灰 `#9CA3AF`
- `NEEDS_CONFIRMATION` / `UNKNOWN`：琥珀/灰小 icon，**不**渲染为过期

## 默认隐藏（Gate 失败）

- 默认 Inbox **不含** `gateStatus=FAILED` 且 `gateOverrideActive=false` 的 Job。
- 默认 **不含** `validityStatus=EXPIRED` 与 `jobStatus=ARCHIVED`。
- `ShowHiddenToggle` 开启后：隐藏项以 **降低对比度（opacity 0.72）** + 左侧虚线边区分，避免与活动 Inbox 混淆。

## 可能重复

- 列表：标题旁 `#DBEAFE` 小 badge「重复?」
- 侧栏 `JobDuplicatePanel`：双列对比，survivor 候选高亮框；冲突 dialog 强调「不会自动覆盖 Shortlisted」

## 交互反馈

- 选中行：左侧 2px `#2563EB` accent + `#EFF6FF` 背景
- 写操作 pending：按钮 spinner；成功 toast 2s（设计默认）
- 409 冲突：侧栏顶 `ConflictRetryBanner` amber

## 无障碍（设计默认）

- tier 色点 + 文本 label（不仅靠颜色）
- 列表行 `aria-selected`；侧栏 `role=complementary`
- Gate/Rank 折叠区 `aria-expanded`

## 响应式

- `<1024px`：侧栏 overlay 宽 **min(420px, 92vw)**，列表保持高密度
- 筛选条窄屏水平 scroll，sticky 顶栏下
