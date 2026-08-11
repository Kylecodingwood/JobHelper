# Style — Jobs Sources

- 路由：`/jobs/sources`
- 状态：设计正文已完成

## 密度与调性

- 较 Inbox **略松**（行高 56px），仍保持数据表格式运维视图，避免卡片化浪费纵向空间。
- 诊断表 **等宽字体** 仅用于 `requestId`、itemKey；消息正文用 sans-serif 13px。
- 与 [`../jobs/style.md`](../jobs/style.md) 共享色板，但 **不展示 Rank tier**（本页无 Job 卡片）。

## SourceRun 状态色

| Status | Badge |
|---|---|
| `SUCCEEDED` | 绿 `#16A34A` |
| `PARTIAL_SUCCESS` | 琥珀 `#D97706` + 「部分成功」 |
| `FAILED` | 红 `#DC2626` |
| `RUNNING` | 蓝 `#2563EB` + pulse |
| `QUEUED` | 灰蓝 `#64748B` |
| `CANCELLED` | 灰 `#9CA3AF` |

## 诊断类别 icon/色

| Category | 色 | 说明 |
|---|---|---|
| `403` | 红 | 权限/封禁 |
| `429` | 橙 | 限速 |
| `CAPTCHA` | 紫 `#9333EA` | 挑战页 |
| `TIMEOUT` | 琥珀 | 超时 |
| `PARSE` | 橙红 | 解析失败 |
| `VALIDATION` | 灰 | 字段缺失 |
| `OTHER` | 灰 | 兜底 |

分组行：site 列固定宽 120px；category chip + count badge。

## Source 概览卡片

- FreeHire PRIMARY：左侧 3px 绿条
- JobSpy 站点：左侧灰条；`enabled=false` 整体 opacity 0.6
- `riskNote`：卡片底部 11px 灰字 ToS 提示

## 手动重跑 Dialog

- 确认前 **强制** 展示将执行的 searchTerms 与 scope 摘要
- RUNNING 时主按钮 disabled + 「排队中 #n」文案

## 安全展示

- 诊断 `message` 最多显示 2 行，展开全文仍经脱敏；**无**「复制原始响应」按钮
- `parametersSnapshot` JSON 折叠默认关闭

## 导航

- `BackToJobsLink` 与 Inbox `LinkToSourcesButton` 对称，均用次要按钮样式

## 响应式

- `<1024px`：Source 概览条全宽 scroll；运行列表与详情上下堆叠（列表 max-height 40vh）
