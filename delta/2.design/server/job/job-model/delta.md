# job-model

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/job/entity.md`](../../../1.req/job/entity.md)、[`../../../1.req/job/entity.puml`](../../../1.req/job/entity.puml)
- 技术栈：PostgreSQL + Spring Data JPA（见 [`../../adr/tech-stack.md`](../../adr/tech-stack.md)）
- 图示：[`domain-job.puml`](domain-job.puml)

## 1. 包与表命名

| 逻辑实体 | PostgreSQL 表 | JPA 实体类（实现仓） |
|---|---|---|
| Source | `job_source` | `JobSource` |
| SourceRun | `job_source_run` | `JobSourceRun` |
| SourceDiagnostic | `job_source_diagnostic` | `JobSourceDiagnostic` |
| RawPosting | `job_raw_posting` | `JobRawPosting` |
| CanonicalJob | `job_canonical` | `CanonicalJob` |
| JobSourceRef | `job_source_ref` | `JobSourceRef` |
| RuleEvidence | `job_rule_evidence` | `JobRuleEvidence` |
| JobDecision | `job_decision` | `JobDecision` |
| PossibleDuplicate | `job_possible_duplicate` | `JobPossibleDuplicate` |

## 2. 枚举

### 2.1 Source 相关

| 枚举 | 值 | 说明 |
|---|---|---|
| `AdapterType` | `FREEHIRE_API`, `JOBSPY`, `MANUAL` | 适配器类型 |
| `SourceRole` | `PRIMARY`, `SUPPLEMENTAL`, `MANUAL` | FreeHire=PRIMARY；JobSpy=SUPPLEMENTAL；手动 URL=MANUAL |
| `TriggerType` | `SCHEDULED`, `MANUAL`, `RETRY` | SourceRun 触发方式 |
| `RunStatus` | `QUEUED`, `RUNNING`, `SUCCEEDED`, `PARTIAL_SUCCESS`, `FAILED`, `CANCELLED` | 运行终态 |
| `ErrorCategory` | `HTTP_403`, `HTTP_429`, `CAPTCHA`, `TIMEOUT`, `PARSE`, `VALIDATION`, `OTHER` | 诊断类别 |
| `ParseStatus` | `PENDING`, `VALID`, `INVALID` | RawPosting 解析状态 |

### 2.2 CanonicalJob 相关

| 枚举 | 值 | 说明 |
|---|---|---|
| `Seniority` | `INTERN`, `GRADUATE`, `JUNIOR`, `MID`, `SENIOR`, `LEAD`, `STAFF`, `PRINCIPAL`, `MANAGER`, `UNKNOWN` | 标准化职级 |
| `GateStatus` | `PASSED`, `FAILED`, `NEEDS_CONFIRMATION`, `UNKNOWN` | Gate 聚合投影 |
| `RankTier` | `HIGH`, `MEDIUM`, `LOW`, `UNRANKED` | Rank 聚合；Gate 未通过通常为 UNRANKED |
| `JobStatus` | `NEW`, `SHORTLISTED`, `IGNORED`, `APPLIED`, `ARCHIVED` | 用户业务状态 |
| `ValidityStatus` | `ACTIVE`, `EXPIRED`, `NEEDS_CONFIRMATION`, `UNKNOWN` | 有效性，与 jobStatus 分离 |

### 2.3 规则与决定

| 枚举 | 值 | 说明 |
|---|---|---|
| `EvaluationType` | `GATE`, `RANK`, `VALIDITY`, `DEDUPE` | RuleEvidence 评估类型 |
| `GateDimension` | `LOCATION`, `WORK_AUTH`, `SENIORITY`, `LANGUAGE` | Gate 四维度 |
| `RankDimension` | `TARGET_ROLE`, `SKILL`, `FRESHNESS`, `LOCATION_FIT`, `GROWTH` | Rank 五因素 |
| `EvidenceOutcome` | `PASS`, `FAIL`, `NEEDS_CONFIRMATION`, `UNKNOWN`, `POSITIVE`, `NEUTRAL`, `NEGATIVE` | Gate/Rank 结论 |
| `DecisionType` | `STATUS_CHANGE`, `GATE_OVERRIDE`, `DUPLICATE_CONFIRMATION`, `DUPLICATE_REJECTION` | JobDecision 类型 |
| `DuplicateStatus` | `PENDING`, `CONFIRMED`, `REJECTED` | 可能重复关系 |

## 3. 表结构（核心字段）

### 3.1 `job_source`

| 列 | 类型 | 约束 |
|---|---|---|
| `source_id` | UUID PK | |
| `code` | VARCHAR(64) UNIQUE | 如 `freehire`、`jobspy_linkedin`、`manual_url` |
| `adapter_type` | AdapterType | NOT NULL |
| `site` | VARCHAR(64)? | JobSpy 站点名；非 JobSpy 可空 |
| `role` | SourceRole | NOT NULL |
| `enabled` | BOOLEAN | JobSpy 各站点默认 true |
| `schedule` | VARCHAR(32) | `daily` / `manual-only` |
| `search_terms` | TEXT[] | 默认自 Profile 活动 TargetRole；用户可覆盖 |
| `risk_note` | TEXT? | JobSpy 站点必填 ToS/限速说明 |
| `config_version` | VARCHAR(32) | 每次配置变更递增 |
| `created_at` / `updated_at` | TIMESTAMPTZ | NOT NULL |

**种子数据（M1）**：`freehire`（PRIMARY）、JobSpy 8 站点各一条（SUPPLEMENTAL）、`manual_url`（MANUAL）。

### 3.2 `job_source_run`

| 列 | 类型 | 约束 |
|---|---|---|
| `source_run_id` | UUID PK | 每次运行独立 ID，不可复用 |
| `source_id` | UUID FK → job_source | |
| `trigger_type` | TriggerType | |
| `status` | RunStatus | |
| `parameters_snapshot` | JSONB | 脱敏配置快照（含合并后 searchTerms） |
| `config_version` | VARCHAR(32) | |
| `started_at` / `ended_at` | TIMESTAMPTZ? | RUNNING 有 started；终态有 ended |
| `request_count` … `failed_count` | INT | 均 ≥0 |
| `duration_ms` | INT? | 终态非负 |

索引：`(source_id, started_at DESC)`、`(status, started_at DESC)`。

### 3.3 `job_source_diagnostic`

| 列 | 类型 | 约束 |
|---|---|---|
| `diagnostic_id` | UUID PK | |
| `source_run_id` | UUID FK | |
| `category` | ErrorCategory | |
| `site` | VARCHAR(64)? | |
| `message` | TEXT | 脱敏 |
| `request_id` | VARCHAR(128)? | |
| `retry_count` | INT | 默认 0 |
| `item_key` | VARCHAR(256)? | 可选单条记录标识 |
| `occurred_at` | TIMESTAMPTZ | |

### 3.4 `job_raw_posting`

| 列 | 类型 | 约束 |
|---|---|---|
| `raw_posting_id` | UUID PK | |
| `source_id` / `source_run_id` | UUID FK | |
| `source_stable_id` | VARCHAR(256)? | 如 FreeHire `public_slug` |
| `source_url` / `apply_url` | TEXT? | 原值保留 |
| `canonical_url` | TEXT? | 规范化派生 |
| `raw_payload` | JSONB | 原样；日志不得复制 |
| `user_provided_jd` | TEXT? | 与抓取分离 |
| `fetched_at` / `source_updated_at` | TIMESTAMPTZ | fetched 必填 |
| `parse_status` | ParseStatus | INVALID 不进 Inbox |
| `parse_errors` | JSONB? | INVALID 时非空 |

**幂等唯一索引**：`(source_id, source_stable_id, source_updated_at)` WHERE `source_stable_id IS NOT NULL`；无 stable ID 时用 `(source_id, canonical_url)` 部分唯一。

### 3.5 `job_canonical`

| 列 | 类型 | 约束 |
|---|---|---|
| `job_id` | UUID PK | |
| `title` / `company` / `location` | TEXT? | 允许未知，禁止伪造 |
| `description` | TEXT? | 带来源与时间元数据 |
| `canonical_apply_url` | TEXT? | 硬去重键 |
| `posted_at` / `first_seen_at` / `last_seen_at` | TIMESTAMPTZ | first_seen 必填 |
| `expected_start_date` | DATE? | **缺失时 WORK_AUTH Gate → NEEDS_CONFIRMATION** |
| `seniority` | Seniority | 默认 UNKNOWN |
| `mandatory_languages` | TEXT[] | 空=未说明 |
| `gate_status` | GateStatus | 派生 |
| `rank_tier` | RankTier | Gate 失败通常 UNRANKED |
| `job_status` | JobStatus | 默认 NEW |
| `validity_status` | ValidityStatus | 默认 UNKNOWN |
| `hidden_by_default` | BOOLEAN | 派生 |
| `rule_version` / `last_evaluated_at` | VARCHAR / TIMESTAMPTZ | 已评估 Job 必填 |
| `version` | INT | 乐观锁 |

索引：`(job_status, hidden_by_default, last_seen_at DESC)`、`(canonical_apply_url)` 部分唯一（非空）、`(gate_status)`、`(rank_tier)`。

### 3.6 `job_source_ref`

| 列 | 类型 | 约束 |
|---|---|---|
| `job_source_ref_id` | UUID PK | |
| `job_id` | UUID FK | |
| `raw_posting_id` | UUID FK UNIQUE | 一个 RawPosting 至多归属一个活动 Job |
| `is_preferred` | BOOLEAN | 展示用首选来源 |
| `linked_at` | TIMESTAMPTZ | |

### 3.7 `job_rule_evidence`

追加式；Profile/规则变化写新行，不 UPDATE 旧结论。

| 列 | 类型 | 约束 |
|---|---|---|
| `evidence_id` | UUID PK | |
| `job_id` | UUID FK | |
| `evaluation_type` | EvaluationType | |
| `dimension` | VARCHAR(64) | GateDimension 或 RankDimension |
| `outcome` | EvidenceOutcome | |
| `rule_id` / `rule_version` | VARCHAR | |
| `fact_value` | JSONB | |
| `fact_source_ref` | VARCHAR | RawPosting/Profile 引用 |
| `explanation` | TEXT | 用户可读 |
| `evaluated_at` | TIMESTAMPTZ | |

索引：`(job_id, evaluation_type, dimension, evaluated_at DESC)`。

### 3.8 `job_decision`

追加式；后台同步不得 INSERT 用户状态变更。

| 列 | 类型 | 约束 |
|---|---|---|
| `decision_id` | UUID PK | |
| `job_id` | UUID FK | |
| `decision_type` | DecisionType | |
| `from_value` / `to_value` | VARCHAR? | STATUS_CHANGE 必填 |
| `reason` | TEXT? | GATE_OVERRIDE 非空 |
| `actor` | VARCHAR | M1 固定 `USER` |
| `based_on_job_version` | INT | 并发校验 |
| `supersedes_decision_id` | UUID? | |
| `created_at` | TIMESTAMPTZ | |

### 3.9 `job_possible_duplicate`

| 列 | 类型 | 约束 |
|---|---|---|
| `duplicate_id` | UUID PK | |
| `left_job_id` / `right_job_id` | UUID FK | 无序对：`LEAST/GREATEST` 唯一 |
| `signals` | JSONB | 标题/公司/地点相似信号 |
| `status` | DuplicateStatus | PENDING 不自动合并 |
| `decided_by_decision_id` | UUID? | 终态 FK → job_decision |

**合并 survivor 规则（CONFIRMED）**：
1. 保留 `first_seen_at` 较早（同则 `created_at` 较早）的 `job_id` 为 survivor。
2. 双方全部 `job_source_ref` 迁至 survivor；非 survivor 归档为 `ARCHIVED`。
3. `job_status` / `JobDecision` 冲突时由应用层暂停合并，API 返回冲突项供用户选择；不得静默覆盖 `SHORTLISTED`/`APPLIED`。
4. RawPosting 永不 DELETE。

## 4. 聚合与不变量

1. **Source 运行聚合**：`JobSource` → `JobSourceRun` → `JobRawPosting` + `JobSourceDiagnostic`。
2. **Job 决策聚合**：`CanonicalJob` → `JobSourceRef`、`JobRuleEvidence`、`JobDecision`；`PossibleDuplicate` 为跨聚合关系。
3. RawPosting / RuleEvidence / JobDecision **不可被标准化或同步覆盖**。
4. 硬去重仅 stable ID + 规范 apply URL；fuzzy 只建 `PossibleDuplicate`。
5. Gate 在标准化与硬去重**之后**执行；Rank 仅 Gate PASSED 或有效 GATE_OVERRIDE 后执行。
6. 后台同步**不得**将 `SHORTLISTED`/`IGNORED`/`APPLIED` 重置为 `NEW`。
7. `expected_start_date` 缺失 → WORK_AUTH 维度 `NEEDS_CONFIRMATION`，禁止推导毕业日或 +90 天。

## 5. searchTerms 派生

```
effectiveSearchTerms(source) =
  DISTINCT( profile.activeTargetRoles[].roleName
          ∪ source.searchTermsUserOverride[] )
```

- 读取 Profile 域只读投影（活动 `TargetRole.roleName`）。
- 用户通过 Source API 增删覆盖项；合并去重后写入 `parameters_snapshot` 与同步 Adapter 入参。
- 配置变更递增 `config_version`。

## 6. 关联 FR

| 实体 | FR |
|---|---|
| Source、SourceRun、Diagnostic | FR-JOB-001、002、004、004a、016、017、022、023 |
| RawPosting | FR-JOB-003、005、006、017 |
| CanonicalJob、PossibleDuplicate | FR-JOB-006～010、012～015 |
| RuleEvidence | FR-JOB-008～011、014、015 |
| JobDecision | FR-JOB-012、013、019、020 |
