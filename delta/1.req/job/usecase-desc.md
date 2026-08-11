# Job 用例说明

- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 对应需求：FR-JOB-001～FR-JOB-023（含 004a、009a～009c）、FR-PRO-002～FR-PRO-005、FR-ACT-001～FR-ACT-007
- 状态：已细化，与 SRS v0.4 对齐

## 用例索引

| 编号 | 名称 | 优先级 | 主参与者 |
|---|---|---|---|
| UC-JOB-001 | 同步职位源 | P0 | 调度器、求职者 |
| UC-JOB-002 | 快速保存公开职位 URL | P0 | 求职者 |
| UC-JOB-003 | 标准化、去重、Gate 与 Rank | P0 | 系统 |
| UC-JOB-004 | 查看和筛选 Job Inbox | P0 | 求职者 |
| UC-JOB-005 | 作出或覆盖职位决定 | P0 | 求职者 |
| UC-JOB-006 | 处理可能重复职位 | P1 | 求职者 |
| UC-JOB-007 | 刷新岗位有效性并归档 | P0 | 调度器、求职者 |
| UC-JOB-008 | 查看来源运行诊断 | P0 | 求职者 |
| UC-JOB-009 | 手动重跑来源 | P1 | 求职者 |

## UC-JOB-001 同步职位源

- **优先级**：P0
- **参与者**：调度器（主）、求职者（手动触发时）、FreeHire、JobSpy 各站点
- **触发条件**：每日计划时间到达，或 UC-JOB-009 发出手动重跑请求。
- **前置条件**：至少一个 `Source` 启用；FreeHire 为主源；JobSpy 支持的全部站点默认启用且可逐站点停用；Profile 已存在。
- **输入**：运行类型、来源/站点、爱尔兰地点参数、搜索词（默认自 Profile 活动 `TargetRole.roleName` 派生，用户可在 Source 设置增删覆盖）、分页参数、超时与有限重试策略。
- **主流程**：
  1. 系统创建独立 `SourceRun`，记录规则/配置快照、搜索词（含 Profile 默认与用户覆盖）和开始时间。
  2. Adapter 调用来源；FreeHire 使用公开 API，JobSpy 按站点隔离执行。
  3. 每条成功记录以来源稳定 ID 和抓取时间写入幂等 `RawPosting`。
  4. 系统累计请求、成功、失败及限速统计，不因单条或单站点失败回滚其他结果。
  5. 成功记录进入 UC-JOB-003；运行结束后写入结束时间、状态和新增/更新/失败数量。
- **备选流程**：
  - A1：来源无新增结果，运行以 `SUCCEEDED` 完成并记录零计数。
  - A2：部分站点失败，成功站点继续处理，运行标记 `PARTIAL_SUCCESS`。
  - A3：同一运行重试取得相同记录，幂等更新既有 `RawPosting`，不创建重复 Job。
- **错误流程**：
  - E1：403、429、captcha、超时或响应格式错误时，记录结构化诊断与重试结果，不生成损坏条目。
  - E2：来源整体不可达时，运行标记 `FAILED`；已有 Jobs 仍可访问。
  - E3：Adapter 返回缺少最低必要字段的记录，记录 item-level error 并隔离该记录。
- **后置条件**：所有可用原始记录可追溯到 `SourceRun`；失败可在诊断视图查询；Jobs 列表不含解析失败的半成品。
- **业务规则**：
  - BR-JOB-001：每日自动同步一次，并支持手动重跑。
  - BR-JOB-002：FreeHire 是主源；JobSpy 所有受支持站点默认启用，但可独立停用。
  - BR-JOB-003：禁止自动登录、绕过 captcha 或自动申请。
  - BR-JOB-004：单来源、单站点或单记录失败必须隔离。
  - BR-JOB-012：搜索词默认来自 Profile 活动 `TargetRole.roleName`；用户可在 Source 设置增删覆盖项，合并去重后用于同步。
- **关联 FR**：FR-JOB-001、002、004、004a、005、016、017、019、022、023。
- **验收场景**：
  - Given LinkedIn 返回 403 且 FreeHire 成功，When 每日同步完成，Then SourceRun 为 `PARTIAL_SUCCESS`、含 403 诊断，且只展示可用职位。
  - Given 同一运行被安全重试，When 返回相同 stable ID，Then RawPosting 与 CanonicalJob 数量不增加。
  - Given JobSpy 某受支持站点未被用户配置，When 首次同步，Then 该站点按默认启用执行。
  - Given Profile 有活动目标 `Graduate Software Engineer` 与 `Backend Developer`，When 同步且 Source 无用户覆盖，Then 搜索词包含两者 roleName。

## UC-JOB-002 快速保存公开职位 URL

- **优先级**：P0
- **参与者**：求职者（主）、公开职位页面（可选）
- **触发条件**：用户提交公开职位 URL，可同时粘贴 JD。
- **前置条件**：URL 使用允许的协议；应用可接受本地输入。
- **输入**：URL、可选 JD、可选备注。
- **主流程**：
  1. 系统校验并规范化 URL。
  2. 以规范 URL 执行硬去重。
  3. 若无匹配，创建手动来源 `RawPosting`；若有匹配，将输入关联到既有 `CanonicalJob`。
  4. 有 JD 时保留用户原文并进入 UC-JOB-003；无 JD 时保留未知字段。
  5. 返回新建或已存在的 Job 详情。
- **备选流程**：页面条款不允许读取或页面不可达时，仅保存用户输入的 URL/JD，不模拟登录。
- **错误流程**：非法协议、空 URL 或超长/不可解析输入被拒绝，并返回可修正错误；不得创建 RawPosting。
- **后置条件**：输入可追溯，且不会仅因缺失抓取内容而伪造职位字段。
- **业务规则**：规范 URL 相同视为确定重复；用户粘贴的 JD 与抓取原文均不得被静默覆盖。
- **关联 FR**：FR-JOB-003、005、007、018、019。
- **验收场景**：
  - Given URL 已由 FreeHire 导入，When 用户再次保存等价规范 URL，Then 系统打开既有 Job 并增加来源证据，不创建新 Job。
  - Given 页面不可读取但用户提供 JD，When 保存，Then Job 保留该 JD 且来源标记为用户输入。

## UC-JOB-003 标准化、去重、Gate 与 Rank

- **优先级**：P0
- **参与者**：系统（主）、Profile（协作域）
- **触发条件**：新增/更新 RawPosting、Profile 变化、规则版本变化或用户请求重算。
- **前置条件**：RawPosting 已完整落盘；Profile 含时间线、工作授权、目标岗位、`LanguageProficiency` 和技能证据。
- **输入**：RawPosting、Profile、预期入职日期、规则版本、现有 CanonicalJob。
- **主流程**：
  1. 系统先标准化标题、公司、地点、语言、职级、日期、URL 和稳定 ID。
  2. 依次按同来源稳定 ID、规范 URL执行硬去重并合并来源引用。
  3. 仅相似的公司/标题/地点生成“可能重复”关系，不自动合并。
  4. Gate 逐项评估地点、预期入职日工作授权、职级、语言（对照 Profile `LanguageProficiency`），保存 `RuleEvidence`。
  5. Gate 通过的岗位按五个因素分别给出 `POSITIVE/NEUTRAL/NEGATIVE` 及证据，再按投票规则聚合为 `High/Medium/Low` tier（≥3 POSITIVE 且 0 NEGATIVE → High；≥2 NEGATIVE → Low；否则 Medium）。
  6. Gate 失败或未知的岗位仍保存；系统生成/更新 `JobDecision` 和展示可见性。
- **备选流程**：
  - A1：爱尔兰地点通过。
  - A2：EU 地点默认 `NEEDS_CONFIRMATION`；仅明确支持当地签证，或明确允许从爱尔兰合法远程雇佣时通过。
  - A3：地点未知则保留并标记 `UNKNOWN`。
  - A4：职级未说明或语言未说明时对应维度通过。
- **错误流程**：标准化或规则计算失败时保留 RawPosting，CanonicalJob 不进入普通 Inbox，并记录可诊断错误；不得生成臆测结论。
- **后置条件**：每项 Gate/Rank 结论均含规则版本、触发条件、事实证据和计算时间。
- **业务规则**：
  - BR-JOB-005：标准化/硬去重必须先于 Gate。
  - BR-JOB-006：地点：Ireland 通过；EU 默认待确认，满足明确签证支持或合法 remote-from-Ireland 才通过；其他非 Ireland 失败并默认隐藏；未知保留。
  - BR-JOB-007：工作授权按 Profile 时间线和岗位预期入职日评估；**预期入职日缺失时工作授权维度为 `NEEDS_CONFIRMATION`**，不得自动推导毕业日或 +90 天。
  - BR-JOB-008：Intern/Graduate/Junior/未说明通过；Senior/Lead/Staff/Principal/Manager 失败。
  - BR-JOB-009：English/未说明通过；岗位强制语言须能在 Profile `LanguageProficiency` 中找到对应记录，否则失败。
  - BR-JOB-010：Gate 失败记录保留、默认隐藏，可检查并覆盖。
  - BR-JOB-011：Rank 只针对 Gate 通过或用户覆盖为可考虑的岗位；五因素各持证据，按投票规则聚合 tier，不显示不透明总分。
- **关联 FR**：FR-JOB-006～011（含 009a～009c）、020、FR-PRO-002～005。
- **验收场景**：
  - Given Berlin 岗位仅写“EU applicants”，When Gate，Then 地点为 `NEEDS_CONFIRMATION`，岗位保留且不进入默认推荐。
  - Given Paris 岗位明确支持本地签证，When Gate，Then 地点维度通过。
  - Given remote 岗位明确可从 Ireland 合法雇佣，When Gate，Then 地点维度通过。
  - Given “Senior Software Engineer”，When Gate，Then 职级失败、岗位默认隐藏但可检查。
  - Given 强制德语且 Profile `LanguageProficiency` 无德语，When Gate，Then 语言失败。
  - Given 岗位无 `expectedStartDate`，When Gate 工作授权维度，Then 结果为 `NEEDS_CONFIRMATION`，系统不推导毕业日或 +90 天。
  - Given Gate 通过且五因素为 3 POSITIVE、1 NEUTRAL、1 NEGATIVE，When Rank，Then tier 为 `MEDIUM`。
  - Given Gate 通过且五因素为 3 POSITIVE、0 NEGATIVE，When Rank，Then tier 为 `HIGH`。
  - Given 两条记录只在标题/公司相似，When 去重，Then 仅标记可能重复，不自动合并。

## UC-JOB-004 查看和筛选 Job Inbox

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户打开 Jobs。
- **前置条件**：应用可读取已有 CanonicalJob。
- **输入**：状态、tier、Gate、来源、有效性、可能重复、是否显示隐藏/归档的筛选条件。
- **主流程**：
  1. 默认列出非归档、非 Gate 失败且未隐藏的职位，优先呈现 `New`。
  2. 每项展示标题、公司、地点、状态、有效性、tier、更新时间和关键依据。
  3. 用户进入详情查看原始来源、Gate/Rank 分项、可能重复和决策历史。
  4. 用户可前往原始页面自行申请。
- **备选流程**：用户显式开启“隐藏/失败/归档”筛选，检查被保留记录及原因。
- **错误流程**：单条来源链接失效时仍展示已保存详情并提示有效性，不影响列表其他项。
- **后置条件**：只读浏览不改变用户决定。
- **业务规则**：Gate 失败与 Expired 默认隐藏；未知/待确认必须可筛选；系统不宣称代替用户申请决策。
- **关联 FR**：FR-JOB-010～012、014、015、018～021。
- **验收场景**：Given 一个 Gate 失败 Job，When 使用默认筛选，Then 不显示；When 开启“Gate 失败”，Then 可查看完整证据和覆盖入口。

## UC-JOB-005 作出或覆盖职位决定

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户在列表或详情选择状态，或覆盖 Gate 结果。
- **前置条件**：目标 CanonicalJob 存在。
- **输入**：目标状态 `New/Shortlisted/Ignored/Applied/Archived`；覆盖类型；覆盖原因。
- **主流程**：
  1. 系统展示当前状态、Gate 结论及其证据。
  2. 用户手动选择 `Shortlisted`、`Ignored`、`Applied` 或 `Archived`。
  3. 若覆盖 Gate 失败，用户输入非空原因。
  4. 系统追加不可覆盖的 `JobDecision`，更新当前投影。
  5. 发布源对象变化供 Action 重算。
- **备选流程**：用户把先前状态恢复为 `New`，系统保留完整决定历史。
- **错误流程**：无原因的 Gate 覆盖被拒绝；并发修改时要求基于最新版本重试。
- **后置条件**：用户决定优先于后续同步、规则重算和系统建议。
- **业务规则**：系统只推荐，只有用户能 Shortlist 或标记 Applied；后台不得覆盖用户状态。
- **关联 FR**：FR-JOB-012、013、019，FR-ACT-005。
- **验收场景**：
  - Given 一个 High Job，When 同步再次命中，Then 用户的 `Shortlisted` 不变。
  - Given 一个地点 Gate 失败 Job，When 用户填写原因并覆盖，Then Job 可进入考虑范围且保留原 Gate 证据和覆盖历史。

## UC-JOB-006 处理可能重复职位

- **优先级**：P1
- **参与者**：求职者
- **触发条件**：用户打开带有可能重复标记的 Job。
- **前置条件**：存在 fuzzy duplicate relation，且未发生硬去重。
- **输入**：两个 Job 的来源、标题、公司、地点、描述与用户判断。
- **主流程**：
  1. 系统并列展示相似依据；标识较早创建的 Job 为默认 survivor 候选。
  2. 用户选择“确认同一职位”“不是重复”或“稍后处理”。
  3. 确认时：保留较早 `CanonicalJob` 为 survivor，合并双方全部 `RawPosting` 引用与来源历史。
  4. 若 `jobStatus` 或用户 `JobDecision` 冲突，系统询问用户选择，不自动覆盖。
  5. 非 survivor Job 归档；所有 RawPosting 保留。
- **备选流程**：用户暂不判断，两个 Job 独立保留且提示不消失。
- **错误流程**：合并目标已被并发归档/合并时，系统刷新关系并要求重新确认。
- **后置条件**：用户判断及原因可追溯；任何 RawPosting 不被删除。
- **业务规则**：fuzzy 永不自动合并；硬去重只使用稳定 ID/规范 URL；合并时 survivor 为较早 CanonicalJob，用户决定冲突须显式选择。
- **关联 FR**：FR-JOB-007、008。
- **验收场景**：
  - Given 同公司同标题但不同 URL，When 系统检测相似，Then 两个 Job 均保留直至用户确认。
  - Given 用户确认重复且两 Job 的 `jobStatus` 分别为 `New` 与 `Shortlisted`，When 合并，Then 系统询问用户保留哪一状态，不自动覆盖 `Shortlisted`。

## UC-JOB-007 刷新岗位有效性并归档

- **优先级**：P0
- **参与者**：调度器（主）、求职者
- **触发条件**：每日刷新、来源同步结果或用户手动检查。
- **前置条件**：CanonicalJob 至少有一个来源或申请 URL。
- **输入**：来源状态、申请链接检查结果、最后可见时间、当前用户状态。
- **主流程**：
  1. 系统检查来源与申请链接，记录检查时间及证据。
  2. 明确仍开放时标记 `ACTIVE`。
  3. 明确过期/关闭时标记 `EXPIRED`，职位状态转为 `Archived` 并默认隐藏。
  4. 保留 RawPosting、Gate/Rank、JobDecision 与历史状态。
- **备选流程**：无法确认时标记 `UNKNOWN/NEEDS_CONFIRMATION`，不归档、不删除。
- **错误流程**：403、captcha、超时仅产生诊断，不得据此判定 Expired。
- **后置条件**：有效性结论有时间与证据，历史数据完整。
- **业务规则**：只有明确失效证据可自动归档；用户可检查归档职位；用户 `Applied` 决定不得被抹除，可通过独立有效性/归档投影表达。
- **关联 FR**：FR-JOB-014～016。
- **验收场景**：
  - Given 链接检查超时，When 刷新，Then 有效性为待确认且 Job 未被删除。
  - Given 来源明确返回 closed，When 刷新，Then Job 归档且默认隐藏，原始数据和决定仍可查看。

## UC-JOB-008 查看来源运行诊断

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户打开来源诊断视图或从异常提示进入。
- **前置条件**：至少存在一个 SourceRun。
- **输入**：来源、站点、状态、时间范围。
- **主流程**：系统列出运行起止、配置快照、请求/记录计数、耗时和状态；用户进入详情查看按站点/错误类别分组的 403、429、captcha、超时、解析失败及重试信息。
- **备选流程**：成功运行展示零错误和新增/更新计数。
- **错误流程**：诊断正文含敏感响应内容时必须脱敏，不向 UI 暴露密钥或完整不必要正文。
- **后置条件**：查看诊断不改变 SourceRun 或 Job。
- **业务规则**：错误必须可检索并关联 SourceRun/Source；错误记录不得变成 Jobs 列表项。
- **关联 FR**：FR-JOB-016、018。
- **验收场景**：Given Glassdoor captcha，When 打开诊断，Then 可见站点、错误类别、发生时间和重试结果，Jobs Inbox 无对应损坏条目。

## UC-JOB-009 手动重跑来源

- **优先级**：P1
- **参与者**：求职者
- **触发条件**：用户在来源或 SourceRun 详情选择“重跑”。
- **前置条件**：来源已启用；当前无冲突的活动运行，或系统支持明确排队。
- **输入**：来源/站点范围；可选沿用原运行参数。
- **主流程**：系统展示将执行的来源与参数；用户确认；创建新的 `MANUAL` SourceRun；按 UC-JOB-001 执行并返回诊断入口。
- **备选流程**：已有运行时请求进入队列，并明确显示等待状态。
- **错误流程**：来源已停用或参数失效时拒绝执行并给出修正入口。
- **后置条件**：手动运行与每日运行独立可审计，处理仍保持幂等。
- **业务规则**：重跑不得复用旧 SourceRun ID；不得因重跑创建重复 RawPosting/CanonicalJob。
- **关联 FR**：FR-JOB-001、016、017。
- **验收场景**：Given 昨日运行失败，When 用户手动重跑成功，Then 产生新 SourceRun、旧诊断保留且职位不重复。
