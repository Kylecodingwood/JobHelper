# UI 布局与视觉基线（已确认）

| 项目 | 内容 |
|---|---|
| 状态 | **已确认**（2026-07-31） |
| 依据 | 用户验收静态原型：[`../prototype/`](../prototype/) |
| 约束力 | **`3.coding` / 前端实现仓必须按此布局与视觉实现**；与各页 `layout.md` / `style.md` 冲突时，以本基线 + 原型为准，并回写设计文档 |

## 1. 决策

用户确认：当前 `prototype/` 中的界面布局与观感即为目标产品 UI。

- **结构布局**：全局 Nav、各页分区、主从分栏、抽屉/对话框交互，实现时与原型页面一一对应。
- **视觉语言**：墨绿色品牌、纸感背景、Figtree + Fraunces 字体、语义色与密度（Home 疏 / Jobs 密），实现时对齐 `prototype/shared.css` 令牌。
- **原型性质**：`prototype/` 仍是静态 HTML 线框，**不是**应用源码；正式代码在独立前端仓用 Vite + React 按本基线重建。

## 2. 全局壳（所有已登录页）

```text
┌──────────────────────────────────────────────┐
│ Nav: Job Helper | Home Jobs Roadmap Profile │  sticky · 52px
│              CV/Behavioral「即将推出」         │
├──────────────────────────────────────────────┤
│ PageTopBar（48px）· 页标题 / 主操作           │
├──────────────────────────────────────────────┤
│                  页面主内容                    │
└──────────────────────────────────────────────┘
```

| 元素 | 规则 |
|---|---|
| 品牌 | 文案 **Job Helper**（Helper 用品牌绿）；Nav 左侧，非小字 eyebrow |
| 主导航 | `Home` / `Jobs` / `Roadmap` / `Profile`；当前项用品牌软底高亮 |
| 占位 | CV、Behavioral：灰色不可点「即将推出」 |
| 背景 | 固定纸感渐变（绿/蓝径向光 + 浅绿灰底），非纯白扁平 |
| 字体 | UI：`Figtree`；标题/品牌：`Fraunces`（实现可用等价 webfont，禁止默认 Inter/Roboto/Arial 栈作为主字体） |

## 3. 分页面布局（与原型文件对齐）

| 路由 | 原型文件 | 布局摘要（实现必须遵守） |
|---|---|---|
| `/` | `index.html` | 顶栏 + 可选降级 Banner + **居中主列 max 960px**：今日优先 Action 卡片栈 → 阻塞折叠 → New Job 网格；「为何优先」右侧抽屉 |
| `/jobs` | `jobs.html` | 顶栏（保存 URL、来源链）+ 筛选条 + **左右分栏**（列表 ~55% / 详情 ~45%）；行高 52px；详情含 Gate 四维 + Rank 五因素（无综合分） |
| `/jobs/sources` | `jobs-sources.html` | 返回 Inbox + Source 横向卡片条 + 筛选 + **左右分栏**（运行列表 / 指标·诊断） |
| `/roadmap` | `roadmap.html` | 顶栏小按钮（模板/生成/重算/系统更新）+ 可执行性 strip + **阶段时间线** + Unscheduled 底区；详情/模板用右侧抽屉；生成/重算用居中对话框 |
| `/profile` | `profile.html` | 顶栏保存 + **表单列 ~65% / 侧栏 ~35%**（重算 + 数据管理）；保存走影响预览对话框 |
| onboarding | `profile-onboarding.html` | 无完整主导航；品牌标题 + 四步条 + 居中 max 640px 向导 |

## 4. 视觉令牌（摘自原型，实现仓建 CSS variables）

| 令牌 | 值 | 用途 |
|---|---|---|
| `--brand` | `#0F6B4C` | 主按钮、Nav 激活、Roadmap 强调 |
| `--brand-soft` | `#D8EFE4` | 激活底、选中行 |
| `--ink` | `#14201A` | 主文字 |
| `--ink-muted` | `#4B5C54` | 次要文字 |
| `--paper` / `--paper-2` | `#F3F6F2` / `#E8EEE8` | 页面底 |
| `--accent` | `#1D4F91` | 链接、进行中 |
| `--danger` | `#DC2626` | deadline / FAIL |
| `--pin` | `#F59E0B` | user pin |
| `--job` / `--roadmap` | `#2563EB` / `#059669` | 源域 badge |
| `--tier-high/med/low` | `#15803D` / `#CA8A04` / `#B45309` | Rank 圆点（**禁止**用紫色表示 HIGH） |
| 圆角 | `8px` | 卡片 |
| Nav / TopBar | `52px` / `48px` | 全局 |

## 5. 实现门禁（前端仓 / `3.coding`）

- [ ] 页面信息架构与 §3 一致（不得改成仪表盘卡片墙、不得把 Roadmap 拆成多路由）
- [ ] Jobs / Sources 保持主从分栏；Home / Roadmap / Profile 保持原型分区
- [ ] 视觉令牌与 §4 一致或可证明的一对一映射
- [ ] 对照 `prototype/` 做逐页走查（结构 + 密度），再合入

## 6. 相关文档

- 原型入口：[`../prototype/README.md`](../prototype/README.md)
- 页面总览：[`pages.md`](pages.md)
- 各页细则：仍写在各页 `layout.md` / `style.md` / `component.md`；与原型冲突时回写本基线优先项
