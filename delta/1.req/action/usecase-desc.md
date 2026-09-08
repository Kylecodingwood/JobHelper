# Action 用例说明

- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 跨域事件契约：[../cross-domain-events.md](../cross-domain-events.md)
- 对应需求：FR-ACT-001～FR-ACT-007，并追踪 FR-RDM-003～012、FR-JOB-012～023、FR-CV-006～013、FR-BHV-008～010
- 状态：已细化

## 用例索引

| 编号 | 名称 | 优先级 | 主参与者 |
|---|---|---|---|
| UC-ACT-001 | 查看今日优先 Action 和新职位 | P0 | 求职者 |
| UC-ACT-002 | 从 Action 进入源对象 | P0 | 求职者 |
| UC-ACT-003 | 根据源对象变化同步 Action | P0 | 系统 |
| UC-ACT-004 | 置顶、完成、忽略或恢复 Action | P0 | 求职者 |
| UC-ACT-005 | 解释 Action 优先级 | P0 | 求职者 |

## UC-ACT-001 查看今日优先 Action 和新职位

- **增补（2026-08）**：进入 Home 时服务端按 `today-priority-v1` **刷新三固定槽**（审岗 / 投递闭环 / 材料·公司），详见 [`entity.md`](entity.md) §2.2 与 `api-contract` Home 节。默认仍最多展示 3 条，其余折叠。

- **优先级**：P0
- **参与者**：求职者（主）、Job/Roadmap/CV/Behavioral 域
- **触发条件**：用户打开 Home，或主动刷新。
- **前置条件**：应用可读取 Action 投影和 Job `New` 投影；跨域数据暂时不可用时允许降级。
- **输入**：当前时间、状态筛选、展示数量；Action 的 deadline、blocker、user pin、system suggestion 依据。
- **主流程**：
  1. 系统查询未完成、未忽略且当前可执行的 Action。
  2. 按确定性优先级顺序排列：临近/逾期 deadline、解除 blocker、用户 pin、系统 suggestion。
  3. 同一优先级内使用稳定次序（deadline、创建时间、Action ID）保证可重复。
  4. Home 展示少量最高优先 Action，每项明确“为什么现在做”。
  5. 独立展示尚未处理的 `New` Job；Job 推荐不自动转为 `Shortlisted`。
- **备选流程**：
  - A1：无 Action 时展示空状态和可进入 Roadmap/Jobs 的入口。
  - A2：某域不可用时展示可用域结果和降级提示，不隐藏已知高优先 Action。
  - A3：被依赖阻塞的 Action 不作为普通可执行项；可显示其 blocker 关系和解除阻塞的 Action。
- **错误流程**：优先级证据缺失时 Action 进入最低可解释分组并标记需重算，不得产生虚假高优先级。
- **后置条件**：浏览不改变状态；排序结果及依据可追溯到同一计算版本。
- **业务规则**：
  - BR-ACT-001：优先级顺序为 deadline → blocker → user pin → system suggestion；用户 pin 是用户显式控制，不能被系统建议取消。
  - BR-ACT-002：系统建议只影响可撤销排序，不代表用户决定。
  - BR-ACT-003：首页优先展示准备任务和 `New` Job，不堆叠无行动意义的报告；**Home Actions 仅由 Action 域投影**。
  - BR-ACT-004：Action 不执行自动申请，也不自动 Shortlist Job。
- **关联 FR**：FR-ACT-001～003、006、007，FR-JOB-012、019、021。
- **验收场景**：
  - Given 一个明日截止 Action 和一个系统建议 Action，When 打开 Home，Then 截止 Action 排在前且显示截止依据。
  - Given 用户 pin 一个无截止 Action，When 同时存在解除 blocker 的 Action，Then blocker Action 优先，pin 依据仍可见。
  - Given 一个 High tier New Job，When 首页展示，Then 状态仍为 `New`，系统未自动 Shortlist。

## UC-ACT-002 从 Action 进入源对象

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户点击一个 Action。
- **前置条件**：Action 含合法 `sourceDomain`、`targetType` 和 `targetId`。
- **输入**：Action ID。
- **主流程**：
  1. 系统解析目标引用并校验目标存在。
  2. 导航到对应 Job、Roadmap Task、CV Review 或 Behavioral Answer。
  3. 目标页面聚焦与 Action 原因相关的对象或待处理项。
  4. 返回 Home 后刷新 Action 状态和优先级。
- **备选流程**：目标已归档时导航到只读/归档详情并提示状态。
- **错误流程**：目标已删除、引用无效或域暂不可用时，不进入错误页面；Action 标记 `STALE`/待重算并提供刷新。
- **后置条件**：导航本身不完成 Action、不改变 Job 状态。
- **业务规则**：每个 Action 必须精确追溯到一个源域目标及生成原因；禁止用外部申请链接作为“自动完成”动作。
- **关联 FR**：FR-ACT-002、004，FR-JOB-018、019。
- **验收场景**：
  - Given Action 指向一个 Gate 待确认 Job，When 点击，Then 打开该 Job 的 Gate 证据区域。
  - Given目标已归档，When 点击，Then 可查看归档详情且 Action 不被误判完成。

## UC-ACT-003 根据源对象变化同步 Action

- **优先级**：P0
- **参与者**：系统（主）、Job/Roadmap/CV/Behavioral 域
- **触发条件**：源对象创建、更新、完成、归档、解除/新增 blocker；或 Profile 变更经 Roadmap 重算后发布 Roadmap 事件（见 cross-domain-events.md）。
- **前置条件**：事件含 event ID、sourceDomain、eventType、sourceObjectId、occurredAt 和 sourceObjectVersion。
- **输入**：领域事件、Action 生成规则版本、既有 Action。
- **主流程**：
  1. 系统以 `sourceDomain + eventId` 幂等接收事件。
  2. 根据 `(sourceDomain, targetType, targetId, actionKind, ruleVersion, active=true)` 查找活动 Action。
  3. 若目标仍需行动，则创建或更新原因、deadline、blocker 和系统建议依据（actionKind 如 `REVIEW_NEW_JOB`、`RESOLVE_GATE`、`COMPLETE_ROADMAP_TASK`、`REVIEW_CV_SUGGESTIONS`、`COMPLETE_BEHAVIORAL_ANSWER`）。
  4. 若 SOURCE_EVENT 关闭条件满足，则标为 `COMPLETED`，`completionMode=SOURCE_EVENT`，`active=false`。
  5. 若源对象变化产生新要求，则创建新 Action 或经 RESTORE 规则重新开放，并保留历史。
  6. 重算优先级投影供 Home 使用。
- **备选流程**：
  - A1：重复事件不重复创建 Action。
  - A2：事件乱序时，低版本事件被忽略并记录。
  - A3：Job 被明确过期归档时，处理该 Job 的活动 Action 自动关闭；原 Action 保留历史。
- **错误流程**：跨域事件处理失败时进入可重试状态并记录诊断；不得删除既有 Action 或覆盖用户 pin/ignore 决定。
- **后置条件**：Action 与最新已处理源版本一致；处理可审计、可重放且幂等。
- **业务规则**：用户状态和 pin 高于系统重算；AI/system suggestion 不得创建不可撤销高优先 Action；源对象变化只能按明确规则完成/重算 Action。
- **关联 FR**：FR-ACT-001、002、005、006。
- **验收场景**：
  - Given 同一 Job 事件投递两次，When 处理，Then 只有一个活动 Action。
  - Given 用户已 pin Action，When 系统建议重算，Then pin 保留。
  - Given Job 从 New 变为 Shortlisted，When 事件处理，Then `REVIEW_NEW_JOB` Action 以 SOURCE_EVENT 完成，但不会自动创建“提交申请”操作。
  - Given Behavioral Answer 达 READY_FOR_FEEDBACK 且含证据链接，When 事件处理，Then `COMPLETE_BEHAVIORAL_ANSWER` Action 完成。

## UC-ACT-004 置顶、完成、忽略或恢复 Action

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户在 Home 或源对象页面操作 Action。
- **前置条件**：Action 存在且版本未过期。
- **输入**：操作 `PIN/UNPIN/COMPLETE/IGNORE/RESTORE`（无 `START`），可选原因，当前版本。
- **主流程**：
  1. 系统展示操作影响和当前生成原因。
  2. 用户选择操作；忽略可填写原因。
  3. 系统追加 `ActionDecision` 并更新 Action 投影。
  4. pin/unpin 触发优先级重算；complete/ignore 设置 `completionMode=USER_CONFIRMED`、`active=false` 并从默认活动列表移除；restore 恢复为可评估状态。
- **备选流程**：对于必须由源对象完成的 Action，用户手动 complete 时系统标记 USER_CONFIRMED，不伪造源对象状态。
- **错误流程**：并发版本冲突时拒绝覆盖并显示最新状态；对已作废目标的 restore 转为待重算。
- **后置条件**：用户决定可追溯，后台重算不得静默撤销。
- **业务规则**：用户可控制 Action，但完成 Action 不等于改变 Job 为 Applied；系统不得把 Action 点击或完成解释为外部申请。
- **关联 FR**：FR-ACT-002、005、006，FR-JOB-013、019。
- **验收场景**：
  - Given 用户忽略系统建议 Action，When 后续发生无实质变化的重算，Then Action 不自动重新出现。
  - Given 用户完成“准备申请材料”，When 保存，Then对应 Job 不自动变为 Applied。

## UC-ACT-005 解释 Action 优先级

- **优先级**：P0
- **参与者**：求职者
- **触发条件**：用户展开 Action 的“为何优先”。
- **前置条件**：Action 已有当前优先级计算结果。
- **输入**：Action ID、计算版本。
- **主流程**：
  1. 系统展示优先级类别及事实：deadline、被其解除的 blocker、user pin、system suggestion。
  2. 对多个依据按实际生效顺序解释，并显示来源对象。
  3. system suggestion 明确标注为建议，不显示虚假精确分数。
  4. 用户可从解释中 unpin、忽略或进入源对象。
- **备选流程**：无 deadline/blocker/pin 时，仅显示建议理由和规则版本。
- **错误流程**：依据已过期时提示重算，不展示陈旧结论为当前事实。
- **后置条件**：查看解释不改变排序或状态。
- **业务规则**：优先级必须可解释、确定性且可复现；不得只返回不透明总分。
- **关联 FR**：FR-ACT-002、003、006、007。
- **验收场景**：Given Action 同时被 pin 且有 deadline，When 查看解释，Then deadline 显示为主依据、pin 为次依据，并可追溯到用户操作。
