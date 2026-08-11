# action 域核对

- 对齐：[`../../../1.req/action/`](../../../1.req/action/)、[`../../../1.req/cross-domain-events.md`](../../../1.req/cross-domain-events.md)
- 前端：[`../../frontend/home/home/`](../../frontend/home/home/)

| 检查项 | 状态 |
|--------|------|
| action-model 与 entity.md 一致 | ✅ |
| actionKind 五类 + 禁止 AUTO_APPLY | ✅ |
| 业务唯一键 active=true | ✅ |
| priority：deadline → blocker → pin → suggestion | ✅ |
| ActionPriorityEvidence 可解释、无 opaque 总分 | ✅ |
| action-crud 覆盖 UC-ACT-001～005 | ✅ |
| action-api `/home`、`/actions` 与 Home `/` 对齐 | ✅ |
| New Jobs 分读 Job `/api/v1/jobs/new`（Action 不拥有） | ✅ |
| event-consume 幂等 Receipt + IGNORED_STALE | ✅ |
| Job 事件路由 JOB_* → REVIEW_NEW_JOB / RESOLVE_GATE | ✅ |
| Roadmap 事件路由 ROADMAP_* → COMPLETE_ROADMAP_TASK | ✅ |
| 消费失败不撤销 pin/ignore/complete | ✅ |
| Action 唯一拥有 Home Actions | ✅ |
| component-action.puml 依赖链完整 | ✅ |

## 待实现阶段确认

| 项 | 说明 |
|---|---|
| CV/Behavioral 事件 handler | M2/M3 启用，路由已文档化 |
| 前端 home/api.md 正文 | frontend 阶段回填 |
