# Action 实体说明

- 状态：已细化
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 跨域事件契约：[../cross-domain-events.md](../cross-domain-events.md)
- 图示：[entity.puml](entity.puml)

## 1. 聚合边界

`Action` 是聚合根，表达来自 Job、Roadmap、CV、Behavioral 的一个可执行行动。聚合内包含目标引用、生成证据、优先级证据、依赖和用户决定。Action 不拥有源对象，也不能直接改变源对象业务状态。

**Home Actions 由 Action 域唯一拥有**；Roadmap 及其他域不得维护独立的 Action 实体或 Home 待办投影。各源域发布最小领域事件目录（见 cross-domain-events.md），Action 幂等消费以创建、更新或关闭 Actions。

## 2. Action

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `actionId` | UUID | 主键、不变 |
| `sourceDomain` | `JOB/ROADMAP/CV/BEHAVIORAL` | 必填 |
| `targetRef` | `TargetRef` | 必填且精确指向一个源对象 |
| `actionKind` | string/受控枚举 | 必填；示例见 §2.1 |
| `title` | string | 用户可读、非空 |
| `status` | `OPEN/BLOCKED/COMPLETED/IGNORED/STALE` | 必填 |
| `active` | boolean | 活动 Action 为 true；关闭后为 false |
| `deadline` | timestamp? | 只保存有事实依据的期限 |
| `pinned` | boolean | 用户属性，默认 false |
| `pinnedAt` | timestamp? | pinned=true 时必填 |
| `completionMode` | `SOURCE_EVENT/USER_CONFIRMED`? | COMPLETED 时必填 |
| `generationRuleId/version` | string | 必填 |
| `sourceObjectVersion` | string/integer | 幂等及乱序判断 |
| `priorityBand` | `DEADLINE/BLOCKER/USER_PIN/SYSTEM_SUGGESTION/NORMAL` | 派生投影 |
| `prioritySortKey` | tuple/string | 确定性、可重建 |
| `createdAt/updatedAt/completedAt` | timestamp | 状态约束 |
| `supersedesActionId` | UUID? | 新规则替代旧 Action 时引用 |
| `version` | integer | 乐观并发控制 |

**业务唯一键**：

`(sourceDomain, targetType, targetId, actionKind, generationRuleVersion, active=true)`

### 2.1 actionKind 示例

| actionKind | 源域 | 典型 targetType |
|---|---|---|
| `REVIEW_NEW_JOB` | JOB | `CANONICAL_JOB` |
| `RESOLVE_GATE` | JOB | `CANONICAL_JOB` |
| `COMPLETE_ROADMAP_TASK` | ROADMAP | `ROADMAP_TASK` |
| `REVIEW_CV_SUGGESTIONS` | CV | `CV_REVIEW` / `CV_SUGGESTION` |
| `COMPLETE_BEHAVIORAL_ANSWER` | BEHAVIORAL | `BEHAVIORAL_ANSWER` |

**不变量**：
1. 每个 Action 必须能追溯到一个源域、目标对象和生成原因。
2. Action 不得以自动投递为 actionKind，也不得直接把 Job 变为 `Shortlisted/Applied`。
3. `pinned` 只能由用户决定改变；系统重算不得撤销。
4. `deadline` 只能来自明确事实，不得用系统建议伪造。
5. COMPLETED/IGNORED 历史不删除；重新开放必须有用户恢复或实质源变化证据。
6. 同一业务键最多一个 `active=true` 的 Action。
7. Action 关闭时 `active=false`；恢复或实质源变化创建新活动记录或重新激活，并保留历史。

## 3. TargetRef

| 属性 | 类型 | 约束 |
|---|---|---|
| `targetType` | `CANONICAL_JOB/ROADMAP_TASK/CV_REVIEW/CV_SUGGESTION/BEHAVIORAL_ANSWER/...` | 必填 |
| `targetId` | UUID/string | 必填 |
| `routeHint` | string? | 仅导航提示，不作为身份 |
| `focusKey` | string? | 指向 Gate、建议或缺口等页面区域 |

**不变量**：身份只由 type + ID 决定；route 变化不改变引用；目标不存在时 Action 转 `STALE` 而非删除。

## 4. ActionGenerationEvidence

**含义**：为何创建/更新/完成 Action 的事实与规则记录。

| 属性 | 类型 | 约束 |
|---|---|---|
| `generationEvidenceId` | UUID | 主键 |
| `actionId` | FK | 必填 |
| `eventId` | string | 幂等；同消费者范围唯一 |
| `eventType` | string | 必填 |
| `sourceObjectVersion` | string/integer | 必填 |
| `ruleId/ruleVersion` | string | 必填 |
| `reasonCode` | string | 必填 |
| `explanation` | string | 用户可读 |
| `facts` | JSON | 最小必要事实，不复制敏感正文 |
| `createdAt` | timestamp | 必填 |

**不变量**：事件重放不能创建重复证据或 Action；低于已处理版本的事件不能回滚当前投影。

## 5. ActionPriorityEvidence

**含义**：一次优先级计算中某个可解释因素。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `priorityEvidenceId` | UUID | 主键 |
| `actionId` | FK | 必填 |
| `calculationId` | UUID | 同次计算分组 |
| `factor` | `DEADLINE/BLOCKER/USER_PIN/SYSTEM_SUGGESTION` | 必填 |
| `factValue` | string/number/date | 必填 |
| `sourceRef` | TargetRef/Decision ref | 必填 |
| `effectiveOrder` | integer | 1=最高生效层 |
| `ruleVersion` | string | 必填 |
| `explanation` | string | 用户可读 |
| `calculatedAt` | timestamp | 必填 |

**不变量**：
1. 因素比较顺序固定为 deadline → blocker → user pin → system suggestion。
2. 当前排序必须能仅根据当前一组证据重建。
3. system suggestion 必须标明建议性质，不得保存为用户决定或不可撤销优先级。
4. 不允许以单一不透明总分替代因素证据。

## 6. ActionDependency

**含义**：一个 Action 的可执行性依赖另一个 Action 或可验证条件。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `dependencyId` | UUID | 主键 |
| `blockedActionId` | FK | 必填 |
| `blockingActionId` | FK? | 与 condition 二选一或并存 |
| `conditionRef` | string? | 外部源条件 |
| `status` | `ACTIVE/RESOLVED/WAIVED` | 必填 |
| `reason` | string | 必填 |
| `resolvedAt` | timestamp? | 终态必填 |

**不变量**：不得形成自依赖；活动依赖图不得形成环；被阻塞 Action 为 `BLOCKED` 时，应优先呈现可解除阻塞且可执行的 Action。

## 7. ActionDecision

**含义**：用户对 Action 的 pin、状态或恢复操作的追加式记录。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `actionDecisionId` | UUID | 主键 |
| `actionId` | FK | 必填 |
| `operation` | `PIN/UNPIN/COMPLETE/IGNORE/RESTORE` | 必填 |
| `reason` | string? | 可选；产品可对 ignore 要求填写 |
| `actor` | `USER` | M1 固定 |
| `basedOnVersion` | integer | 并发校验 |
| `createdAt` | timestamp | 必填 |
| `supersedesDecisionId` | UUID? | 纠正时引用 |

**不变量**：系统和 AI 不得伪造 ActionDecision；决定不覆盖删除；完成 Action 不等于完成源对象的其他业务动作，更不等于提交申请。

## 8. DomainEventReceipt

**含义**：跨域事件的幂等消费与故障恢复记录。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `eventId` | string | 必填 |
| `consumer` | string | 必填 |
| `sourceDomain` | `JOB/ROADMAP/CV/BEHAVIORAL/PROFILE` | 必填 |
| `targetId` | string | 必填；通常等于 `sourceObjectId` |
| `sourceVersion` | string/integer | 必填 |
| `status` | `RECEIVED/PROCESSED/FAILED/IGNORED_STALE` | 必填 |
| `errorCode` | string? | FAILED 时必填 |
| `receivedAt/processedAt` | timestamp | 状态约束 |

## 9. 关闭模式与生命周期

### 9.1 关闭模式

| `completionMode` | 触发 | 说明 |
|---|---|---|
| `SOURCE_EVENT` | 源条件满足 | 消费领域事件后自动关闭；追加 `ActionGenerationEvidence` |
| `USER_CONFIRMED` | 用户 `COMPLETE` 或 `IGNORE` | 追加 `ActionDecision`；不伪造源对象状态 |

**SOURCE_EVENT 关闭示例**：

- `REVIEW_NEW_JOB`：Job 用户决定为 Shortlisted / Ignored / Applied / Archived
- `RESOLVE_GATE`：Gate 可确定或用户覆盖决定
- `COMPLETE_ROADMAP_TASK`：RoadmapTask `COMPLETED`
- `REVIEW_CV_SUGGESTIONS`：开放建议均已决定或 defer 且不再开放
- `COMPLETE_BEHAVIORAL_ANSWER`：Answer 达 `READY_FOR_FEEDBACK` 或更后状态，且含证据链接

### 9.2 生命周期

- 创建：收到有效领域事件（见 cross-domain-events.md），先保存生成证据，`active=true`。
- 活动：`OPEN` / `BLOCKED` 且 `active=true`，可被 Home 查询。
- 关闭：`COMPLETED` / `IGNORED`，`active=false`；`STALE` 等待修复或归档查看。
- 恢复：仅用户 `RESTORE`，或源对象实质变化使原完成条件失效；必须追加证据。
- 所有实体默认保留历史；源对象归档不级联删除 Action，而是完成、stale 或隐藏其活动投影。

## 10. 关系与 FR 跟踪

- Action `1—1` TargetRef。
- Action `1—*` ActionGenerationEvidence、ActionPriorityEvidence、ActionDecision。
- Action 通过 ActionDependency 与其他 Action 建立有向无环依赖。
- DomainEventReceipt 可关联零个或多个受影响 Action。

| 实体 | 支撑需求 |
|---|---|
| Action、TargetRef | FR-ACT-001、002、004 |
| ActionGenerationEvidence、DomainEventReceipt | FR-ACT-002、005 |
| ActionPriorityEvidence | FR-ACT-002、003、006、007 |
| ActionDependency | FR-ACT-002、003、005 |
| ActionDecision | FR-ACT-005、006 |
