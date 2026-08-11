# Model — Profile

- 路由：`/profile`
- 状态：设计正文已完成
- 域实体权威：[`../../../../1.req/profile/entity.md`](../../../../1.req/profile/entity.md)

## 视图模型

### `ProfilePageViewModel`

| 字段 | 类型 | 说明 |
|---|---|---|
| `mode` | `onboarding \| edit` | |
| `profile` | `ProfileFormVM?` | |
| `profileVersion` | integer | |
| `recomputeRequests` | `RecomputeRequestVM[]` | PENDING/DEFERRED |
| `backupStatus` | `BackupStatusVM` | |
| `isDirty` | boolean | 未保存变更 |

### `ProfileFormVM`

| 字段 | 类型 |
|---|---|
| `educationPeriods` | `EducationPeriodVM[]` |
| `workAuthorizations` | `WorkAuthorizationVM[]` |
| `targetRoles` | `TargetRoleVM[]` |
| `languageProficiencies` | `LanguageProficiencyVM[]` |
| `skillEvidences` | `SkillEvidenceVM[]` |
| `experienceEvidences` | `ExperienceEvidenceVM[]` |

### `EducationPeriodVM`

| 字段 | 类型 | 约束 |
|---|---|---|
| `educationPeriodId` | UUID? | 新建空 |
| `institutionName` | string | 必填 |
| `programmeName` | string | 必填 |
| `startDate` | date | |
| `expectedGraduationDate` | date | ≥ start |
| `countryCode` | string | ISO |
| `isPrimary` | boolean | 最多一个 true |

### `WorkAuthorizationVM`

| 字段 | 类型 |
|---|---|
| `workAuthorizationId` | UUID? |
| `countryCode` | string |
| `permissionType` | enum |
| `validFrom` / `validUntil` | date |
| `weeklyHoursLimit` | number ≥ 0 |
| `isFuture` | boolean |

### `TargetRoleVM`

| 字段 | 类型 |
|---|---|
| `targetRoleId` | UUID? |
| `roleName` | string |
| `seniority` | enum |
| `location` | string |
| `priorityOrder` | integer |
| `active` | boolean |

### `LanguageProficiencyVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `languageProficiencyId` | UUID? | **实体主键** |
| `languageCode` | string | ISO 639-1；Profile 内唯一 |
| `proficiency` | `NATIVE \| C2 \| … \| UNKNOWN` | |
| `evidenceRef` | string? | |
| `notes` | string? | |

### `SkillEvidenceVM` / `ExperienceEvidenceVM`

对齐 entity.md 枚举与字段。

### `ProfileImpactPreviewVM`

| 字段 | 类型 |
|---|---|
| `changedFields` | `{ path, before, after }[]` |
| `affectedScopes` | `ROADMAP \| JOB_GATE \| JOB_RANK[]` |
| `preservedDecisionsSummary` | string |

### `RecomputeRequestVM`

| 字段 | 类型 |
|---|---|
| `recomputeRequestId` | UUID |
| `profileVersion` | integer |
| `scopes` | RecomputeScope[] |
| `status` | `PENDING \| DEFERRED \| …` |
| `changeSummary` | string |

### `BackupStatusVM`

| 字段 | 类型 | 说明 |
|---|---|---|
| `scheduleCron` | string | 设计默认 `0 3 * * SUN` |
| `retentionCount` | 4 | |
| `backupDir` | `./data/backups` | |
| `lastSuccessAt` | ISO8601? | |
| `lastFailureAt` | ISO8601? | |
| `recentBackups` | `{ backupId, createdAt, sizeBytes, checksum }[]` |

### `RestorePreviewVM`

| 字段 | 类型 |
|---|---|
| `packageVersion` | string |
| `domainsIncluded` | string[] |
| `willOverwrite` | boolean |
| `warnings` | string[] |

## 校验规则（前端）

- 至少 1 主教育、1 active TargetRole、1 当前或未来 WorkAuthorization（onboarding 完成时）
- 日期区间、weeklyHours ≥ 0
- languageCode 大小写无关唯一

## PlantUML

见 [`model.puml`](model.puml)。
