# job-crud

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/job/usecase-desc.md`](../../../1.req/job/usecase-desc.md)
- 依赖 model：[`../job-model/delta.md`](../job-model/delta.md)
- 图示：[`service-job.puml`](service-job.puml)
- 实现包（后端仓）：`com.jobhelper.job.application` / `com.jobhelper.job.domain`

## 1. 服务分层

| 服务 | 职责 |
|---|---|
| `JobSourceService` | Source 配置、searchTerms 合并、启停 |
| `JobSourceRunService` | 运行生命周期、诊断查询、手动触发 |
| `JobRawPostingService` | Raw 持久化、幂等写入、手动 URL |
| `JobCanonicalService` | Inbox 投影、详情、可见性 |
| `JobNormalizationService` | 标准化、硬去重、fuzzy 检测 |
| `JobGateRankService` | Gate/Rank 评估（见 workflow-gate-rank） |
| `JobDecisionService` | 用户状态、Gate 覆盖、决定历史 |
| `JobDuplicateService` | 可能重复查询与合并 |
| `JobValidityService` | 有效性检查与归档投影 |
| `JobDomainEventPublisher` | 事务后发布 Job 领域事件（Outbox） |

## 2. JobSourceService

| 方法 | 说明 | 用例 |
|---|---|---|
| `listSources()` | 全部 Source 及 enabled、searchTerms | UC-JOB-008 |
| `getSource(sourceId)` | 单源详情含 riskNote | UC-JOB-008 |
| `updateSource(sourceId, cmd)` | 更新 enabled、searchTerms 覆盖；递增 configVersion | UC-JOB-001 |
| `resolveEffectiveSearchTerms(sourceId)` | Profile 活动 TargetRole.roleName ∪ 用户覆盖，DISTINCT | UC-JOB-001 |
| `assertSourceEnabled(sourceId)` | 停用源拒绝运行 | UC-JOB-009 |

**searchTerms 合并**（BR-JOB-012）：
- 从 Profile 读 `activeTargetRoles[].roleName`（只读 API/投影）。
- 与 `job_source.search_terms` 用户覆盖项 UNION DISTINCT。
- 结果写入 SourceRun `parametersSnapshot.searchTerms`。

## 3. JobSourceRunService

| 方法 | 说明 | 用例 |
|---|---|---|
| `createRun(sourceId, triggerType, params?)` | 新建 SourceRun（QUEUED→RUNNING）；**永不复用旧 ID** | UC-JOB-001、009 |
| `markRunning(runId)` | 设置 startedAt | UC-JOB-001 |
| `recordProgress(runId, counters)` | 更新 request/received/valid/created/updated/failed | UC-JOB-001 |
| `appendDiagnostic(runId, diagnostic)` | 追加 SourceDiagnostic | UC-JOB-001、008 |
| `completeRun(runId, status, durationMs)` | SUCCEEDED / PARTIAL_SUCCESS / FAILED | UC-JOB-001 |
| `listRuns(filter)` | 按 source、status、时间分页 | UC-JOB-008 |
| `getRunDetail(runId)` | 含 diagnostics、parametersSnapshot | UC-JOB-008 |
| `triggerManualSync(sourceId?, siteScope?)` | 创建 MANUAL run；可排队 | UC-JOB-009 |

**终态规则**：有 failedCount 且也有成功解析 → `PARTIAL_SUCCESS`；全失败 → `FAILED`；零失败 → `SUCCEEDED`。

## 4. JobRawPostingService

| 方法 | 说明 | 用例 |
|---|---|---|
| `upsertFromAdapter(sourceRunId, adapterRecord)` | 幂等键写入；INVALID 隔离 | UC-JOB-001 |
| `saveManualUrl(cmd)` | 规范 URL、硬去重、可选 userProvidedJd | UC-JOB-002 |
| `linkToExistingJob(rawPostingId, jobId)` | 手动 URL 命中既有 Job | UC-JOB-002 |
| `findByIdempotencyKey(sourceId, stableId, updatedAt)` | 防重复 Raw | UC-JOB-001 |

## 5. JobNormalizationService

| 方法 | 说明 | 用例 |
|---|---|---|
| `normalizeAndMaterialize(rawPostingId)` | 字段标准化 → CanonicalJob 创建/更新 | UC-JOB-003 |
| `hardDedupeByStableId(rawPosting)` | 同 source stable ID 合并 ref | UC-JOB-003 |
| `hardDedupeByCanonicalUrl(applyUrl)` | 跨源 URL 合并（FreeHire↔JobSpy） | UC-JOB-003 |
| `detectPossibleDuplicates(jobId)` | fuzzy 信号；建 PENDING 关系 | UC-JOB-006 |
| `rebuildInboxProjection(jobId)` | hiddenByDefault、lastSeenAt | UC-JOB-004 |

**顺序约束**：持久化 Raw → 标准化 → 硬去重 → Gate → Rank → Inbox。

## 6. JobGateRankService

| 方法 | 说明 | 用例 |
|---|---|---|
| `evaluateGate(jobId, profileSnapshot, ruleVersion)` | 四维度 Gate + RuleEvidence | UC-JOB-003 |
| `evaluateRank(jobId, profileSnapshot, ruleVersion)` | 五因素 Rank；更新 rankTier | UC-JOB-003 |
| `recomputeForProfileChange(profileVersion)` | Profile 变更批量重算 | UC-JOB-003 |
| `getLatestEvidence(jobId, evaluationType)` | 详情页证据 | UC-JOB-004 |

Gate WORK_AUTH：`expectedStartDate == null` → 维度 outcome `NEEDS_CONFIRMATION`（不推导日期）。

## 7. JobCanonicalService

| 方法 | 说明 | 用例 |
|---|---|---|
| `listInbox(query)` | 默认隐藏 Gate FAILED / Expired / Archived | UC-JOB-004 |
| `getJobDetail(jobId)` | 含 sources、gate、rank、duplicates | UC-JOB-004 |
| `listNewJobsForHome(limit)` | `jobStatus=NEW` 且非 hidden；供 Home 独立投影 | FR-ACT-003 |

**默认筛选**：`hiddenByDefault=false`；显式参数可含 `includeHidden`、`gateStatus`、`validityStatus`。

## 8. JobDecisionService

| 方法 | 说明 | 用例 |
|---|---|---|
| `changeStatus(jobId, toStatus, expectedVersion)` | 追加 STATUS_CHANGE；**不覆盖同步** | UC-JOB-005 |
| `overrideGate(jobId, reason, expectedVersion)` | GATE_OVERRIDE；reason 非空 | UC-JOB-005 |
| `listDecisions(jobId)` | 时间倒序 | UC-JOB-004 |
| `applyDecisionProjection(jobId)` | 从最新决定推导 jobStatus / 可见性 | UC-JOB-005 |

每次用户决定后调用 `JobDomainEventPublisher` 发布 `JOB_STATUS_CHANGED` / `JOB_DECISION_RECORDED` / `JOB_GATE_NEEDS_CONFIRMATION` 等。

## 9. JobDuplicateService

| 方法 | 说明 | 用例 |
|---|---|---|
| `listPendingDuplicates(filter?)` | PENDING 关系 | UC-JOB-006 |
| `confirmDuplicate(duplicateId, conflictResolution?)` | survivor=较早 first_seen；合并 refs | UC-JOB-006 |
| `rejectDuplicate(duplicateId)` | DUPLICATE_REJECTION 决定 | UC-JOB-006 |
| `resolveStatusConflict(survivorId, loserId, chosenStatus)` | 冲突时用户选择 | UC-JOB-006 |

## 10. JobValidityService

| 方法 | 说明 | 用例 |
|---|---|---|
| `refreshValidity(jobId)` | 检查 apply/source URL | UC-JOB-007 |
| `refreshAllDue()` | 每日与 sync 后批量 | UC-JOB-007 |
| `markExpired(jobId, evidence)` | validity=EXPIRED；jobStatus→ARCHIVED 投影 | UC-JOB-007 |

403/timeout **不得**单独判 EXPIRED。

## 11. JobDomainEventPublisher

| 方法 | 事件 |
|---|---|
| `publishJobCreated(jobId, version)` | `JOB_CREATED` |
| `publishStatusChanged(jobId, from, to, version)` | `JOB_STATUS_CHANGED` |
| `publishGateNeedsConfirmation(jobId, dimension, version)` | `JOB_GATE_NEEDS_CONFIRMATION` |
| `publishDecisionRecorded(jobId, decisionType, version)` | `JOB_DECISION_RECORDED` |
| `publishArchived(jobId, reason, version)` | `JOB_ARCHIVED` / `JOB_EXPIRED` |

Outbox 与 CanonicalJob/JobDecision 同事务；提交后 infra 投递至 Action consumer。

## 12. 事务与并发

- 用户写操作：`job_canonical.version` 乐观锁；冲突返回 409。
- SourceRun 与 RawPosting：单 run 内 batch 提交；单条失败不 rollback 整 run。
- RuleEvidence / JobDecision：**仅 INSERT**。
