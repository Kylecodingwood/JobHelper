# Job 实体说明

- 状态：已细化
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 图示：[entity.puml](entity.puml)

## 1. 聚合边界

- **Source 运行聚合**：`Source` 为配置根，拥有多次 `SourceRun`；每次运行产生 `RawPosting` 和诊断。
- **Job 决策聚合**：`CanonicalJob` 为根，关联一个或多个 RawPosting，拥有规则证据、决定历史和可能重复关系。
- 原始事实、系统规则结论和用户决定必须分离：RawPosting 不可被标准化结果覆盖；RuleEvidence 不可被用户覆盖操作改写；JobDecision 不可被后台同步覆盖。

## 2. Source

**含义**：一个可独立启停、运行和诊断的职位输入通道或 JobSpy 站点。

| 属性 | 类型/示例 | 约束 |
|---|---|---|
| `sourceId` | UUID | 主键，不变 |
| `code` | `freehire`、`jobspy_linkedin`、`manual_url` | 唯一、稳定 |
| `adapterType` | `FREEHIRE_API/JOBSPY/MANUAL` | 必填 |
| `site` | JobSpy site name | 非 JobSpy 可空 |
| `role` | `PRIMARY/SUPPLEMENTAL/MANUAL` | FreeHire 为 PRIMARY |
| `enabled` | boolean | JobSpy 所有受支持站点初始为 true |
| `schedule` | daily/manual-only | 自动源默认为 daily |
| `searchTerms` | string[] | 默认自 Profile 活动 `TargetRole.roleName` 派生；用户可增删覆盖 |
| `riskNote` | ToS/限速说明 | JobSpy 站点必填 |
| `configVersion` | string | 每次变更递增/更新 |
| `createdAt/updatedAt` | timestamp | 必填 |

**生命周期**：创建 → 启用/停用 → 配置更新；停用不删除历史 SourceRun 或职位。

**不变量**：
1. `code` 唯一，JobSpy 每个站点必须能独立启停。
2. FreeHire 是主源；所有 JobSpy 受支持站点默认启用（保持 all-sites 默认）。
3. Source 配置不得包含提交申请、自动登录或绕过 captcha 的能力。
4. `searchTerms` 默认来自 Profile 活动 `TargetRole.roleName`；用户覆盖项与默认值合并去重后用于同步。

## 3. SourceRun

**含义**：一次确定范围和配置快照的来源执行。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `sourceRunId` | UUID | 主键 |
| `sourceId` | FK | 必填 |
| `triggerType` | `SCHEDULED/MANUAL/RETRY` | 必填 |
| `status` | `QUEUED/RUNNING/SUCCEEDED/PARTIAL_SUCCESS/FAILED/CANCELLED` | 必填 |
| `parametersSnapshot` | JSON | 脱敏、不可覆盖 |
| `configVersion` | string | 必填 |
| `startedAt/endedAt` | timestamp | RUNNING 有 startedAt；终态有 endedAt |
| `requestCount/receivedCount/validCount` | integer | 非负 |
| `createdCount/updatedCount/skippedCount/failedCount` | integer | 非负 |
| `durationMs` | integer | 终态非负 |
| `diagnostics` | `SourceDiagnostic[]` | 可为空 |

`SourceDiagnostic` 至少含 `category`（403/429/CAPTCHA/TIMEOUT/PARSE/VALIDATION/OTHER）、`site`、脱敏消息、发生时间、请求 ID、重试次数和可选 item key。

**不变量**：
1. 每次重跑创建新 ID，不改写旧运行。
2. 单站点/单记录错误必须可诊断，且不能生成损坏 CanonicalJob。
3. `SUCCEEDED` 不得含未解释 failedCount；有成功和失败时为 `PARTIAL_SUCCESS`。

## 4. RawPosting

**含义**：来源返回或用户输入的不可覆盖原始职位快照。

| 属性 | 类型 | 约束 |
|---|---|---|
| `rawPostingId` | UUID | 主键 |
| `sourceId/sourceRunId` | FK | 手动 URL 可使用手动 SourceRun |
| `sourceStableId` | string? | 来源提供时保存，如 FreeHire `public_slug` |
| `sourceUrl/applyUrl` | string? | 保存原值 |
| `canonicalUrl` | string? | 规范化派生值 |
| `rawPayload` | JSON/text | 原样保留，敏感日志不得复制 |
| `userProvidedJd` | text? | 与抓取字段分离 |
| `fetchedAt/sourceUpdatedAt` | timestamp | fetchedAt 必填 |
| `parseStatus` | `PENDING/VALID/INVALID` | INVALID 不进入普通 Jobs |
| `parseErrors` | list | INVALID 时非空 |

**幂等键**：优先 `(sourceId, sourceStableId, sourceUpdatedAt/version)`；无 stable ID 时使用来源内规范 URL 与内容快照标识。重复抓取可追加快照或更新运行关联，但不得重复创建同一业务职位。

**不变量**：原始 payload/JD 不被 CanonicalJob 反写；INVALID 记录不生成半成品 Job；删除/失效来源不级联删除 RawPosting。

## 5. CanonicalJob

**含义**：跨来源标准化后的业务职位。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `jobId` | UUID | 主键 |
| `title/company/location` | string? | 允许未知，不得伪造 |
| `description` | text? | 保留选择来源与时间 |
| `canonicalApplyUrl` | string? | 用于硬去重 |
| `sourceRefs` | RawPosting 引用集合 | 至少一个 |
| `postedAt/firstSeenAt/lastSeenAt` | timestamp? | firstSeenAt 必填 |
| `expectedStartDate` | date? | Gate 工作授权基准；缺失时工作授权维度为 `NEEDS_CONFIRMATION`，不得自动推导毕业日或 +90 天 |
| `seniority` | 标准枚举/UNKNOWN | 必填 |
| `mandatoryLanguages` | set | 未说明为空并保留“未说明”语义 |
| `gateStatus` | `PASSED/FAILED/NEEDS_CONFIRMATION/UNKNOWN` | 派生投影 |
| `rankTier` | `HIGH/MEDIUM/LOW/UNRANKED` | Gate 未通过通常为 UNRANKED |
| `jobStatus` | `NEW/SHORTLISTED/IGNORED/APPLIED/ARCHIVED` | 用户状态投影 |
| `validityStatus` | `ACTIVE/EXPIRED/NEEDS_CONFIRMATION/UNKNOWN` | 与 jobStatus 分离 |
| `hiddenByDefault` | boolean | 按 Gate/有效性派生 |
| `ruleVersion/lastEvaluatedAt` | string/timestamp | 必填于已评估 Job |
| `version` | integer | 乐观并发控制 |

**不变量**：
1. 只有 stable ID/规范 URL 硬命中才可自动合并；模糊相似不能改变归属。
2. Gate 必须在标准化和去重后执行。
3. Gate 失败、未知和 Expired 都保留；默认隐藏不等于删除。
4. `rankTier` 只有三档和 UNRANKED；五个 Rank 因素各持独立 `RuleEvidence`（`POSITIVE/NEUTRAL/NEGATIVE`），聚合规则：≥3 `POSITIVE` 且 0 `NEGATIVE` → `HIGH`；≥2 `NEGATIVE` → `LOW`；否则 `MEDIUM`。
5. 后台同步不得把 `SHORTLISTED/IGNORED/APPLIED` 重置为 `NEW`。

## 6. RuleEvidence

**含义**：系统对 Gate 或 Rank 某一维度的版本化事实—规则—结论记录。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `evidenceId` | UUID | 主键 |
| `jobId` | FK | 必填 |
| `evaluationType` | `GATE/RANK/VALIDITY/DEDUPE` | 必填 |
| `dimension` | `LOCATION/WORK_AUTH/SENIORITY/LANGUAGE/TARGET_ROLE/SKILL/FRESHNESS/GROWTH/...` | 必填 |
| `outcome` | `PASS/FAIL/NEEDS_CONFIRMATION/UNKNOWN/POSITIVE/NEUTRAL/NEGATIVE` | 必填 |
| `ruleId/ruleVersion` | string | 必填 |
| `factValue/factSourceRef` | value + RawPosting/Profile ref | 必须可追溯 |
| `explanation` | string | 用户可读 |
| `evaluatedAt` | timestamp | 必填 |

**不变量**：证据是追加式评估记录；Profile/规则变化创建新评估，不静默改写旧证据；Rank 的五个优先因素各须独立证据；Gate 语言维度对照 Profile `LanguageProficiency`。

## 7. JobDecision

**含义**：用户对职位状态、Gate 覆盖或重复关系作出的不可覆盖决定。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `decisionId` | UUID | 主键 |
| `jobId` | FK | 必填 |
| `decisionType` | `STATUS_CHANGE/GATE_OVERRIDE/DUPLICATE_CONFIRMATION/DUPLICATE_REJECTION` | 必填 |
| `fromValue/toValue` | string? | 状态变更时必填 |
| `reason` | string? | Gate 覆盖时非空 |
| `actor` | `USER` | M1 决策主体 |
| `createdAt` | timestamp | 必填 |
| `basedOnJobVersion` | integer | 并发校验 |
| `supersedesDecisionId` | UUID? | 纠正/恢复时引用，不删除旧决定 |

**不变量**：系统建议不能创建用户状态决定；Shortlist/Applied 仅由用户操作产生；后台不得修改历史；覆盖保留原 Gate 结论。

## 8. PossibleDuplicate

**含义**：两个 CanonicalJob 之间仅基于相似性的待确认关系。

| 属性 | 类型/枚举 | 约束 |
|---|---|---|
| `duplicateId` | UUID | 主键 |
| `leftJobId/rightJobId` | FK | 无序对唯一、不得相同 |
| `signals` | 标题/公司/地点等 | 至少一个 |
| `status` | `PENDING/CONFIRMED/REJECTED` | PENDING 不自动合并 |

**确认合并语义**（`CONFIRMED` 时）：
1. 保留较早创建的 `CanonicalJob` 为 survivor。
2. 合并双方全部 `RawPosting` 引用与来源历史至 survivor。
3. 若 `jobStatus` 或用户 `JobDecision` 冲突，系统须询问用户选择，不得自动覆盖用户决定。
4. 非 survivor 的 CanonicalJob 归档，不删除 RawPosting。
| `decidedByDecisionId` | UUID? | 终态必填 |

## 9. 关系与删除策略

- Source `1—*` SourceRun；SourceRun `1—*` RawPosting。
- CanonicalJob `*—*` RawPosting（通过来源引用；一个原始记录至多归属一个活动 CanonicalJob）。
- CanonicalJob `1—*` RuleEvidence、`1—*` JobDecision。
- CanonicalJob 通过 PossibleDuplicate 与其他 CanonicalJob 建立对称关系。
- 所有核心记录默认采用归档/保留策略；任何来源失败、职位过期或用户隐藏都不得级联删除原始事实和决定。

## 10. 状态与 FR 跟踪

| 实体 | 支撑需求 |
|---|---|
| Source、SourceRun | FR-JOB-001、002、004、004a、016、017 |
| RawPosting | FR-JOB-003、005、006、017 |
| CanonicalJob、PossibleDuplicate | FR-JOB-006～010、012～015 |
| RuleEvidence | FR-JOB-008～011、014、015 |
| JobDecision | FR-JOB-012、013、019 |
