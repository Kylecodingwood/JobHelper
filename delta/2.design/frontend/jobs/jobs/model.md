# Model — Jobs Inbox

- 路由：`/jobs`
- 状态：设计正文已完成
- 对齐实体：[`../../../../1.req/job/entity.md`](../../../../1.req/job/entity.md)

## 枚举（与需求一致，UI 展示用 label 映射）

| 枚举 | 值 |
|---|---|
| `JobStatus` | `NEW`, `SHORTLISTED`, `IGNORED`, `APPLIED`, `ARCHIVED` |
| `GateStatus` | `PASSED`, `FAILED`, `NEEDS_CONFIRMATION`, `UNKNOWN` |
| `RankTier` | `HIGH`, `MEDIUM`, `LOW`, `UNRANKED` |
| `ValidityStatus` | `ACTIVE`, `EXPIRED`, `NEEDS_CONFIRMATION`, `UNKNOWN` |
| `RankOutcome` | `POSITIVE`, `NEUTRAL`, `NEGATIVE` |
| `GateOutcome` | `PASS`, `FAIL`, `NEEDS_CONFIRMATION`, `UNKNOWN` |
| `DecisionType` | `STATUS_CHANGE`, `GATE_OVERRIDE`, `DUPLICATE_CONFIRMATION`, `DUPLICATE_REJECTION` |
| `DuplicateRelationStatus` | `PENDING`, `CONFIRMED`, `REJECTED` |

## JobsInboxFilters（筛选 / URL 同步）

| 字段 | 类型 | 说明 |
|---|---|---|
| `jobStatus` | `JobStatus[]?` | 空=不限；默认投影不含 `ARCHIVED` |
| `rankTier` | `RankTier[]?` | |
| `gateStatus` | `GateStatus[]?` | 默认不含 `FAILED`（除非 `showHidden`） |
| `sourceCode` | `string[]?` | 如 `freehire`, `jobspy_linkedin`, `manual_url` |
| `validityStatus` | `ValidityStatus[]?` | 默认不含 `EXPIRED` |
| `hasPossibleDuplicate` | `boolean?` | true 仅可能重复 |
| `showHidden` | `boolean` | 显式含 `hiddenByDefault`（Gate 失败未覆盖、Expired、用户归档等） |
| `q` | `string?` | 标题/公司关键词（设计默认） |
| `sort` | `enum` | `NEW_FIRST`（默认）, `UPDATED_DESC`, `TIER_DESC` |
| `cursor` | `string?` | 分页游标 |
| `limit` | `number` | 设计默认 `50` |

## JobListItemDto（列表行 / Job 卡片）

| 字段 | 类型 | 说明 |
|---|---|---|
| `jobId` | UUID | |
| `title` | string? | |
| `company` | string? | |
| `location` | string? | |
| `jobStatus` | JobStatus | 用户投影 |
| `gateStatus` | GateStatus | |
| `rankTier` | RankTier | Gate 未通过通常为 `UNRANKED` |
| `validityStatus` | ValidityStatus | |
| `hiddenByDefault` | boolean | UI 灰显/筛选依据 |
| `hasPossibleDuplicate` | boolean | 显示重复徽标 |
| `gateSummary` | string? | 单行关键依据，如「地点：FAIL」 |
| `lastSeenAt` | ISO8601 | 列表时间列 |
| `version` | integer | 乐观锁 |

## JobDetailDto（详情侧栏）

| 字段 | 类型 | 说明 |
|---|---|---|
| `jobId` | UUID | |
| `title, company, location` | string? | |
| `description` | string? | 选中来源摘要 |
| `canonicalApplyUrl` | string? | 外链申请 |
| `expectedStartDate` | date? | 缺失时 Gate 工作授权为 NEEDS_CONFIRMATION |
| `seniority` | string | |
| `mandatoryLanguages` | string[] | |
| `jobStatus` | JobStatus | |
| `gateStatus` | GateStatus | |
| `rankTier` | RankTier | |
| `validityStatus` | ValidityStatus | |
| `hiddenByDefault` | boolean | |
| `ruleVersion` | string | |
| `lastEvaluatedAt` | ISO8601 | |
| `version` | integer | |
| `sourceRefs` | RawPostingSummary[] | 见下 |
| `gateOverrideActive` | boolean | 是否有有效 Gate 覆盖 |

### RawPostingSummary

| 字段 | 类型 |
|---|---|
| `rawPostingId` | UUID |
| `sourceCode` | string |
| `sourceUrl` | string? |
| `fetchedAt` | ISO8601 |
| `hasUserProvidedJd` | boolean |

## GateDimensionView（Gate 证据卡片）

| 字段 | 类型 |
|---|---|
| `dimension` | `LOCATION` \| `WORK_AUTH` \| `SENIORITY` \| `LANGUAGE` |
| `outcome` | GateOutcome |
| `explanation` | string |
| `ruleId` | string |
| `ruleVersion` | string |
| `factValue` | string? |
| `evaluatedAt` | ISO8601 |

## RankFactorView（Rank 因素卡片）

| 字段 | 类型 |
|---|---|
| `dimension` | `TARGET_ROLE` \| `SKILL` \| `FRESHNESS` \| `LOCATION_FIT` \| `GROWTH` |
| `outcome` | RankOutcome |
| `explanation` | string |
| `evaluatedAt` | ISO8601 |

## JobDecisionView（决定历史）

| 字段 | 类型 |
|---|---|
| `decisionId` | UUID |
| `decisionType` | DecisionType |
| `fromValue` | string? |
| `toValue` | string? |
| `reason` | string? |
| `actor` | `USER` |
| `createdAt` | ISO8601 |

## 表单 ViewModel

### SaveUrlFormVm（UC-JOB-002）

| 字段 | 类型 | 校验 |
|---|---|---|
| `url` | string | 必填；允许 http/https |
| `userProvidedJd` | string? | 可选 |
| `note` | string? | 可选 |

### StatusChangeFormVm

| 字段 | 类型 | 校验 |
|---|---|---|
| `toStatus` | JobStatus | 必填 |
| `basedOnJobVersion` | integer | 必填 |

### GateOverrideFormVm

| 字段 | 类型 | 校验 |
|---|---|---|
| `reason` | string | 必填、trim 非空 |
| `basedOnJobVersion` | integer | 必填 |

### DuplicateResolveFormVm（UC-JOB-006）

| 字段 | 类型 | 校验 |
|---|---|---|
| `resolution` | `CONFIRM` \| `REJECT` \| `DEFER` | 必填 |
| `survivorJobId` | UUID? | CONFIRM 时默认较早 Job，可显式指定 |
| `preferredJobStatus` | JobStatus? | 两 Job status 冲突时必填 |
| `reason` | string? | 设计默认可选 |
| `basedOnDuplicateVersion` | integer? | 并发校验（设计默认） |

## PossibleDuplicatePanelVm（重复面板）

| 字段 | 类型 |
|---|---|
| `duplicateId` | UUID |
| `status` | DuplicateRelationStatus |
| `signals` | string[] |
| `leftJob` | DuplicateJobSummary |
| `rightJob` | DuplicateJobSummary |
| `defaultSurvivorJobId` | UUID |

### DuplicateJobSummary

| 字段 | 类型 |
|---|---|
| `jobId` | UUID |
| `title, company, location` | string? |
| `jobStatus` | JobStatus |
| `firstSeenAt` | ISO8601 |
| `sourceCodes` | string[] |

## JobsListPageVm（页面聚合）

| 字段 | 类型 |
|---|---|
| `filters` | JobsInboxFilters |
| `selectedJobId` | UUID? |
| `items` | JobListItemDto[] |
| `nextCursor` | string? |
| `totalApprox` | number? |
| `listState` | `idle` \| `loading` \| `error` |
| `detail` | JobDetailDto? |
| `gateDimensions` | GateDimensionView[] |
| `rankFactors` | RankFactorView[] |
| `decisions` | JobDecisionView[] |
| `activeDuplicate` | PossibleDuplicatePanelVm? |

## PlantUML

见 [`model.puml`](model.puml)。
