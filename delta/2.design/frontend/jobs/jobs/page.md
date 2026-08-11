# Jobs Inbox

- 路由：`/jobs`
- 技术栈：Vite + React SPA；API 基址 `/api/v1`
- 需求：UC-JOB-002～007
- 说明：高密度 Job Inbox；左侧列表尽量多展示岗位，右侧固定详情侧栏；默认隐藏 Gate 失败与过期归档项
- 状态：设计正文已完成

## 概述

本页为求职者主工作区：浏览、筛选、查看证据并作出职位决定。列表与详情同屏呈现（主从布局），减少跳转、提高吞吐。系统只展示 Gate/Rank 结论与证据，**不代替用户 Shortlist 或申请**；用户 `JobDecision` 优先于后续同步与规则重算。

关联页：来源同步与诊断见 [`../jobs-sources/page.md`](../jobs-sources/page.md)（`/jobs/sources`）。

## UC 跟踪

| UC | 名称 | 本页职责 |
|---|---|---|
| UC-JOB-002 | 快速保存公开职位 URL | 顶栏「保存 URL」入口；提交后打开对应 Job 详情侧栏 |
| UC-JOB-003 | 标准化、去重、Gate 与 Rank | 列表/详情展示 Gate/Rank 投影与 `RuleEvidence`；Gate 失败默认隐藏 |
| UC-JOB-004 | 查看和筛选 Job Inbox | 默认 Inbox 列表、筛选条、详情只读浏览 |
| UC-JOB-005 | 作出或覆盖职位决定 | 状态操作区、Gate 覆盖表单、乐观并发 `version` |
| UC-JOB-006 | 处理可能重复职位 | 详情内「可能重复」面板与合并确认流 |
| UC-JOB-007 | 刷新岗位有效性并归档 | 列表/详情展示 `validityStatus`；`EXPIRED` 默认隐藏 |

## 功能清单

| ID | 功能 | 说明 |
|---|---|---|
| F-JOB-INBOX-01 | 默认 Inbox 列表 | 非归档、非 Gate 失败、非 Expired 隐藏项；`New` 优先排序 |
| F-JOB-INBOX-02 | 多维筛选 | `jobStatus`、`rankTier`、`gateStatus`、`sourceCode`、`validityStatus`、可能重复、显式显示隐藏/归档 |
| F-JOB-INBOX-03 | 高密度 Job 卡片 | 标题、公司、地点、状态、tier、有效性、更新时间、关键依据摘要（单行） |
| F-JOB-INBOX-04 | 详情侧栏 | 原始来源、Gate 四维 + Rank 五因素证据、决定历史、外链申请 |
| F-JOB-INBOX-05 | 用户状态决定 | `NEW/SHORTLISTED/IGNORED/APPLIED/ARCHIVED`；恢复 `New` 保留历史 |
| F-JOB-INBOX-06 | Gate 覆盖 | 失败/待确认时可覆盖；**原因非空**；不修改原 `RuleEvidence` |
| F-JOB-INBOX-07 | 可能重复处理 | 并列对比、确认同一职位 / 不是重复 / 稍后；冲突状态须用户选择 |
| F-JOB-INBOX-08 | 保存 URL | URL 校验、硬去重、可选 JD/备注 |
| F-JOB-INBOX-09 | 跳转来源诊断 | 顶栏链至 `/jobs/sources`；异常 Job 可带 `sourceRunId` 查询参数 |

## 交互流程

### 打开 Inbox（UC-JOB-004）

1. 进入 `/jobs`，读取 URL 查询参数同步筛选状态（设计默认：无参数时用服务端默认投影）。
2. `GET /api/v1/jobs` 拉取首屏；列表虚拟滚动（设计默认）以支撑高密度。
3. 默认选中首条或 URL 中 `?jobId=` 指定项，右侧加载详情。
4. 单击列表行切换侧栏；双击或「在外部打开」在新标签打开 `canonicalApplyUrl`（用户自行申请）。

### 筛选与显式查看隐藏项

1. 筛选条变更 debounce 300ms 后重查（设计默认）。
2. 勾选「显示 Gate 失败 / 归档 / Expired」时，列表含 `hiddenByDefault=true` 项并以视觉区分（见 `style.md`）。
3. `NEEDS_CONFIRMATION/UNKNOWN` Gate 或有效性项在默认 Inbox 可见，可通过 Gate/有效性筛选收窄。

### 作出状态决定（UC-JOB-005）

1. 详情底部操作区展示当前 `jobStatus` 与快捷按钮。
2. 用户选择目标状态 → 确认（`Applied`/`Archived` 二次确认，设计默认）。
3. `POST /api/v1/jobs/{jobId}/decisions` 携带 `basedOnJobVersion`。
4. 成功：列表行与侧栏投影更新；409 时提示刷新后重试。

### Gate 覆盖（UC-JOB-005）

1. Gate 为 `FAILED/NEEDS_CONFIRMATION/UNKNOWN` 时显示「覆盖 Gate」。
2. 用户填写非空 `reason` → 提交 `GATE_OVERRIDE` 决定。
3. 覆盖后 Job 可进入 Rank/考虑范围，**不等同于 Shortlisted**；原 Gate 证据只读保留。

### 处理可能重复（UC-JOB-006）

1. 卡片/详情展示「可能重复」徽标；打开 `JobDuplicatePanel`。
2. 并列展示 `leftJob`/`rightJob` 与 `signals`；默认 survivor 为较早 `firstSeenAt`。
3. 用户选「确认同一职位」→ 若 `jobStatus` 冲突，弹出 `DuplicateStatusConflictDialog` 让用户选保留状态。
4. 提交 `POST /api/v1/jobs/duplicates/{duplicateId}/resolve`；非 survivor 归档，RawPosting 保留。

### 保存 URL（UC-JOB-002）

1. 顶栏打开 `SaveUrlDialog`：URL（必填）、JD、备注（可选）。
2. 成功返回既有或新建 `jobId`，侧栏打开该 Job；重复 URL 提示「已存在」并合并来源证据。

## 业务规则（来自需求）

| 规则 | 本页体现 |
|---|---|
| BR-JOB-005～011 Gate/Rank | 详情分区展示四维 Gate 与五因素 Rank 及各自 `RuleEvidence`；**不显示不透明综合分** |
| Rank 投票 | ≥3 `POSITIVE` 且 0 `NEGATIVE` → `HIGH`；≥2 `NEGATIVE` → `LOW`；否则 `MEDIUM` |
| BR-JOB-010 Gate 失败默认隐藏 | 默认列表排除 `gateStatus=FAILED` 且未覆盖项；筛选可显式开启 |
| 用户决定不可被覆盖 | UI 不提供「系统推荐 Shortlist」；同步/重算后用户 `SHORTLISTED/APPLIED` 等保持不变 |
| 硬去重 vs 模糊重复 | 同 stable ID/规范 URL 自动合并（用户见单一 Job）；模糊相似仅标记，**永不自动合并** |
| BR-JOB-007 预期入职日 | 缺失时工作授权 Gate 为 `NEEDS_CONFIRMATION`；UI 不推导毕业日或 +90 天 |
| 有效性 | `EXPIRED` 默认隐藏；403/captcha/超时仅 `NEEDS_CONFIRMATION/UNKNOWN`，不展示为过期 |
| 只读浏览 | 列表滚动、查看证据不改变用户决定 |

## 引用

- layout → [`layout.md`](layout.md)
- component → [`component.md`](component.md)
- model → [`model.md`](model.md)
- api → [`api.md`](api.md)
- style → [`style.md`](style.md)
- checklist → [`checklist.md`](checklist.md)
