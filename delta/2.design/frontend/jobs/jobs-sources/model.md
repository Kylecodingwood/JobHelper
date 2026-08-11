# Model — Jobs Sources

- 路由：`/jobs/sources`
- 状态：设计正文已完成
- 对齐实体：[`../../../../1.req/job/entity.md`](../../../../1.req/job/entity.md)

## 枚举

| 枚举 | 值 |
|---|---|
| `SourceRunStatus` | `QUEUED`, `RUNNING`, `SUCCEEDED`, `PARTIAL_SUCCESS`, `FAILED`, `CANCELLED` |
| `TriggerType` | `SCHEDULED`, `MANUAL`, `RETRY` |
| `AdapterType` | `FREEHIRE_API`, `JOBSPY`, `MANUAL` |
| `SourceRole` | `PRIMARY`, `SUPPLEMENTAL`, `MANUAL` |
| `DiagnosticCategory` | `403`, `429`, `CAPTCHA`, `TIMEOUT`, `PARSE`, `VALIDATION`, `OTHER` |

## SourceSummaryDto（概览卡片）

| 字段 | 类型 | 说明 |
|---|---|---|
| `sourceId` | UUID | |
| `code` | string | `freehire`, `jobspy_linkedin`, … |
| `adapterType` | AdapterType | |
| `site` | string? | JobSpy 站点名 |
| `role` | SourceRole | FreeHire = PRIMARY |
| `enabled` | boolean | |
| `searchTerms` | string[] | 合并 Profile 默认 + 用户覆盖 |
| `searchTermsFromProfile` | string[] | 只读展示派生项（设计默认） |
| `configVersion` | string | |
| `lastRunStatus` | SourceRunStatus? | |
| `lastRunAt` | ISO8601? | |
| `riskNote` | string? | JobSpy ToS/限速 |

## SourceRunListFilters

| 字段 | 类型 | 说明 |
|---|---|---|
| `sourceId` | UUID? | |
| `status` | SourceRunStatus[]? | |
| `triggerType` | TriggerType[]? | |
| `from` / `to` | ISO8601? | 日期范围 |
| `cursor` | string? | |
| `limit` | number | 设计默认 `30` |

## SourceRunListItemDto（运行列表行）

| 字段 | 类型 |
|---|---|
| `sourceRunId` | UUID |
| `sourceId` | UUID |
| `sourceCode` | string |
| `site` | string? |
| `triggerType` | TriggerType |
| `status` | SourceRunStatus |
| `startedAt` | ISO8601? |
| `endedAt` | ISO8601? |
| `durationMs` | number? |
| `createdCount` | number |
| `updatedCount` | number |
| `failedCount` | number |
| `requestCount` | number |

## SourceRunDetailDto（运行详情）

| 字段 | 类型 |
|---|---|
| `sourceRunId` | UUID |
| `sourceId` | UUID |
| `sourceCode` | string |
| `triggerType` | TriggerType |
| `status` | SourceRunStatus |
| `configVersion` | string |
| `parametersSnapshot` | object | 脱敏 JSON |
| `startedAt` / `endedAt` | ISO8601 |
| `requestCount` | number |
| `receivedCount` | number |
| `validCount` | number |
| `createdCount` | number |
| `updatedCount` | number |
| `skippedCount` | number |
| `failedCount` | number |
| `durationMs` | number |
| `diagnostics` | SourceDiagnosticView[] |

## SourceDiagnosticView

| 字段 | 类型 | 说明 |
|---|---|---|
| `diagnosticId` | UUID? | 设计默认可选 |
| `category` | DiagnosticCategory | |
| `site` | string? | |
| `message` | string | 已脱敏 |
| `occurredAt` | ISO8601 | |
| `requestId` | string? | |
| `retryCount` | number | |
| `itemKey` | string? | 单条记录键 |
| `count` | number? | 分组聚合时 >1 |

## DiagnosticGroupVm（表格分组）

| 字段 | 类型 |
|---|---|
| `site` | string |
| `category` | DiagnosticCategory |
| `totalCount` | number |
| `lastOccurredAt` | ISO8601 |
| `items` | SourceDiagnosticView[] |

## ManualRerunFormVm（UC-JOB-009）

| 字段 | 类型 | 说明 |
|---|---|---|
| `scope` | `ALL_ENABLED` \| `SOURCE` \| `SITE` | |
| `sourceId` | UUID? | scope=SOURCE/SITE 时必填 |
| `site` | string? | scope=SITE |
| `useLastParameters` | boolean | 沿用最近成功/选中 run 快照 |
| `referenceSourceRunId` | UUID? | useLastParameters 时 |

## SourceEnablePatchVm

| 字段 | 类型 |
|---|---|
| `enabled` | boolean |

## SourcesPageVm（页面聚合）

| 字段 | 类型 |
|---|---|
| `sources` | SourceSummaryDto[] |
| `runFilters` | SourceRunListFilters |
| `selectedSourceRunId` | UUID? |
| `runs` | SourceRunListItemDto[] |
| `runDetail` | SourceRunDetailDto? |
| `diagnosticGroups` | DiagnosticGroupVm[] |
| `activeRunPolling` | boolean |

## PlantUML

见 [`model.puml`](model.puml)。
