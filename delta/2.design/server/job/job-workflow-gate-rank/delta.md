# job-workflow-gate-rank

- 状态：**已细化**（M1）
- 对齐：[`../../../1.req/job/workflow-desc.md`](../../../1.req/job/workflow-desc.md) §3；[`../../../1.req/job/entity.md`](../../../1.req/job/entity.md)
- 服务：`JobGateRankService`（[`../job-crud/delta.md`](../job-crud/delta.md)）
- 图示：[`workflow-job-gate-rank.puml`](workflow-job-gate-rank.puml)
- 规则版本：M1 默认 `gate-v1` / `rank-v1`（写入 RuleEvidence 与 CanonicalJob.ruleVersion）

## 1. 执行顺序

```
RawPosting(VALID) → 标准化 → 硬去重 → Gate → Rank → hiddenByDefault 投影
```

Gate **不得**在硬去重之前执行（BR-JOB-005）。

## 2. Gate 决策表

对四维度分别评估，各写一条 `RuleEvidence`（evaluationType=GATE）。

| 维度 | PASS | NEEDS_CONFIRMATION / UNKNOWN | FAIL |
|---|---|---|---|
| **LOCATION** | Ireland；EU 且 JD 明确当地签证支持；remote 且明确可从 Ireland 合法受雇 | EU 默认待确认；地点未知 → UNKNOWN | 其他非 Ireland |
| **WORK_AUTH** | Profile 时间线在 `expectedStartDate` 具备适用授权 | **`expectedStartDate` 缺失 → NEEDS_CONFIRMATION**；授权事实不足 | 预期入职日明确且无授权且无 JD 支持 |
| **SENIORITY** | Intern、Graduate、Junior、未说明(UNKNOWN) | — | Senior、Lead、Staff、Principal、Manager |
| **LANGUAGE** | English、未说明；或 Profile `LanguageProficiency` 覆盖全部 mandatoryLanguages | 强制语言无法从 JD 判定 | 存在 Profile 缺失的强制语言 |

**WORK_AUTH 关键约束**（BR-JOB-007）：
- `expectedStartDate == null` → 维度 outcome = `NEEDS_CONFIRMATION`。
- **禁止**用 Profile 毕业日、firstSeenAt+90d 等推导入职日。

### 2.1 Gate 聚合

| 条件 | gateStatus |
|---|---|
| 任一维度 FAIL | `FAILED` |
| 无 FAIL，至少一维 NEEDS_CONFIRMATION | `NEEDS_CONFIRMATION` |
| 无 FAIL，至少一维 UNKNOWN（且无 NEEDS_CONFIRMATION） | `UNKNOWN` |
| 全部 PASS | `PASSED` |

`FAILED` → `hiddenByDefault=true`；NEEDS_CONFIRMATION/UNKNOWN 保留可见但可筛（BR-JOB-010）。

Gate 未通过 → `rankTier=UNRANKED`，跳过 Rank。

### 2.2 用户覆盖

`JobDecision` type=`GATE_OVERRIDE` 后：
- 原 RuleEvidence **不修改**；
- 岗位可进入 Rank 与考虑范围；
- 发布 `JOB_DECISION_RECORDED`；Action 关闭 `RESOLVE_GATE`（SOURCE_EVENT）。

## 3. Rank 投票规则

**前提**：gateStatus=`PASSED` 或有效 GATE_OVERRIDE。

对五因素各写一条 `RuleEvidence`（evaluationType=RANK，outcome=POSITIVE/NEUTRAL/NEGATIVE）：

| RankDimension | 评估要点 |
|---|---|
| `TARGET_ROLE` | 岗位 title/描述 vs Profile 活动 TargetRole |
| `SKILL` | JD 技能 vs Profile 技能证据 |
| `FRESHNESS` | postedAt / firstSeenAt 新鲜度 |
| `LOCATION_FIT` | 地点与 remote-from-Ireland 适配 |
| `GROWTH` | 早期职业发展价值 |

### 3.1 聚合（透明投票，无隐藏总分）

| 条件 | rankTier |
|---|---|
| ≥3 因素 `POSITIVE` **且** 0 因素 `NEGATIVE` | `HIGH` |
| ≥2 因素 `NEGATIVE` | `LOW` |
| 否则 | `MEDIUM` |

示例：3 POS + 1 NEU + 1 NEG → `MEDIUM`（因存在 1 NEG，不满足 HIGH）。

**禁止**：合成百分比或单一 opaque score（FR-JOB-010）。

## 4. 重算触发

| 触发 | 行为 |
|---|---|
| 新/更新 RawPosting | 单 Job 全量 Gate+Rank |
| Profile 时间线/目标/语言变更 | `recomputeForProfileChange` 批量 |
| 规则版本 bump | 全量或增量重算；新 RuleEvidence 追加 |
| 用户 GATE_OVERRIDE | 仅 Rank（Gate 证据保留） |

重算**不得**覆盖用户 jobStatus（FR-JOB-013）。

## 5. 与 Action 联动

| gateStatus | 领域事件 |
|---|---|
| NEEDS_CONFIRMATION / UNKNOWN（非 FAIL） | `JOB_GATE_NEEDS_CONFIRMATION` payload: gateDimension, gateStatus |
| 用户 GATE_OVERRIDE | `JOB_DECISION_RECORDED` → 关闭 `RESOLVE_GATE` |
| Gate 变为 PASSED（重算后） | 关闭 `RESOLVE_GATE`（SOURCE_EVENT） |

新 Job + jobStatus=NEW → `JOB_CREATED` → Action `REVIEW_NEW_JOB`。

## 6. 验收场景（设计期）

- Berlin “EU applicants” → LOCATION NEEDS_CONFIRMATION，hiddenByDefault 按聚合规则。
- 无 expectedStartDate → WORK_AUTH NEEDS_CONFIRMATION，不推导日期。
- Senior title → SENIORITY FAIL，默认隐藏。
- Gate PASSED，3 POS 0 NEG → HIGH；3 POS 1 NEG → MEDIUM；2 NEG → LOW。

## 7. FR

FR-JOB-006～011、020；FR-PRO-002～005（Profile 输入）；cross-domain `JOB_GATE_NEEDS_CONFIRMATION`。
