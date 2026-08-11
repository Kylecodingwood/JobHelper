# Layout — Profile

- 路由：`/profile`
- 状态：设计正文已完成；**布局已由原型验收**（[`../../../prototype/profile.html`](../../../prototype/profile.html)、[`profile-onboarding.html`](../../../prototype/profile-onboarding.html)）
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)

## 布局目标

- **表单可读**：分组卡片纵向排列；onboarding 用步骤条 + 单步主内容区。
- **重算与数据管理固定可见**（edit 模式）：右侧栏或底部 panel（设计默认：≥1024px 右栏 360px）。

## 区域划分 — Edit 模式

| 区域 ID | 名称 | 尺寸 | 职责 |
|---|---|---|---|
| `ProfilePageShell` | 页面壳 | 100% viewport | 模式切换 onboarding/edit |
| `ProfileTopBar` | 顶栏 | 48px | 标题、保存、版本号 |
| `ProfileMainSplit` | 主分栏 | 剩余高度 | 表单 + 侧栏 |
| `ProfileFormColumn` | 表单列 | ~65% | 各 section 卡片 |
| `ProfileSideColumn` | 侧栏 | ~35% min 320px | 重算 + 数据管理 |
| `ProfileRecomputePanel` | 重算面板 | 侧栏上 | PENDING/DEFERRED 请求 |
| `ProfileDataManagementPanel` | 数据管理 | 侧栏下 | 备份/导出/恢复 |

## 表单 Section 顺序（Edit）

1. `ProfileSectionEducation`
2. `ProfileSectionWorkAuth`
3. `ProfileSectionTargetRole`
4. `ProfileSectionLanguageProficiency`
5. `ProfileSectionSkills`
6. `ProfileSectionExperience`

## 区域划分 — Onboarding 模式

| 区域 ID | 说明 |
|---|---|
| `ProfileOnboardingWizard` | 全宽居中 max 640px |
| `ProfileStepIndicator` | 顶步条 1–4 |
| `ProfileStepContent` | 当前步表单 |
| `ProfileStepActions` | 上一步/下一步/保存 |

## 对话框（portal）

- `ProfileImpactConfirmDialog`
- `ProfileExportDialog`
- `ProfileRestoreDialog`
- Roadmap 预览可嵌入 RecomputePanel 或复用 Roadmap 预览组件（设计默认：内嵌 iframe 式表格，非跳转）

## 响应式

| 断点 | 行为 |
|---|---|
| ≥1024px | 65/35 split |
| <1024px | 单列；Recompute + Data 在表单下方 accordion |

## PlantUML

见 [`layout.puml`](layout.puml)。
