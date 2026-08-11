# job-api

- 状态：**已细化**（M1）
- Base path：`/api/v1`
- 对齐前端：[`../../../2.design/frontend/jobs/jobs/page.md`](../../../2.design/frontend/jobs/jobs/page.md)（`/jobs`）、[`../../../2.design/frontend/jobs/jobs-sources/page.md`](../../../2.design/frontend/jobs/jobs-sources/page.md)（`/jobs/sources`）
- 服务：[`../job-crud/delta.md`](../job-crud/delta.md)
- 图示：[`api-job.puml`](api-job.puml)
- 鉴权：M1 本地单用户，无 token

## 1. Jobs Inbox（`/jobs`）

### GET `/api/v1/jobs`

Inbox 分页列表（UC-JOB-004）。

**Query**

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `status` | JobStatus[] | — | NEW / SHORTLISTED / … |
| `rankTier` | RankTier[] | — | HIGH / MEDIUM / LOW / UNRANKED |
| `gateStatus` | GateStatus[] | — | |
| `validityStatus` | ValidityStatus[] | — | |
| `includeHidden` | boolean | false | Gate FAILED、Expired 等 |
| `includeArchived` | boolean | false | |
| `hasPendingDuplicate` | boolean | — | 可能重复 |
| `sourceCode` | string | — | 如 `freehire` |
| `q` | string | — | 标题/公司模糊 |
| `page` / `size` | int | 0 / 20 | |
| `sort` | string | `lastSeenAt,desc` | |

**Response `200`**：`Page<JobSummaryDto>`

```json
{
  "content": [{
    "jobId": "uuid",
    "title": "Software Engineer",
    "company": "Acme",
    "location": "Dublin",
    "jobStatus": "NEW",
    "validityStatus": "ACTIVE",
    "gateStatus": "PASSED",
    "rankTier": "HIGH",
    "hiddenByDefault": false,
    "expectedStartDate": "2026-09-01",
    "lastSeenAt": "2026-07-30T06:00:00Z",
    "hasPendingDuplicate": false,
    "preferredSourceCode": "freehire",
    "version": 3
  }],
  "totalElements": 120
}
```

### GET `/api/v1/jobs/{jobId}`

详情（UC-JOB-004、005、006）。

**Response `200`**：`JobDetailDto` — 含 `sources[]`、`gateDimensions[]`、`rankFactors[]`、`pendingDuplicates[]`、`decisions[]`（摘要）。

### POST `/api/v1/jobs/manual-url`

快速保存 URL（UC-JOB-002）。

**Request `ManualUrlRequest`**

| 字段 | 类型 | 必填 |
|---|---|---|
| `url` | string | ✓ |
| `userProvidedJd` | string | |
| `note` | string | |

**Response `201`**：`JobDetailDto`（新建或命中既有）；`200` 若已存在。

**Errors**：`400` 非法 URL；`422` 无法规范化。

### PATCH `/api/v1/jobs/{jobId}/status`

用户状态变更（UC-JOB-005）。

**Request `JobStatusChangeRequest`**

```json
{ "toStatus": "SHORTLISTED", "expectedVersion": 3 }
```

**Response `200`**：`JobDetailDto`  
**Errors**：`409` 版本冲突；后台同步不得覆盖用户状态（FR-JOB-013）。

### POST `/api/v1/jobs/{jobId}/gate-override`

Gate 覆盖（UC-JOB-005）。

**Request**

```json
{ "reason": "明确支持 Stamp 4", "expectedVersion": 3 }
```

**Response `200`**：`JobDetailDto`；原 RuleEvidence 保留。

### GET `/api/v1/jobs/{jobId}/evidence`

Gate/Rank/Validity 证据时间线。

**Query**：`evaluationType=GATE|RANK|VALIDITY`

**Response**：`List<RuleEvidenceDto>`

### GET `/api/v1/jobs/{jobId}/decisions`

**Response**：`List<JobDecisionDto>`（完整历史）

### GET `/api/v1/jobs/duplicates`

**Query**：`status=PENDING`（默认）

**Response**：`List<PossibleDuplicateDto>`

### POST `/api/v1/jobs/duplicates/{duplicateId}/confirm`

确认合并（UC-JOB-006）。

**Request**（冲突时必填）

```json
{
  "survivorJobId": "uuid",
  "resolvedJobStatus": "SHORTLISTED"
}
```

**Response `200`**：`MergeResultDto`（survivorJobId、archivedJobId）

### POST `/api/v1/jobs/duplicates/{duplicateId}/reject`

**Response `204`**

### POST `/api/v1/jobs/{jobId}/validity/check`

手动有效性刷新（UC-JOB-007）。

**Response `200`**：`ValidityResultDto`

---

## 2. Home 新职位投影（Action 域消费 Job 读模型）

供 Home `/` 独立展示 New Job（FR-ACT-003）；**不**经 Action 聚合。

### GET `/api/v1/jobs/new`

**Query**：`limit=10`（默认）

**Response `200`**：`List<JobSummaryDto>` — `jobStatus=NEW`、非 hidden、按 `lastSeenAt desc`。

---

## 3. Sources & Sync（`/jobs/sources`）

### GET `/api/v1/job-sources`

**Response**：`List<JobSourceDto>`（code、enabled、searchTerms、effectiveSearchTerms、riskNote）

### GET `/api/v1/job-sources/{sourceId}`

### PATCH `/api/v1/job-sources/{sourceId}`

**Request**

```json
{
  "enabled": true,
  "searchTermsOverride": ["graduate software engineer", "backend developer"]
}
```

合并 Profile TargetRole 后返回 `effectiveSearchTerms`。

### GET `/api/v1/job-source-runs`

**Query**：`sourceId`、`status`、`from`、`to`、`page`、`size`

**Response**：`Page<JobSourceRunSummaryDto>`

### GET `/api/v1/job-source-runs/{runId}`

含 `diagnostics[]`、`parametersSnapshot`（脱敏）、计数器。

### POST `/api/v1/job-source-runs`

手动同步（UC-JOB-009）。

**Request**

```json
{
  "sourceId": "uuid",
  "siteScope": ["linkedin"],
  "reuseLastParameters": true
}
```

**Response `202`**：`JobSourceRunSummaryDto`（QUEUED/RUNNING）

---

## 4. 核心 DTO 摘要

| DTO | 用途 |
|---|---|
| `JobSummaryDto` | 列表、Home new jobs |
| `JobDetailDto` | 详情侧栏 |
| `JobSourceDto` / `JobSourceRunSummaryDto` / `SourceDiagnosticDto` | Sources 页 |
| `RuleEvidenceDto` | Gate/Rank 分项；含 dimension、outcome、explanation |
| `JobDecisionDto` | 决定历史 |
| `PossibleDuplicateDto` | left/right Job 摘要 + signals |
| `GateDimensionDto` | LOCATION/WORK_AUTH/SENIORITY/LANGUAGE + outcome |
| `RankFactorDto` | 五因素 POS/NEU/NEG |

**GateDimensionDto 示例**（WORK_AUTH 缺失日期）：

```json
{
  "dimension": "WORK_AUTH",
  "outcome": "NEEDS_CONFIRMATION",
  "explanation": "岗位未提供 expectedStartDate，无法对照 Profile 工作授权时间线"
}
```

## 5. 错误码（实现仓 specification 展开）

| HTTP | code | 场景 |
|---|---|---|
| 409 | `JOB_VERSION_CONFLICT` | 乐观锁 |
| 409 | `DUPLICATE_MERGE_CONFLICT` | 合并时 jobStatus 冲突 |
| 422 | `GATE_OVERRIDE_REASON_REQUIRED` | 覆盖无 reason |
| 404 | `JOB_NOT_FOUND` | |
| 409 | `SOURCE_RUN_ALREADY_ACTIVE` | 并发手动 run |

## 6. FR 映射

| 端点组 | FR |
|---|---|
| manual-url、jobs CRUD | FR-JOB-003～013、018～021 |
| job-sources、source-runs | FR-JOB-001、002、004、016、017、022、023 |
| evidence、gate-override | FR-JOB-008～011、020 |
| duplicates | FR-JOB-007、008 |
| validity | FR-JOB-014～016 |
| `/jobs/new` | FR-ACT-003（Home 新职位） |
