# Job 流程说明

- 状态：已细化
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 用例：[usecase-desc.md](usecase-desc.md)
- 图示：[workflow.puml](workflow.puml)
- 数据源验证记录（本次不修改）：[`datasource-validation.md`](datasource-validation.md)

## 1. 端到端流程

1. 每日调度或用户手动重跑为每个来源/站点创建独立 `SourceRun`。
2. FreeHire 主源与 JobSpy 默认启用的全部受支持站点输出 `RawPosting`（all-sites 默认）；站点错误只写诊断。搜索词默认自 Profile 活动 `TargetRole.roleName` 派生，用户可在 Source 设置增删覆盖。
3. 系统先持久化原始记录，再执行标准化和硬去重；只有可靠 stable ID 或规范 URL 可以自动合并。
4. 仅相似的记录建立“可能重复”关系，等待用户确认。
5. 系统依据 Profile 时间线、预期入职日及岗位证据执行 Gate。
6. Gate 通过或被用户覆盖可考虑的岗位进入 explainable Rank，得到 `High/Medium/Low` tier。
7. 新岗位进入 `New`；Gate 失败保留但默认隐藏；未知/待确认保留并可筛选。
8. 用户查看证据后手动选择 `Shortlisted`、`Ignored`、`Applied` 或 `Archived`；系统从不自动 Shortlist 或申请。
9. 有效性刷新仅在有明确关闭证据时归档；网络错误保持待确认。
10. Job 变化发布给 Action 域，由其生成、完成或重算相关 Action。

## 2. 来源运行子流程

### 2.1 正常路径

`SCHEDULED/MANUAL → RUNNING → SUCCEEDED`

- SourceRun 在任何外部调用前创建；
- 每条 RawPosting 使用 `(sourceId, sourceStableId)` 或等价抓取幂等键；
- 运行完成时保存请求数、接收数、有效数、新增/更新/失败数和耗时。

### 2.2 部分成功

`RUNNING → PARTIAL_SUCCESS`

- 某站点出现 403、429、captcha、超时或解析失败时，记录错误类别、脱敏消息、重试次数和时间；
- 其他站点及已解析记录继续；
- 失败输入不得创建占位 CanonicalJob，也不得污染 Jobs 列表。

### 2.3 整体失败与重跑

`RUNNING → FAILED`；手动重跑创建新的 `MANUAL` SourceRun，不修改旧运行。重跑命中相同原始记录时幂等更新。已有职位和用户决定保持可用。

## 3. 标准化与决策子流程

### 3.1 顺序约束

顺序固定为：

`RawPosting 持久化 → 字段标准化 → 硬去重/可能重复 → Gate → Rank → Inbox 投影`

不得在去重前 Gate，以免同一岗位形成冲突决定；不得用 fuzzy 结果自动合并。

### 3.2 Gate 决策表

| 维度 | 通过 | 待确认/未知 | 失败 |
|---|---|---|---|
| 地点 | Ireland；EU 且明确当地签证支持；明确可从 Ireland 合法远程雇佣 | EU 默认；地点未知 | 其他非 Ireland |
| 工作授权 | Profile 在预期入职日具备适用授权 | **预期入职日缺失**或授权事实不足 | 预期入职日明确无授权且无支持 |
| 职级 | Intern、Graduate、Junior、未说明 | 无 | Senior、Lead、Staff、Principal、Manager |
| 语言 | English、未说明；或 Profile `LanguageProficiency` 覆盖全部强制语言 | 强制性无法判断 | 存在 Profile 缺失的强制语言 |

聚合原则：任一维度失败则 Gate `FAILED`；无失败但至少一项待确认/未知则分别为 `NEEDS_CONFIRMATION/UNKNOWN`；其余为 `PASSED`。每项均生成 `RuleEvidence`。

### 3.3 Rank

只对 Gate `PASSED` 或有有效用户覆盖的岗位排名。系统对以下五个因素分别给出 `POSITIVE/NEUTRAL/NEGATIVE` 及独立 `RuleEvidence`：

1. 目标岗位与目标职级接近度；
2. Profile 中可引用的技能证据；
3. 岗位发布时间/发现时间的新鲜度；
4. 地点与 remote-from-Ireland 的适配；
5. 对早期职业发展的成长性。

**聚合投票规则**（不得生成隐藏综合分）：
- `HIGH`：≥3 因素为 `POSITIVE` 且 0 因素为 `NEGATIVE`；
- `LOW`：≥2 因素为 `NEGATIVE`；
- 否则 `MEDIUM`。

输出仅为 `High/Medium/Low` tier、各因素方向和事实依据。

## 4. Job 业务状态与技术状态

### 4.1 用户业务状态

| 状态 | 含义 | 进入方式 | 退出方式 |
|---|---|---|---|
| `New` | 尚未处理 | 新 CanonicalJob 默认 | 用户选择其他状态 |
| `Shortlisted` | 用户明确列入候选 | 仅用户操作 | 用户改为其他状态 |
| `Ignored` | 用户暂不考虑 | 仅用户操作 | 用户恢复/改状态 |
| `Applied` | 用户已在外部申请 | 仅用户操作 | 用户归档或纠正 |
| `Archived` | 不在活动列表 | 用户操作或明确过期 | 用户恢复 |

用户状态与 Gate、有效性分开保存。任何同步、规则重算、tier 变化都不得覆盖用户决定。

### 4.2 有效性状态

- `ACTIVE`：有明确仍开放证据；
- `NEEDS_CONFIRMATION`：来源冲突或需人工确认；
- `UNKNOWN`：检查失败或证据不足；
- `EXPIRED`：有明确关闭/过期证据。

`EXPIRED` 触发默认隐藏和归档投影，但必须保留原始记录、规则证据和用户决定；403/captcha/超时不能单独证明过期。

### 4.3 默认可见性

- 默认显示：非 Archived、非 Expired、Gate 通过或非失败的活动职位；
- 默认隐藏：Gate Failed、其他非 Ireland、Expired/Archived；
- 始终可通过显式筛选检查隐藏、未知、待确认与归档记录。

## 5. 用户覆盖流程

1. 用户查看失败维度、规则版本和原始岗位证据。
2. 用户选择覆盖，并填写非空原因。
3. 系统追加 `JobDecision`，不修改原 `RuleEvidence`。
4. 覆盖使岗位可进入 Rank/考虑范围，但不等同于 `Shortlisted`。
5. Profile 或规则变化时可重算系统结论；有效覆盖与历史决定继续保留并提示用户复核。

## 6. 失败、幂等与可观察性

- SourceRun、RawPosting、CanonicalJob、RuleEvidence、JobDecision 分层保存，禁止用来源错误伪造职位。
- 运行重试、重复分页、双源命中不得产生重复 CanonicalJob。
- 单条解析失败仅增加 item error；单站点失败不撤销成功站点事务。
- 诊断至少可按 run、source、site、error category 和时间查询。
- 外部日志不得保存密钥或不必要的完整响应/JD 正文。

## 7. 流程—用例—FR 跟踪

| 流程 | 用例 | FR |
|---|---|---|
| 每日/手动同步与诊断 | UC-JOB-001、008、009 | FR-JOB-001、002、004、004a、005、016、017 |
| 保存 URL | UC-JOB-002 | FR-JOB-003、005、007、018 |
| 标准化、去重、Gate、Rank | UC-JOB-003、006 | FR-JOB-006～011 |
| Inbox 与决定 | UC-JOB-004、005 | FR-JOB-010～013、018、019 |
| 有效性与归档 | UC-JOB-007 | FR-JOB-014～016 |
