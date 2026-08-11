# Style — Profile

- 路由：`/profile`
- 状态：设计正文已完成

## 设计原则

- **信任与透明**：敏感字段与用途说明可见；重算影响在保存前明示。
- **Onboarding 低摩擦**：分步向导、每步少量字段；Review 步汇总。
- **LanguageProficiency 独立分组**：不与 Skill 混排，强调 Gate 语言维度。

## 排版

| 令牌 | 值（设计默认） |
|---|---|
| Form section 卡片间距 | 24px |
| Section 内字段间距 | 16px |
| Onboarding max-width | 640px |
| Edit form / side split | 65/35 @ ≥1024px |
| 顶栏 | 48px |

## 颜色

| 语义 | 色 |
|---|---|
| 必填标记 | `#DC2626` |
| 敏感字段 icon | `#B45309` |
| 用途 Roadmap | `#059669` |
| 用途 Gate | `#2563EB` |
| 用途 Rank | `#7C3AED` |
| PENDING 重算 badge | `#F59E0B` |
| DEFERRED | `#6B7280` |
| 保存 disabled | 标准 muted |

## Section 样式

- 白底卡片、圆角 8px、section 标题 16px semibold
- `ProfileFieldUsagePopover` trigger：16px 「ℹ」neutral
- WorkAuthorization 时间线：垂直 connector + 卡片每项
- LanguageProficiency：表格行编辑（language + proficiency + notes）

## Onboarding

- Step indicator：4 圆点 + 标签；当前步 primary 填充
- 底部 sticky 操作栏（mobile）

## 重算面板

- scope Checkbox 组；ROADMAP 选中时展开预览表格（amber 警告「不覆盖已完成任务」）
- 主按钮「确认重算」destructive-secondary 样式（设计默认：outline + 确认 dialog）

## 数据管理

-  monospace 展示 backupDir 与 cron 描述
- Export primary；Restore danger outline

## 无障碍

- 表单关联 label/for；错误 `aria-invalid`
- 敏感 popover 可键盘聚焦

## 与全局

- 无 Profile 时 Nav 仍可进入 Profile（当前页）
- 完成后 Nav 全功能可用
