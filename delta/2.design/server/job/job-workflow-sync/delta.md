# job-workflow-sync

- 状态：**已细化**（M1）
- 调度：**每日 06:00** 本地时区（见 [`../../delta.md`](../../delta.md)）
- 对齐：[`../../../1.req/job/usecase-desc.md`](../../../1.req/job/usecase-desc.md) UC-JOB-001、008、009；[`../../../1.req/job/datasource-validation.md`](../../../1.req/job/datasource-validation.md)
- 服务：[`../job-crud/delta.md`](../job-crud/delta.md)
- 图示：[`workflow-job-sync.puml`](workflow-job-sync.puml)

## 1. 触发与范围

| 触发 | triggerType | 范围 |
|---|---|---|
| Cron `0 0 6 * * *` | `SCHEDULED` | 全部 `enabled=true` 的 Source |
| `POST /api/v1/job-source-runs` | `MANUAL` | 用户指定 sourceId / siteScope |
| 失败重试（可选） | `RETRY` | 新 run，参数来自原 snapshot |

**主源**：FreeHire（`adapterType=FREEHIRE_API`，`countries=ie`）。  
**补充源**：JobSpy 全受支持站点默认 enabled（LinkedIn、Indeed、ZipRecruiter、Glassdoor、Google、Bayt、Naukri、Bdjobs）；单站点可 PATCH 停用。

## 2. 执行流程

1. **准备**：对每个 Source 调用 `resolveEffectiveSearchTerms` — Profile 活动 `TargetRole.roleName` ∪ 用户覆盖，DISTINCT。
2. **创建 SourceRun**：status=QUEUED→RUNNING；`parametersSnapshot` 写入 searchTerms、location=Ireland、configVersion（脱敏，无密钥）。
3. **Adapter 调用**：
   - **FreeHireAdapter**：`GET https://freehire.me/api/v1/jobs/search`，`countries=ie`，分页至上限；stableId=`public_slug`。
   - **JobSpyAdapter**：Python 子进程 `scripts/datasource/jobspy_fetch.py`，按 site 隔离；location=Ireland。
   - **ManualAdapter**：不参与定时；仅 UC-JOB-002。
4. **逐条 upsert RawPosting**：幂等键 `(sourceId, sourceStableId, sourceUpdatedAt)` 或 canonical URL；parse 失败 → INVALID + diagnostic，**不**建 CanonicalJob。
5. **管道**：每条 VALID Raw → `normalizeAndMaterialize` → Gate → Rank（见 job-workflow-gate-rank）。
6. **计数与完成**：更新 request/received/valid/created/updated/failed；写 durationMs、endedAt。
7. **有效性**：sync 成功后对 touched jobs 调度 `refreshAllDue`（UC-JOB-007）。
8. **事件**：新 CanonicalJob → `JOB_CREATED`；Gate 待确认 → `JOB_GATE_NEEDS_CONFIRMATION`。

## 3. 终态与 PARTIAL_SUCCESS

| 条件 | RunStatus |
|---|---|
| 全部来源/站点成功，failedCount=0 | `SUCCEEDED` |
| 至少一条成功解析，且存在站点/条目失败 | `PARTIAL_SUCCESS` |
| 无任何有效 Raw，或主源整体不可达 | `FAILED` |
| 用户取消 | `CANCELLED` |

**PARTIAL_SUCCESS 规则**（BR-JOB-004、FR-JOB-022）：
- LinkedIn 403 且 FreeHire 成功 → `PARTIAL_SUCCESS` + HTTP_403 diagnostic。
- 失败站点**不**生成占位 Job；成功站点事务**不回滚**。
- `SUCCEEDED` 时 failedCount 必须为 0 或每条 failure 已写入 diagnostic 并计入 failedCount（与 UC 一致：有 unexplained failed 不可 SUCCEEDED）。

## 4. 诊断（SourceDiagnostic）

每条 Adapter 错误写入：

| category | 典型场景 |
|---|---|
| `HTTP_403` | ZipRecruiter、Glassdoor |
| `HTTP_429` | 限速 |
| `CAPTCHA` | Bayt、Naukri |
| `TIMEOUT` | 网络 |
| `PARSE` | 响应格式 |
| `VALIDATION` | 缺 title/url 等必要字段 |

字段：脱敏 message、site、requestId、retryCount、occurredAt、可选 itemKey。  
UI 路径：`/jobs/sources` → `GET /api/v1/job-source-runs/{runId}`。

## 5. 幂等与重跑

- 每次重跑**新** `sourceRunId`（FR-JOB-017）。
- 相同 stable ID 重命中 → 更新 RawPosting + 关联 CanonicalJob，**不** duplicate Job。
- 已有 `SHORTLISTED`/`APPLIED` 等用户状态**不变**（FR-JOB-013）。

## 6. Adapter 配置摘要

| Source code | adapter | 关键参数 |
|---|---|---|
| `freehire` | FREEHIRE_API | q=searchTerm[], countries=ie, pagination |
| `jobspy_*` | JOBSPY | site, location=Ireland, results_wanted, search_terms |
| `manual_url` | MANUAL | — |

JobSpy 风险：`riskNote` 记录 ToS/封禁；禁止自动登录、申请、captcha 绕过（FR-JOB-004）。

## 7. 失败隔离

- 单条 VALIDATION 失败：item-level diagnostic，failedCount++。
- 单站点失败：该站点 diagnostic，其他站点继续。
- 整体 FAILED：已有 Jobs 与历史 Run **仍可读**（E2）。

## 8. 关联 FR

FR-JOB-001、002、004、004a、005、016、017、019、022、023；Profile searchTerms FR-PRO（TargetRole）。
