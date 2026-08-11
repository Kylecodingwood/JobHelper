# API — Jobs Inbox

- 路由：`/jobs`
- API 基址：`/api/v1`
- **契约真相**：[`../../../server/job/job-api/delta.md`](../../../server/job/job-api/delta.md)
- 状态：已按后端对齐（2026-08-09）；前端调用策略已优化

## 推荐调用策略（性能）

| 场景 | 调用 | 说明 |
|---|---|---|
| 打开 Inbox | `GET /jobs`（page/size/sort） | 默认 `includeHidden=false` |
| 选中行 | **优先用列表行已有字段**；详情 `GET /jobs/{jobId}` | `JobDetailDto` 已含 `gateDimensions`、`rankFactors`、`pendingDuplicates`、`decisions` 摘要 → **首屏不必再打 evidence/decisions** |
| 展开完整证据时间线 | `GET /jobs/{id}/evidence?evaluationType=` | 按需 |
| 完整决定历史 | `GET /jobs/{id}/decisions` | 按需（详情摘要不够时） |
| Shortlist 等 | `PATCH /jobs/{id}/status` | `expectedVersion` |
| Gate 覆盖 | `POST /jobs/{id}/gate-override` | reason 必填 |
| 保存 URL | `POST /jobs/manual-url` | 201/200 → 打开返回的 `jobId` 详情 |
| 全局待处理重复 | `GET /jobs/duplicates?status=PENDING` | Inbox 筛选「可能重复」可配合列表 `hasPendingDuplicate` |
| 确认/驳回重复 | `POST …/duplicates/{id}/confirm` 或 `…/reject` | 冲突填 survivor + resolvedJobStatus |
| 手动有效性 | `POST /jobs/{id}/validity/check` | 详情操作 |

**禁止**：`POST /jobs/{id}/decisions` 打包写状态（后端无此端点）；`POST /jobs/manual`（应为 `manual-url`）。

## 端点一览

| 方法 | 路径 | UC |
|---|---|---|
| GET | `/api/v1/jobs` | UC-JOB-004 |
| GET | `/api/v1/jobs/{jobId}` | UC-JOB-004～006 |
| POST | `/api/v1/jobs/manual-url` | UC-JOB-002 |
| PATCH | `/api/v1/jobs/{jobId}/status` | UC-JOB-005 |
| POST | `/api/v1/jobs/{jobId}/gate-override` | UC-JOB-005 |
| GET | `/api/v1/jobs/{jobId}/evidence` | UC-JOB-003 |
| GET | `/api/v1/jobs/{jobId}/decisions` | UC-JOB-005 |
| GET | `/api/v1/jobs/duplicates` | UC-JOB-006 |
| POST | `/api/v1/jobs/duplicates/{duplicateId}/confirm` | UC-JOB-006 |
| POST | `/api/v1/jobs/duplicates/{duplicateId}/reject` | UC-JOB-006 |
| POST | `/api/v1/jobs/{jobId}/validity/check` | UC-JOB-007 |
| GET | `/api/v1/jobs/new` | Home 用；本页一般不调 |

## GET `/api/v1/jobs`

**Query**：`status`、`rankTier`、`gateStatus`、`validityStatus`、`includeHidden`、`includeArchived`、`hasPendingDuplicate`、`sourceCode`、`q`、`page`、`size`、`sort`（默认 `lastSeenAt,desc`）

**Response**：`Page<JobSummaryDto>`（含 `version`、`hasPendingDuplicate`、`preferredSourceCode` 等）

## GET `/api/v1/jobs/{jobId}`

**Response**：`JobDetailDto` — `sources[]`、`gateDimensions[]`、`rankFactors[]`、`pendingDuplicates[]`、`decisions[]`（摘要）

## POST `/api/v1/jobs/manual-url`

```json
{ "url": "https://…", "userProvidedJd": null, "note": null }
```

**201/200**：`JobDetailDto`

## PATCH `/api/v1/jobs/{jobId}/status`

```json
{ "toStatus": "SHORTLISTED", "expectedVersion": 3 }
```

**409**：`JOB_VERSION_CONFLICT`

## POST `/api/v1/jobs/{jobId}/gate-override`

```json
{ "reason": "…", "expectedVersion": 3 }
```

原 RuleEvidence 保留。

## GET `/api/v1/jobs/{jobId}/evidence`

**Query**：`evaluationType=GATE|RANK|VALIDITY`  
**Response**：`List<RuleEvidenceDto>`

## 重复处理

- `GET /api/v1/jobs/duplicates?status=PENDING` → `List<PossibleDuplicateDto>`
- `POST …/confirm`：`{ "survivorJobId", "resolvedJobStatus" }` → `MergeResultDto`
- `POST …/reject` → `204`

## 错误码（本页常用）

| HTTP | code |
|---|---|
| 409 | `JOB_VERSION_CONFLICT` / `DUPLICATE_MERGE_CONFLICT` |
| 422 | `GATE_OVERRIDE_REASON_REQUIRED` |
| 404 | `JOB_NOT_FOUND` |
