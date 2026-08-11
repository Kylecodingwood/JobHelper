# action-api

- 状态：**已细化**（M1）
- Base path：`/api/v1`
- 对齐前端：[`../../../2.design/frontend/home/home/page.md`](../../../2.design/frontend/home/home/page.md)（`/`）
- 服务：[`../action-crud/delta.md`](../action-crud/delta.md)
- 图示：[`api-action.puml`](api-action.puml)
- **Home Actions 唯一 API 入口**；New Jobs 读 Job API

## 1. Home 聚合（`/`）

### GET `/api/v1/home`

Home 单次加载（UC-ACT-001）。组合 Action 与 New Jobs **分开展示**（FR-ACT-003）。

**Query**

| 参数 | 默认 | 说明 |
|---|---|---|
| `actionLimit` | 5 | 优先 Action 条数 |
| `newJobLimit` | 10 | New Job 条数 |

**Response `200`：`HomeFeedDto`**

```json
{
  "actions": {
    "content": [ /* ActionSummaryDto[] */ ],
    "priorityRuleVersion": "priority-v1"
  },
  "newJobs": {
    "content": [ /* JobSummaryDto from Job API */ ],
    "source": "/api/v1/jobs/new"
  },
  "generatedAt": "2026-07-31T06:00:00Z"
}
```

实现：BFF 层调用 `ActionQueryService` + `JobCanonicalService.listNewJobsForHome`（或内部 HTTP 调 `/jobs/new`）。

---

## 2. Actions

### GET `/api/v1/actions`

分页列表（Home「查看更多」或管理视图）。

**Query**：`status=OPEN|BLOCKED|...`、`active=true`、`page`、`size`、`sort=prioritySortKey,asc`

**Response**：`Page<ActionSummaryDto>`

**ActionSummaryDto**

| 字段 | 说明 |
|---|---|
| `actionId` | |
| `sourceDomain` | JOB / ROADMAP / … |
| `actionKind` | REVIEW_NEW_JOB 等 |
| `title` | |
| `status` | OPEN / BLOCKED / … |
| `priorityBand` | DEADLINE / BLOCKER / USER_PIN / SYSTEM_SUGGESTION / NORMAL |
| `deadline` | 可空 |
| `pinned` | |
| `targetRef` | type, id, routeHint, focusKey |
| `primaryReason` | 一句解释 |
| `version` | 乐观锁 |

### GET `/api/v1/actions/{actionId}`

**Response `200`**：`ActionDetailDto` — 含 `generationEvidence[]`（最近）、`dependencies[]`、`decisions[]` 摘要。

### GET `/api/v1/actions/{actionId}/priority-evidence`

**Query**：`calculationId`（可选，默认最新）

**Response**：`PriorityExplanationDto`

```json
{
  "calculationId": "uuid",
  "priorityBand": "DEADLINE",
  "factors": [
    { "factor": "DEADLINE", "effectiveOrder": 1, "factValue": "2026-08-01T00:00:00Z", "explanation": "Roadmap 任务截止" },
    { "factor": "USER_PIN", "effectiveOrder": 3, "explanation": "用户置顶" }
  ],
  "ruleVersion": "priority-v1"
}
```

因素按 deadline → blocker → pin → suggestion 排序展示。

### POST `/api/v1/actions/{actionId}/decisions`

用户操作（UC-ACT-004）。

**Request `ActionDecisionRequest`**

```json
{
  "operation": "PIN",
  "reason": null,
  "expectedVersion": 2
}
```

| operation | 效果 |
|---|---|
| `PIN` / `UNPIN` | 更新 pinned；重算优先级 |
| `COMPLETE` | USER_CONFIRMED，active=false |
| `IGNORE` | USER_CONFIRMED |
| `RESTORE` | 恢复 OPEN（规则允许时） |

**Response `200`**：`ActionDetailDto`  
**Errors**：`409` 版本冲突；`422` 非法迁移。

### GET `/api/v1/actions/{actionId}/navigation`

**Response**：`{ "route": "/jobs/{jobId}", "focusKey": "gate:WORK_AUTH" }`

---

## 3. actionKind 与前端路由

| actionKind | routeHint | focusKey 示例 |
|---|---|---|
| `REVIEW_NEW_JOB` | `/jobs/{jobId}` | — |
| `RESOLVE_GATE` | `/jobs/{jobId}` | `gate:{dimension}` |
| `COMPLETE_ROADMAP_TASK` | `/roadmap` | `task:{taskId}` |
| `REVIEW_CV_SUGGESTIONS` | `/cv` | `review:{reviewId}` |
| `COMPLETE_BEHAVIORAL_ANSWER` | `/behavioral` | `answer:{answerId}` |

---

## 4. DTO 与 Job API 对齐

- `JobSummaryDto`：**复用** job-api 定义（`/api/v1/jobs/new`），不在 action 域重复 schema。
- Action `targetRef.targetId` 对应 Job `jobId` 等同 UUID。

---

## 5. 错误码

| HTTP | code | 场景 |
|---|---|---|
| 409 | `ACTION_VERSION_CONFLICT` | |
| 404 | `ACTION_NOT_FOUND` | |
| 410 | `ACTION_STALE` | 目标不可用 |
| 422 | `RESTORE_NOT_ALLOWED` | 无实质源变化 |

---

## 6. FR 映射

| 端点 | FR |
|---|---|
| `/home`、`/actions` | FR-ACT-001、003 |
| `/actions/{id}/navigation` | FR-ACT-002、004 |
| `/actions/{id}/decisions` | FR-ACT-005、006 |
| `/actions/{id}/priority-evidence` | FR-ACT-002、003、007 |
