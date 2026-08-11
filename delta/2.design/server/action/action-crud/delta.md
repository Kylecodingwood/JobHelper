# action-crud

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/action/usecase-desc.md`](../../../1.req/action/usecase-desc.md)
- 依赖：[`../action-model/delta.md`](../action-model/delta.md)、[`../../../1.req/cross-domain-events.md`](../../../1.req/cross-domain-events.md)
- 图示：[`service-action.puml`](service-action.puml)

## 1. 服务分层

| 服务 | 职责 |
|---|---|
| `ActionQueryService` | Home/列表查询、详情、优先级解释 |
| `ActionCommandService` | 用户 pin/complete/ignore/restore |
| `ActionPriorityService` | 优先级重算与 ActionPriorityEvidence |
| `ActionGenerationService` | 按规则创建/更新/关闭 Action |
| `DomainEventConsumerService` | 幂等消费跨域事件（workflow） |
| `ActionDependencyService` | blocker 依赖图 |
| `ActionStaleService` | 目标失效标记 STALE |

## 2. ActionQueryService

| 方法 | 说明 | 用例 |
|---|---|---|
| `listHomeActions(query)` | OPEN、非 BLOCKED、active=true；按 prioritySortKey | UC-ACT-001 |
| `getAction(actionId)` | 含 targetRef、generation 摘要 | UC-ACT-002 |
| `getPriorityEvidence(actionId, calculationId?)` | 因素解释 | UC-ACT-005 |
| `resolveNavigation(actionId)` | routeHint + focusKey | UC-ACT-002 |

**Home 查询默认**：`status IN (OPEN)`、`active=true`、排除 BLOCKED（或单独返回 blocked 说明）。

## 3. ActionCommandService

| 方法 | 说明 | 用例 |
|---|---|---|
| `pin(actionId, expectedVersion)` | PIN + pinnedAt | UC-ACT-004 |
| `unpin(actionId, expectedVersion)` | UNPIN | UC-ACT-004 |
| `complete(actionId, reason?, expectedVersion)` | USER_CONFIRMED；active=false | UC-ACT-004 |
| `ignore(actionId, reason?, expectedVersion)` | USER_CONFIRMED | UC-ACT-004 |
| `restore(actionId, expectedVersion)` | RESTORE；需规则允许 | UC-ACT-004 |

每次操作 **INSERT** `ActionDecision`；触发 `ActionPriorityService.recalculate`（pin 变更）。

**约束**：complete/ignore **不**修改 Job/Roadmap 源对象（FR-ACT-005 语义边界）。

## 4. ActionPriorityService

| 方法 | 说明 |
|---|---|
| `recalculate(actionId)` | 单条重算 |
| `recalculateAll()` | 批量（事件处理后） |
| `buildSortKey(action, evidences)` | 确定性 prioritySortKey |

**因素采集**：

| factor | 来源 |
|---|---|
| DEADLINE | action.deadline；Roadmap task dueAt |
| BLOCKER | ActionDependency：本 Action 解除他人阻塞的数量/权重 |
| USER_PIN | pinned=true → pinnedAt |
| SYSTEM_SUGGESTION | 规则建议分（如 HIGH rank New Job、openSuggestionCount） |

**比较顺序**：deadline → blocker → user pin → system suggestion → createdAt → actionId。

## 5. ActionGenerationService

| 方法 | 说明 |
|---|---|
| `upsertFromEvent(event, receipt)` | 总入口（consumer 调用） |
| `createAction(spec)` | 写 Action + TargetRef + GenerationEvidence |
| `updateAction(actionId, patch)` | deadline、title、sourceObjectVersion |
| `closeBySourceEvent(actionId, event, mode=SOURCE_EVENT)` | COMPLETED + evidence |
| `markStale(actionId, reason)` | STALE |
| `supersede(oldActionId, newSpec)` | 规则版本升级 |

**业务键查找**：`(sourceDomain, targetType, targetId, actionKind, generationRuleVersion, active=true)`。

## 6. Job 事件生成规则（M1）

| eventType | actionKind | 创建/更新 | SOURCE_EVENT 关闭 |
|---|---|---|---|
| `JOB_CREATED` | REVIEW_NEW_JOB | jobStatus=New | Shortlisted/Ignored/Applied/Archived |
| `JOB_STATUS_CHANGED` | REVIEW_NEW_JOB | — | 离开 New |
| `JOB_GATE_NEEDS_CONFIRMATION` | RESOLVE_GATE | gate 待确认/未知 | Gate PASSED 或 GATE_OVERRIDE |
| `JOB_DECISION_RECORDED` | RESOLVE_GATE / REVIEW_NEW_JOB | 更新 | 与 Gate/状态一致 |
| `JOB_ARCHIVED` / `JOB_EXPIRED` | * | 相关 active → STALE 或 COMPLETED | — |

Roadmap/CV/Behavioral 规则见 cross-domain-events.md §4.2～4.4（M1 实现 Job + Roadmap 事件处理骨架，CV/BHV 表结构预留）。

## 7. ActionDependencyService

| 方法 | 说明 |
|---|---|
| `linkBlocker(blockedId, blockingId, reason)` | ROADMAP_TASK_BLOCKED 等 |
| `resolveDependencies(actionId)` | 解除后 blocked→OPEN |
| `detectCycles()` | 启动/迁移校验 |

## 8. DomainEventConsumerService

| 方法 | 说明 |
|---|---|
| `consume(envelope)` | 见 action-workflow-event-consume |
| `recordReceipt(eventId, status)` | DomainEventReceipt |
| `isStaleVersion(targetId, version)` | IGNORED_STALE |

## 9. 与 Job 读模型

- Home **New Jobs**：**不**在本服务；调用 Job API `GET /api/v1/jobs/new`（FR-ACT-003）。
- Action 只拥有待办；Job 列表投影只读。

## 10. 事务

- 消费事件：Receipt + Action 变更同一事务。
- 失败：Receipt=FAILED，可重试；不覆盖用户 pin/ignore。
