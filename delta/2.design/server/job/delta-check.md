# job 域核对

- 对齐：[`../../../1.req/job/`](../../../1.req/job/) 全量；[`../../delta.md`](../../delta.md)
- 前端：[`../../frontend/jobs/jobs/`](../../frontend/jobs/jobs/)、[`../../frontend/jobs/jobs-sources/`](../../frontend/jobs/jobs-sources/)

| 检查项 | 状态 |
|--------|------|
| job-model 表/枚举与 entity.md 一致 | ✅ |
| searchTerms 自 Profile TargetRole + 用户覆盖 DISTINCT | ✅ |
| expectedStartDate 缺失 → WORK_AUTH NEEDS_CONFIRMATION | ✅ |
| 硬去重 stableId/URL；fuzzy PossibleDuplicate 不自动合并 | ✅ |
| 合并 survivor=较早 first_seen；status 冲突须用户选择 | ✅ |
| job-crud 服务方法覆盖 UC-JOB-001～009 | ✅ |
| job-api `/api/v1` 与 `/jobs`、`/jobs/sources` 路由对齐 | ✅ |
| job-workflow-sync 每日 06:00、PARTIAL_SUCCESS、诊断 | ✅ |
| job-workflow-gate-rank Gate 表 + Rank ≥3pos0neg=HIGH ≥2neg=LOW | ✅ |
| 用户 jobStatus 不被同步覆盖；Gate 证据追加式 | ✅ |
| Job 领域事件 Outbox → Action 消费 | ✅ |
| component-job.puml 依赖链完整 | ✅ |

## 待实现阶段确认（非设计阻塞）

| 项 | 说明 |
|---|---|
| FreeHire 分页上限 | 实现仓 Adapter 配置 |
| JobSpy 站点限速参数 | 实现仓 constants |
| 前端 api.md 正文 | 由 frontend 阶段回填，路径已与本域对齐 |
