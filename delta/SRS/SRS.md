# Job Helper 软件需求规格说明书（SRS）

## 0. 文档控制

| 项目 | 内容 |
|---|---|
| 文档状态 | v0.4 — Requirement 门禁通过；设计与 coding 映射已完成，可生成应用代码 |
| 版本 | 0.4 |
| 产品阶段 | Personal-first / Local-first |
| 主要地区 | Ireland，可配置学校、时间线、地点与目标岗位 |
| 文档语言 | 中文为主，领域名、接口术语和求职材料保留英文 |
| 权威范围 | 本文是产品需求的单一事实来源；`delta/1.req/` 负责按域细化与追踪 |
| 进度日志 | [`progress-log.md`](progress-log.md)（2026-08-09：M1 文档完成范围与待实现项） |

### 0.1 已确认决策与设计期 TBD

**Pivot 2026-08-09（覆盖上表 3、5 及相关 FR）：**

3′. Roadmap 为**独立 Notion 式待办**（name / due / comment / checkbox），与 Profile **解耦**；废止系统模板、GRS 锚点、依赖图、Profile→Roadmap 重算 merge。  
5′. Profile 为**用户画像**（国籍、身份含原工作许可、目标国家、教育、工作经验、求职目标、skills、多语言），供后续 AI prompt；**废止 TargetRole**；搜索词由用户在 **Job Source** 自管（大搜索框），首次同步前必填。  
Home：今日优先默认展示 **3** 条，其余折叠展开。  
Sources 页：**上下布局** + LinkedIn 风格搜索框。

2026-07-30 已确认（含 Requirement 冻结轮；其中 3、5 已被上列 Pivot 覆盖）：

1. Gate 是清洗后的硬资格判断；失败岗位保留、默认隐藏并允许用户覆盖；缺 `expectedStartDate` 时工作授权维度为 `NEEDS_CONFIRMATION`，不自动推导入职日。
2. Rank 对五项因素分别给出正/中/负证据后按透明投票聚合：≥3 正且无负=`High`；≥2 负=`Low`；其余=`Medium`。Shortlist 只由用户确认。
3. Roadmap 使用版本化系统模板（含 `system-template-v1` 种子）+ 结构化 Markdown 导入 + 领域事件追加任务；招聘季锚点为毕业日前最近的 9 月 1 日；首次与重算均先预览确认；重算只更新系统生成且未完成的任务。
4. Home Action 由 Action 域唯一拥有；Roadmap 只维护 `RoadmapTask` 并发布事件。Action 状态为 `OPEN/BLOCKED/COMPLETED/IGNORED/STALE`（无 `IN_PROGRESS`）。
5. Profile 增加独立 `LanguageProficiency`；搜索词默认来自启用中的 `TargetRole`，用户可覆盖。
6. CV/Behavioral：本地确定性检查始终可用；外部 AI / Cursor Skill/SDK 为可选增强；统一全局 AI Consent Registry。
7. 数据无自动过期；每周本地备份 + 手动完整 ZIP 导出 + 恢复前影响预览；敏感对象级联硬删除。
8. JobSpy 全站点默认尝试；站点失败只进入 SourceRun 诊断；重复职位确认时保留较早 Job，冲突由用户选择。

进入设计阶段后仍需选择：

- AI provider、具体模型及预算；需求层仅冻结 provider-neutral 接口、默认关闭和授权边界。
- PDF/DOCX 解析库、任务调度钟点与备份保留份数（策略已冻结：每周自动 + 导出 + 恢复预览）。

**设计已确认技术栈（2026-07-30）：** Java Spring Boot、PostgreSQL、React、Python（JobSpy）；详见 `delta/2.design/adr/tech-stack.md`。默认本地 Web；桌面壳为可选增强。

**已于 Phase 0 确认（2026-07-29）：**

- M1 主自动来源：**FreeHire API**（`countries=ie`，公开 REST，无需 API Key）；
- M1 补充自动来源：**JobSpy**（LinkedIn、Indeed 已验证可用；默认尝试全部受支持站点并隔离失败）；
- EURES 与独立 ATS 探针：**不进入 M1**（验证噪声高或已被 FreeHire 覆盖）。

## 1. 项目概述

### 1.1 背景

计划赴爱尔兰攻读一年制硕士的软件开发者需要在紧凑时间内同时完成求职准备、职位发现、简历迭代和面试准备。现有信息分散在招聘网站、学校 Career Service、个人笔记、CV 文件和面试题库中，用户难以判断当前最重要的行动，也容易遗漏新职位或重复劳动。

### 1.2 产品定位

Job Helper 是一个面向开发者、尤其是赴爱尔兰攻读一年制硕士并寻找 Internship、Graduate 或 Junior 全职岗位用户的本地优先求职准备与职位决策助手。

产品不定位为招聘网站、自动投递机器人或普通 Todo 应用。核心价值是：

> 将分散的职位和准备要求转化为少量、有依据、可执行的“今日行动”。

### 1.3 第一用户

第一用户是项目所有者本人。系统先解决个人真实求职问题，并作为可解释的软件工程作品。第一阶段不为公开 SaaS、多租户和商业运营设计。

### 1.4 产品目标

- 持续发现符合基本资格的爱尔兰软件岗位；
- 解释岗位为何被保留、排序或排除；
- 根据个人时间线生成有依赖关系的求职准备路线图；
- 针对具体岗位评审 CV，并保留用户对每条建议的决定；
- 使用 STAR 方法积累和练习 Behavioral Question 答案；
- 在首页明确展示最优先准备任务和待处理新职位。

### 1.5 成功标准

Milestone 1 的主要成功标准是：系统连续运行两周，稳定提供真实、仍可申请且符合基本资格的新岗位，并推动用户完成准备任务；期间无需人工清理重复数据，且每项纳入、排除和排序结果均可解释。

## 2. 范围与里程碑

### 2.1 Milestone 1：Roadmap + Job

必须完成：

- 建立个人求职档案；
- 按爱尔兰求职模板和确定性规则生成个性化 Roadmap；
- 支持预览并导入用户提供的 Markdown Roadmap 内容；
- 管理任务状态、依赖、完成标准和用户调整；
- 通过 **FreeHire API** 及 **JobSpy（LinkedIn/Indeed）** 获取爱尔兰软件岗位；
- 支持用户快速保存公开职位 URL，并可附加 JD；
- 标准化、去重、Gate、Rank 和来源追踪；
- 管理 `New`、`Shortlisted`、`Ignored`、`Applied`、`Archived` 状态；
- 刷新岗位有效性，异常时标记待确认而非自动删除；
- 首页展示优先准备任务和待处理新职位。

### 2.2 Milestone 2：CV Review

必须完成：

- 上传 PDF/DOCX CV；
- 保存提取文本和版本关系；
- 将 CV 版本与目标 Job 关联；
- 展示原文、问题、判断依据和修改建议；
- 用户逐项接受或拒绝建议；
- 根据已接受建议形成新版本，不覆盖原版本。

### 2.3 Milestone 3：Behavioral Preparation

必须完成：

- 管理 Behavioral Question 题库；
- 建立可复用的 STAR 经历证据；
- 编写并保存答案草稿；
- 从 STAR 结构完整性和证据具体性两个维度反馈；
- 将待练习内容转化为 Action。

### 2.4 明确不做

- 自动提交申请或自动联系雇主；
- 自动登录招聘平台账号或绕过验证码；
- 第一阶段公开部署、多用户注册、RBAC 或付费功能；
- 第一阶段语音模拟面试；
- AI 自动覆盖 CV、替用户改变 Job 状态或作最终申请决定；
- 将不可解释的单一百分比分数作为推荐依据；
- 第一阶段完整面试、拒绝、Offer 等申请管线。

## 3. 参与者与外部系统

| 参与者 | 类型 | 职责 |
|---|---|---|
| 求职者 | 主参与者 | 配置档案、处理职位、完成 Roadmap、评审 CV 建议、练习 BQ |
| 调度器 | 系统参与者 | 定时触发职位源同步和岗位有效性刷新 |
| FreeHire | 外部系统 | 公开 REST API，聚合 75+ ATS；M1 主自动发现源 |
| JobSpy | 外部库 | 本地 Python 尝试所有受支持站点；有效结果进入职位流，失败进入诊断 |
| 公开职位页面 | 外部资源 | 用户主动提供 URL；与自动源按 URL 去重合并 |
| AI 服务 | 可选外部系统 | 在用户明确同意后提供 CV/BQ 等推理或写作辅助 |

## 4. 术语

| 术语 | 定义 |
|---|---|
| Gate | 使用地点、工作许可、语言、职级等硬条件判断岗位是否具备基本可行性 |
| Rank | 对通过 Gate 的岗位按技能、时效、偏好和成长价值分维度排序 |
| RawPosting | 从某个来源取得的原始职位记录，保留来源证据 |
| CanonicalJob | 标准化、可合并多个来源的职位记录 |
| SourceRun | 一次数据源同步运行及其结果、错误和耗时 |
| Roadmap | 根据用户阶段和目标组织的结构化求职准备计划 |
| Action | 由 Job、Roadmap、CV 或 Behavioral 产生的跨域可执行行动 |
| CV Version | 不可覆盖的 CV 内容快照及其父版本关系 |
| STAR Evidence | Situation、Task、Action、Result 结构化经历证据 |

## 5. 总体业务流程

### 5.1 首次配置

1. 用户启动本地应用；
2. 用户配置教育时间、工作许可、目标岗位、目标职级、地点、技能和经历；
3. 系统根据模板和规则初始化 Roadmap；
4. 系统展示当前阻塞项和首批优先任务。

### 5.2 每日使用

1. 调度器同步允许的数据源并刷新已有岗位；
2. 系统标准化、去重、Gate 和 Rank；
3. 首页展示优先准备任务和待处理新职位；
4. 用户进入目标对象完成任务或处理职位；
5. 系统保存决定并重新计算后续 Action。

### 5.3 职位处理

1. 职位由 FreeHire、JobSpy 或用户保存 URL 进入；
2. 系统保留 RawPosting 并生成或更新 CanonicalJob；
3. 系统执行去重和有效性检查；
4. 系统展示 Gate 结果、Rank 维度和证据；
5. 用户将职位标记为 `Shortlisted`、`Ignored` 或 `Applied`；
6. 用户决定优先于后续同步，不得被后台任务覆盖。

### 5.4 CV Review

1. 用户上传一个 CV 版本并关联目标 Job；
2. 系统提取文本并提示用户确认；
3. 在需要云端 AI 时，系统展示将发送的数据并请求确认；
4. 系统按问题、依据和建议生成评审项；
5. 用户逐项接受或拒绝；
6. 系统依据已接受项创建新版本并保留完整历史。

### 5.5 Behavioral Preparation

1. 用户选择题目；
2. 用户选择已有 STAR Evidence 或新建经历；
3. 用户编写答案草稿；
4. 系统检查 STAR 结构和证据具体性；
5. 用户修改并保存新版本；
6. 未完成练习可形成后续 Action。

## 6. 功能需求

### 6.1 Profile

- **FR-PRO-001** 系统必须允许用户配置学校、专业、入学时间和毕业时间。
- **FR-PRO-002** 系统必须允许用户按国家维护工作许可时间线，包括许可类型、有效期、每周工时限制和预计未来许可。
- **FR-PRO-003** 系统必须允许用户配置目标岗位、目标职级和目标地点。
- **FR-PRO-004** 系统必须允许用户维护技能、项目和实习经历。
- **FR-PRO-005** Profile 变化影响 Gate、Rank 或 Roadmap 时，系统必须提示重新计算，不得静默改变既有用户决定。
- **FR-PRO-006** 系统必须在本地保存 Profile，并明确标识可能被发送到外部 AI 的字段。
- **FR-PRO-007** 系统必须允许用户维护独立的语言熟练度（如 `LanguageProficiency`），并作为语言 Gate 的输入。
- **FR-PRO-008** 系统必须支持每周本地自动备份、手动完整导出，以及从备份/导出恢复前的影响预览与二次确认。

### 6.2 Roadmap

- **FR-RDM-001** 系统必须根据爱尔兰求职模板、教育时间线和目标岗位生成初始 Roadmap。
- **FR-RDM-002** Roadmap 生成必须以确定性规则为主，AI 只能提供可选建议。
- **FR-RDM-003** 每个 Roadmap Task 必须包含阶段、状态、前置依赖、完成标准、来源和优先级信息。
- **FR-RDM-004** 系统必须阻止依赖未满足的任务被表示为“已完全准备”，但允许用户覆盖并记录原因。
- **FR-RDM-005** 用户必须能够新增、编辑、完成、恢复和归档自定义任务。
- **FR-RDM-006** 系统必须根据依赖和当前状态生成优先 Action。
- **FR-RDM-007** 模板更新不得覆盖用户已完成、已编辑或已归档的任务。
- **FR-RDM-008** 第一版不要求外部日历同步、重复任务和系统级推送通知。
- **FR-RDM-009** 系统模板必须记录来源 URL、适用地区、许可证或使用依据、版本和更新时间；不得整篇复制许可证不明的外部内容。
- **FR-RDM-010** 用户必须能够上传或粘贴 Markdown，预览解析结果后导入为自定义模板。
- **FR-RDM-011** Roadmap 任务日期必须可相对于入学、招聘季、毕业、工作许可到期等 Profile 锚点计算。
- **FR-RDM-012** CV Review、Job Decision 和 Behavioral Preparation 可追加 Roadmap Task 或 Action，但不得重建或覆盖现有 Roadmap。

### 6.3 Job

- **FR-JOB-001** 系统必须通过统一 Source Adapter 接入不同职位来源。
- **FR-JOB-002** Milestone 1 的自动来源为 **FreeHire API**（主）和 **JobSpy**（默认尝试其支持的全部站点，各站点可独立启停）。
- **FR-JOB-003** 系统必须允许用户快速保存公开职位 URL，并可附加 JD 文本。
- **FR-JOB-004** JobSpy 适配器可搜索已启用的商业招聘板（LinkedIn、Indeed 等）；不得自动登录账号、提交申请或绕过验证码；每个站点必须可独立启用/停用，并记录 ToS 与速率限制风险。
- **FR-JOB-004a** FreeHire 适配器必须通过公开 `GET /api/v1/jobs/search` 接入，支持 `countries=ie` 及 facet 过滤，无需 API Key。
- **FR-JOB-005** 系统必须保存 RawPosting、来源 ID、来源 URL、抓取时间和原始字段。
- **FR-JOB-006** 系统必须将 RawPosting 标准化为统一 CanonicalJob。
- **FR-JOB-007** 系统必须优先使用来源稳定 ID 和规范 URL 硬去重。
- **FR-JOB-008** 仅凭公司、标题、地点等相似性判断时，系统必须标记“可能重复”，不得自动合并。
- **FR-JOB-009** Gate 必须至少支持地点、工作许可、语言和 Intern/Graduate/Junior 职级条件。
- **FR-JOB-009a** Ireland 或 Remote Ireland 岗位在其他资格满足时通过地点 Gate；EU 岗位默认为“待确认”，仅在明确支持当地许可或允许用户居住 Ireland 合法远程受雇时通过。
- **FR-JOB-009b** Intern、Graduate、Junior 和未注明职级默认通过职级 Gate；Senior、Lead、Staff、Principal 和 Manager 默认不通过。
- **FR-JOB-009c** English 或未注明语言默认通过；岗位明确要求 Profile 不具备的额外语言时不通过。
- **FR-JOB-010** Rank 必须分别展示目标岗位/职级、技能证据、发布时间、地点偏好和成长价值五项证据方向（正/中/负），并按透明投票聚合：≥3 正且无负=`High`；≥2 负=`Low`；其余=`Medium`；不得展示黑箱百分比分数。
- **FR-JOB-011** 每个 Gate 和 Rank 结论必须保存规则版本、触发条件和职位证据。
- **FR-JOB-012** 系统必须提供 `New`、`Shortlisted`、`Ignored`、`Applied`、`Archived` 状态。
- **FR-JOB-013** 用户设置的职位状态不得被后续同步覆盖。
- **FR-JOB-014** 系统必须通过来源刷新和申请链接检查评估岗位是否仍可申请。
- **FR-JOB-015** 无法确认有效性时，系统必须标记“待确认”，不得自动删除职位。
- **FR-JOB-016** 系统必须保存 SourceRun 的开始时间、结束时间、状态、请求统计、错误和新增/更新/失效数量。
- **FR-JOB-017** 同一 SourceRun 重试不得重复创建相同 RawPosting 或 CanonicalJob。
- **FR-JOB-018** 系统必须允许用户查看原始来源和前往原页面申请。
- **FR-JOB-019** 系统不得自动提交申请。
- **FR-JOB-020** Gate 未通过的岗位必须保留并默认隐藏；用户可查看证据、填写原因并覆盖 Gate 结论。
- **FR-JOB-021** 系统不得因 Rank 自动设置 `Shortlisted`；Shortlist 仅由用户操作。
- **FR-JOB-022** 每个 JobSpy 站点失败必须记录在 SourceRun 诊断中，不得在 Jobs 列表生成空岗位或错误岗位。
- **FR-JOB-023** 自动职位同步默认每日运行一次，并支持用户手动重跑。

### 6.4 CV

- **FR-CV-001** 系统必须允许用户上传 PDF 或 DOCX CV。
- **FR-CV-002** 系统必须保存原文件、提取文本、创建时间和版本关系。
- **FR-CV-003** 文本提取失败或疑似不完整时，系统必须要求用户确认或修正。
- **FR-CV-004** 用户必须能够将一个 CV Version 与一个或多个 Job 关联。
- **FR-CV-005** 每条评审建议必须包含目标原文、问题、判断依据和建议修改。
- **FR-CV-006** 用户必须能够逐项接受、拒绝或暂缓建议。
- **FR-CV-007** 系统只能根据用户接受的建议创建新版本，不得覆盖原版本。
- **FR-CV-008** AI 生成内容必须与用户原始事实和 AI 建议明确区分。
- **FR-CV-009** 系统不得编造用户未提供的经历、指标、技能或教育信息。
- **FR-CV-010** 第一版不要求内置富文本编辑器或直接生成最终 PDF/DOCX。
- **FR-CV-011** 第一版必须同时支持不关联 Job 的通用 CV 健康检查，以及关联具体 Job 的定向 Review。
- **FR-CV-012** 第一版不要求 OCR；扫描件或无法可靠提取的文件必须进入用户文本确认/修正流程。
- **FR-CV-013** 用户接受建议后，系统必须创建可复制的结构化文本新版本，不得覆盖原文件或原文本。

### 6.5 Behavioral

- **FR-BHV-001** 系统必须允许用户按能力主题管理 Behavioral Question。
- **FR-BHV-002** 系统必须允许用户建立并复用 STAR Evidence。
- **FR-BHV-003** 系统必须允许一个答案引用一个或多个 STAR Evidence。
- **FR-BHV-004** 系统必须保存答案草稿及版本。
- **FR-BHV-005** 系统反馈必须覆盖 STAR 结构完整性和证据具体性。
- **FR-BHV-006** 系统必须将缺失的 STAR 部分和泛化表述指出，并引用对应答案片段。
- **FR-BHV-007** 系统不得用未经校准的百分制总分表示面试成功概率。
- **FR-BHV-008** 用户必须能够接受、拒绝或忽略 AI 建议。
- **FR-BHV-009** 第一版只支持文本练习，不要求语音录制和语音分析。
- **FR-BHV-010** 第一版必须提供按常见能力主题整理且有来源的内置题库，并允许用户新增、编辑和归档自定义题目。

### 6.6 Action 与首页

- **FR-ACT-001** 系统必须使用统一 Action 表示来自 Job、Roadmap、CV 和 Behavioral 的待办行动。
- **FR-ACT-002** 每个 Action 必须保留来源域、目标对象、生成原因、状态和优先级依据。
- **FR-ACT-003** 首页必须优先展示准备任务和 `New` Job。
- **FR-ACT-004** 用户必须能够从 Action 进入对应 Job、Roadmap Task、CV Review 或 Behavioral Answer。
- **FR-ACT-005** 用户完成源对象操作后，相关 Action 必须同步完成或重新计算。
- **FR-ACT-006** 系统不得仅因 AI 建议自动创建不可撤销的高优先级 Action。
- **FR-ACT-007** Action 默认优先顺序必须依次考虑截止日期、是否阻塞其他任务、用户置顶和系统建议，并展示命中的依据。

## 7. 页面级需求

第一版主导航固定为：

```text
Home / Jobs / Roadmap / CV / Interview / Profile
```

### 7.1 Home

- 展示当前最高优先级的准备任务；
- 展示尚未处理的 `New` Job；
- 说明每项行动为何现在优先；
- 不在首页堆叠完整分析报告或无行动意义的统计图。

### 7.2 Jobs

- 提供 Inbox、筛选、岗位详情、来源证据、Gate/Rank 解释和状态操作；
- 区分确定重复与可能重复；
- 明确显示岗位有效性及最后检查时间；
- 提供快速保存 URL 入口。
- 提供 SourceRun/数据源诊断入口；站点失败不在岗位列表显示为岗位。

### 7.3 Roadmap

- 展示阶段、任务依赖、阻塞、完成标准和进度；
- 用户可以查看系统模板任务与自定义任务的区别；
- 用户可以修改任务，但系统保留来源和变更记录。

### 7.4 CV

- 展示 CV Version 列表、关联 Job 和 Review 状态；
- Review 页面逐项展示原文、问题、依据、建议及接受/拒绝操作。

### 7.5 Interview

- 展示题库、STAR Evidence 和答案草稿；
- 反馈以结构缺口和证据缺口呈现，不显示虚假成功概率。

### 7.6 Profile

- 管理教育时间、工作许可、目标岗位、技能和经历；
- 标识敏感字段及其对筛选、Roadmap 和 AI 的影响。

## 8. 数据需求

### 8.1 核心实体

系统至少需要表达：

- `Profile`、`EducationPeriod`、`WorkAuthorization`、`TargetRole`、`SkillEvidence`；
- `RoadmapTemplate`、`Roadmap`、`RoadmapTask`、`TaskDependency`；
- `Source`、`SourceRun`、`RawPosting`、`CanonicalJob`、`JobDecision`、`RuleEvidence`；
- `CVDocument`、`CVVersion`、`CVReview`、`CVSuggestion`；
- `BehavioralQuestion`、`STAREvidence`、`BehavioralAnswer`、`AnswerFeedback`；
- `Action`；
- `AIConsent`、`AIInvocation`。

### 8.2 数据原则

- 来源原始数据与标准化数据必须分离；
- 用户决定与后台同步状态必须分离；
- CV 和答案使用不可覆盖的版本记录；
- 所有 AI 输出必须可追溯到输入版本、模型、提示版本、时间和用户决定；
- 删除敏感文件时必须明确处理其提取文本、派生版本和 AI 调用记录；
- 演示数据必须与真实个人数据分离，真实数据不得提交到 Git。
- 真实数据无自动清理期限；只有用户确认影响范围后才能手动删除。
- 删除敏感对象时必须级联硬删除文件、提取文本和派生正文，只保留不含正文的删除审计事件。

## 9. 外部集成需求

### 9.1 数据源统一约束

每个 Source Adapter 必须：

- 声明来源、使用方式和合规依据；
- 支持超时、有限重试和错误记录；
- 输出统一 RawPosting；
- 提供来源稳定 ID（若存在）、规范 URL 和更新时间；
- 不因单个岗位失败而丢弃整个运行结果；
- 能够被独立启用、停用和手动重跑。

### 9.2 FreeHire API

M1 主自动来源。Phase 0 验证（2026-07-29）结论：**采用**。

- 端点：`GET https://freehire.me/api/v1/jobs/search`；
- 爱尔兰过滤：`countries=ie`，配合 `q`、`category`、`seniority`、`cities` 等 facet；
- 稳定标识：`public_slug`；申请 URL 多指向 Greenhouse 等 ATS；
- 分页：`limit`/`offset`，服务端 `offset + limit ≤ 10000`；
- 验证结果：8 组 IE 查询，去重后 1,345 条，0 查询错误；
- 风险：部分网络环境连接超时；adapter 须支持重试与独立停用；
- 详细记录见 `delta/1.req/job/datasource-validation.md`。

### 9.3 JobSpy

M1 补充自动来源。Phase 0 验证结论：LinkedIn + Indeed 当前可产出数据；运行时默认尝试全部受支持站点。

- 接入方式：本地 Python subprocess / worker，非 HTTP API；
- 默认站点：`linkedin`、`indeed`、`zip_recruiter`、`glassdoor`、`google`、`bayt`、`naukri`、`bdjobs`，并允许逐站点停用；
- 参数：`location=Ireland`，`country_indeed=Ireland`，可配置 `search_term` 与 `results_wanted`；
- 验证结果：222 条唯一岗位（LinkedIn 115 + Indeed 107）；ZipRecruiter/Glassdoor 403，Naukri recaptcha；
- 风险：反爬、429、403、captcha 与 ToS；须限速、错误隔离、不丢弃整次 SourceRun，失败只显示在诊断中；
- 脚本参考：`scripts/datasource/jobspy_fetch.py`。

### 9.4 用户保存 URL

- 用户主动保存的公开 URL 与自动源 **按规范 URL 硬去重**；
- 不自动登录或模拟用户操作；
- 页面读取前须检查适用条款；不允许时只保存用户输入和链接。

### 9.5 已弃用来源（M1 不采用）

- **EURES**：爱尔兰 SWE 噪声高，JobsIreland 链接可达性差；不再验证或接入；
- **独立 Greenhouse/Lever 探针**：由 FreeHire 聚合覆盖；后续如需单公司直连可在 M1+ 作为可选 adapter。

## 10. AI 使用需求

- AI 必须是可选能力；Job 同步、去重、Gate、基本 Rank 和 Roadmap 不依赖 AI 也能运行；
- 首次向某 provider 发送某一类敏感数据（CV、经历、答案）前，必须展示数据范围并获得分类授权；授权可随时撤销；
- 用户必须能够脱敏或取消调用；
- 调用必须保存 provider、model、时间、输入版本、提示版本、成本元数据和结果；
- 相同输入和版本应优先使用缓存；
- AI 失败不得导致原始数据或用户编辑丢失；
- AI 建议不得自动改变 Job Decision、CV 原文或已完成任务。

## 11. 非功能需求

### 11.1 隐私与安全

- 第一阶段只在用户本机运行，默认不设置账号系统；
- 服务默认只监听本地接口，公开绑定必须由用户显式配置；
- 密钥通过环境变量或本地忽略配置提供，不得提交到仓库；
- CV、Profile、申请决定和面试答案视为敏感数据；
- 日志不得记录完整 CV、完整答案、密钥或不必要的个人信息；
- 外部 URL 和上传文件必须进行输入验证。

### 11.2 可靠性

- 导入和同步必须幂等；
- 外部调用必须设置超时、有限重试和可诊断错误；
- 单一数据源失败不得阻止用户访问已有数据或使用其他来源；
- 后台同步不得覆盖用户决定；
- 本地数据库需要可执行的导出与恢复方案，量化周期在设计阶段确定。
- 系统必须支持手动完整导出，并默认每周生成一个可恢复的本地备份。
- 过期岗位必须归档并默认隐藏，但保留 RawPosting、来源证据和用户决定，直到用户手动删除。

### 11.3 可解释性

- Gate、Rank、Roadmap 优先级、CV 建议和 BQ 反馈必须展示依据；
- 系统必须区分事实、规则判断、AI 建议和用户决定；
- 用户能够覆盖系统判断，系统保存覆盖原因。

### 11.3a 性能容量

- 在本地累计不超过 10,000 个 CanonicalJob 时，岗位列表、筛选和排序操作应在 2 秒内返回。
- 不依赖外部调用的普通页面操作应在 1 秒内返回。
- 每日职位同步在正常网络下应于 30 分钟内完成；单站点失败不得阻塞其他站点。

### 11.4 可维护性

- 数据源通过统一 adapter 扩展；
- 核心业务不得依赖 EURES、LinkedIn 或特定 ATS 字段；
- 领域规则和外部适配器必须可独立测试；
- 规则、模板和提示必须版本化。

### 11.5 国际化与可访问性

- UI 支持中文和英文切换；
- JD、CV、BQ 等求职材料保留其原始语言，第一阶段主要为英文；
- 不应将机器翻译结果静默覆盖原文；
- 键盘操作、语义标签、颜色对比等具体标准在前端设计阶段量化。

### 11.6 可观测性

- SourceRun 和 AIInvocation 必须具有可检索的运行记录；
- 外部请求日志至少包含 request ID、source、运行 ID、结果和耗时，不记录敏感正文；
- 健康检查、指标和告警阈值在设计阶段确定。

## 12. 核心业务规则

1. 用户决定的优先级高于后台同步和 AI 建议。
2. 硬去重只使用可靠来源 ID 或规范 URL；模糊相似只能提示。
3. 无法确认岗位失效时标记待确认，不自动删除。
4. 未通过 Gate 的岗位仍可由用户查看和覆盖，不得无痕丢弃。
5. Roadmap 模板更新不得覆盖用户完成记录和自定义内容。
6. CV、BQ 和 AI 输出不得编造个人事实。
7. 云端 AI 按敏感数据类别授权；首次发送前明确同意，撤销后不得继续发送。
8. JobSpy 等商业板抓取须逐站点可配置、可停用，且不得自动登录、绕过验证码或提交申请。
9. 所有跨域 Action 必须能追溯到源对象和生成原因。
10. 系统推荐不得自动变成 Shortlist；用户决定始终优先。
11. Ireland 工作许可不得被推断为其他 EU 国家工作许可。

## 13. 验收摘要

### 13.1 Milestone 1

- 用户完成 Profile 后可获得个性化 Roadmap；
- 首页能够展示有原因的优先准备任务；
- 至少 **FreeHire** 或 **JobSpy（LinkedIn/Indeed）** 之一能够连续运行并产出爱尔兰软件岗位；
- 用户能保存 URL、查看新职位并完成状态决策；
- 重复同步不产生重复 Job；
- 每个岗位能够显示来源、有效性、Gate 和 Rank 依据；
- 连续两周运行满足第 1.5 节成功标准。

### 13.2 Milestone 2

- 用户可上传并确认 CV 文本；
- 用户可针对 Job 获得可追溯建议；
- 接受/拒绝操作可保存；
- 新版本不覆盖原版本；
- 未确认时敏感内容不会发送至云端。

### 13.3 Milestone 3

- 用户可建立 STAR Evidence 并复用于不同题目；
- 答案反馈能够明确指出 STAR 和证据缺口；
- 用户修改历史可追溯；
- 未完成练习能够进入今日 Action。

## 14. 目标—领域—里程碑追踪

| 产品目标 | 主要领域 | 里程碑 |
|---|---|---|
| 明确今天最该做什么 | Action、Roadmap | M1 |
| 发现真实且符合资格的岗位 | Job、Profile | M1 |
| 减少重复浏览和重复职位 | Job | M1 |
| 针对岗位改进 CV | CV、Job、Profile | M2 |
| 系统准备 Behavioral Question | Behavioral、Profile | M3 |
| 保持用户控制与数据隐私 | 全部领域 | M1–M3 |

## 15. 主要风险

| 风险 | 当前控制方式 |
|---|---|
| FreeHire 网络不可达或 API 变更 | adapter 隔离、重试、可停用；JobSpy 作补充 |
| JobSpy 反爬 / 403 / ToS | 分站点启用、限速、错误记录、不丢弃整次运行 |
| 双源 URL 重复 | 规范 URL 硬去重；FreeHire slug 作辅键 |
| Rank 缺乏校准数据 | 分维度展示、保留证据和用户反馈，不宣称客观概率 |
| AI 泄露或编造个人信息 | 按敏感数据类别授权、可撤销、脱敏、事实与建议分离、版本追踪 |
| 四个业务域导致范围过大 | 按 M1 → M2 → M3 交付，每个里程碑保持可运行 |

