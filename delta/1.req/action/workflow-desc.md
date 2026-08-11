# Action 流程说明

- 状态：已细化
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 跨域事件契约：[../cross-domain-events.md](../cross-domain-events.md)
- 用例：[usecase-desc.md](usecase-desc.md)
- 图示：[workflow.puml](workflow.puml)

## 1. 端到端流程

1. Job、Roadmap、CV 或 Behavioral 域按 [cross-domain-events.md](../cross-domain-events.md) 发布带 event ID 和对象版本的变化；Profile 变更经 Roadmap 重算间接影响 Action。
2. Action 域幂等接收事件，定位对应 `active=true` 的活动 Action。
3. 生成规则判断应创建、更新、完成、作废还是保持 Action。
4. 系统保存生成原因、deadline、blocker、system suggestion 等事实。
5. 优先级引擎按 deadline → blocker → user pin → system suggestion 生成可解释排序键。
6. Home 展示少量可执行 Action，并单独展示 `New` Job。
7. 用户进入源对象完成实际工作，或对 Action 执行 pin、ignore、complete、restore。
8. 源对象的新事件再次闭环更新 Action；Roadmap 可能从同一事件追加任务，但 **Home 待办始终只由 Action 域维护**。

## 2. Action 生成与幂等

### 2.1 生成键

活动 Action 使用以下业务唯一键：

`(sourceDomain, targetType, targetId, actionKind, generationRuleVersion, active=true)`

同一事件或相同业务键不得生成重复活动 Action。若规则版本改变，应关闭/替代旧 Action（`active=false`）并记录 `supersedesActionId`，而不是无痕覆盖。

### 2.2 领域输入示例

| 源域事件 | actionKind | 完成/关闭条件 |
|---|---|---|
| Job 进入 `New` | `REVIEW_NEW_JOB` | SOURCE_EVENT：Shortlisted / Ignored / Applied / Archived；或 USER_CONFIRMED |
| Job Gate 待确认 | `RESOLVE_GATE` | SOURCE_EVENT：Gate 可确定或用户覆盖；或 USER_CONFIRMED |
| Roadmap Task 可执行 | `COMPLETE_ROADMAP_TASK` | SOURCE_EVENT：Task `COMPLETED`；或 USER_CONFIRMED |
| CV Review 有待决建议 | `REVIEW_CV_SUGGESTIONS` | SOURCE_EVENT：建议均已决定/defer 且不再开放；或 USER_CONFIRMED |
| Behavioral Answer 缺 STAR/证据 | `COMPLETE_BEHAVIORAL_ANSWER` | SOURCE_EVENT：`READY_FOR_FEEDBACK` 或更后且含证据链接；或 USER_CONFIRMED |

不得生成“自动投递”或会隐式改变 Job 为 Applied 的 Action。

## 3. 优先级计算

### 3.1 比较顺序

1. **Deadline**：逾期优先于未逾期；越临近越优先。没有 deadline 不得伪造日期。
2. **Blocker**：能解除其他高价值/临近截止工作的 Action 优先；被阻塞且当前不可执行的 Action 不进入普通可执行序列。
3. **User pin**：用户显式 pin 的 Action 在没有更高 deadline/blocker 依据时优先。
4. **System suggestion**：根据确定性规则提出的可撤销建议，仅作为最后一层依据。
5. **稳定次序**：同级按 deadline、创建时间、Action ID 排序，保证重算可复现。

### 3.2 解释要求

每次计算保存 `ActionPriorityEvidence`：类别、事实值、来源对象、规则版本、说明和计算时间。UI 显示主依据及次要依据，不显示不透明综合分数。

### 3.3 边界

- user pin 不得覆盖真实 deadline/blocker 的更高优先级，但必须保留并展示；
- system suggestion 不得撤销 pin、ignore 或用户完成决定；
- AI 建议只能作为 `SYSTEM_SUGGESTION` 事实，不能创建不可撤销高优先级；
- 排序只表达“建议先做什么”，不代表系统执行该行动。

## 4. 状态机

| 状态 | 含义 | 允许迁移 |
|---|---|---|
| `OPEN` | 活动且可评估 | → COMPLETED、IGNORED、BLOCKED、STALE |
| `BLOCKED` | 当前依赖未满足 | → OPEN（解除阻塞）、IGNORED、STALE |
| `COMPLETED` | 源条件满足（SOURCE_EVENT）或用户确认（USER_CONFIRMED） | → OPEN（明确恢复/新实质变化） |
| `IGNORED` | 用户选择不处理（USER_CONFIRMED） | → OPEN（仅用户 RESTORE 或规则定义的重大变化并经提示） |
| `STALE` | 目标不可用或依据过期 | → OPEN（修复并重算）、IGNORED |

`pinned` 是独立用户属性，不是状态。`COMPLETED/IGNORED` 对应 `active=false`，默认不在 Home 活动列表，但历史可查。

### 4.1 关闭模式

- **SOURCE_EVENT**：消费 cross-domain-events 中定义的关闭条件；设置 `completionMode=SOURCE_EVENT`。
- **USER_CONFIRMED**：用户执行 `COMPLETE` 或 `IGNORE`；设置 `completionMode=USER_CONFIRMED`，追加 `ActionDecision`。

## 5. 用户操作流程

### 5.1 导航

Action 必须包含稳定的 `TargetRef`。点击后进入对应 Job、Roadmap Task、CV Review 或 Behavioral Answer。导航本身不完成 Action，也不改变源对象状态。

### 5.2 Pin / Ignore / Complete / Restore

- 允许操作：`PIN` / `UNPIN` / `COMPLETE` / `IGNORE` / `RESTORE`（无 `START`）。
- 每次操作追加 `ActionDecision`，包含操作、原因、用户、时间和目标版本。
- pin/unpin 只改变排序依据。
- ignore/complete 不得伪造源对象变化；若 Action 要求源对象操作，UI 必须区分“用户确认完成”与“源对象自动满足”。
- restore 恢复评估，但目标已失效时先进入 `STALE`。

## 6. 失败与恢复

- **重复事件**：按 event ID 去重，无额外 Action。
- **乱序事件**：目标版本较旧则忽略并记录；不得回滚当前状态。
- **领域暂不可用**：已有 Action 继续可见，目标导航提示降级；不删除数据。
- **处理失败**：记录消费状态与错误，可安全重试。
- **无效引用**：Action 进入 `STALE`，保留用户决定和生成证据。
- **并发用户操作**：使用版本校验；冲突时返回最新状态，不做最后写入静默覆盖。

## 7. Home 展示规则

- 优先展示非完成、非忽略、非 stale 的可执行准备 Action；
- `BLOCKED` 项默认不混入可执行列表，应展示解除它的 Action 或阻塞说明；
- `New` Job 独立展示，不因 High tier 自动 Shortlist；
- Action 数量与完整分析报告不在 Home 堆叠，详情通过解释或源对象页面查看。

## 8. 流程—用例—FR 跟踪

| 流程 | 用例 | FR |
|---|---|---|
| 聚合与 Home 展示 | UC-ACT-001 | FR-ACT-001～003、006、007 |
| 导航源对象 | UC-ACT-002 | FR-ACT-002、004 |
| 跨域事件同步 | UC-ACT-003 | FR-ACT-001、002、005、006 |
| 用户控制 | UC-ACT-004 | FR-ACT-002、005、006 |
| 优先级解释 | UC-ACT-005 | FR-ACT-002、003、006、007 |
