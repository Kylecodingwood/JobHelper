> **OBSOLETE (Pivot 2026-08-09)** — 系统模板 / GRS / Profile→Roadmap 重算已废止。见 `delta/3.coding/api-contract.md`。

# profile-workflow-recompute

- 对齐：[`../../../1.req/profile/workflow-desc.md`](../../../1.req/profile/workflow-desc.md) WF-PRO-001、UC-PRO-003
- 触发：Profile 保存后存在派生影响；或用户手动打开待处理 `RecomputeRequest`
- 原则：**预览 → 显式确认 → 执行**；Roadmap 合并**仅**系统模板来源且未完成任务
- 状态：**设计完成**

## 1. 流程概览

```text
保存 Profile → 影响分析 → [有影响] 创建 PENDING RecomputeRequest
    → 用户选择 scopes → preview（ROADMAP 必预览）
    → confirm / defer / 取消
    → [confirm] 分 scope 调用下游 Port → 终态 + resultSummary
```

Profile **不**直接修改 Roadmap 表或 JobDecision；通过 Port 与 Outbox 协作。

## 2. 步骤详解

| 步骤 | 责任 | 输入/输出 |
|---|---|---|
| S1 加载 | ProfileQueryService | 当前 `profileVersion` |
| S2 编辑校验 | ProfileDomainService | 无效则停止，无新版本 |
| S3 影响分析 | ProfileImpactAnalyzer | `ImpactSummary` + `affectedScopes[]` |
| S4 保存确认 | ProfileCommandService | 用户确认后原子保存，version+1 |
| S5 登记请求 | RecomputeRequestService | 有影响 → `PENDING` + scopes；无影响 → 结束 |
| S6 范围选择 | API `/preview` | 用户勾选 `ROADMAP`/`JOB_GATE`/`JOB_RANK` 子集 |
| S7 Roadmap 预览 | RoadmapRecomputePort | 锚点快照、待更新任务列表、`previewToken` |
| S8 展示不可覆盖项 | API 响应 | JobDecision、用户 Roadmap 任务、已完成/归档、pin、DependencyOverride |
| S9 用户确认 | `/confirm` | `confirmRoadmapPreview=true` + token |
| S10 版本门禁 | RecomputeRequestService | 非当前 version → `SUPERSEDED` |
| S11 执行 | 各 Port | 见 §3 |
| S12 结果 | API | `COMPLETED` / `PARTIALLY_FAILED` / `FAILED` |

**推迟**：S6 后用户选 defer → `DEFERRED`（非保持 `PENDING`）。

**Profile 再次保存**：旧请求 → `SUPERSEDED`，基于新 version 新建请求。

## 3. 分 Scope 执行语义

### ROADMAP

- 调用 `RoadmapRecomputePort.confirmMerge(previewToken)`
- **合并策略**（与 Roadmap 域一致）：
  - 更新当前 `ACTIVE` Roadmap
  - **仅** `origin IN (SYSTEM_TEMPLATE)` 且 `status IN (TODO, IN_PROGRESS)` 的任务
  - 重算：`dueAt`、可安全合并的模板字段（title/description/completionCriteria 若模板版本未变且任务未 `userEdited`）
  - **保留**：`USER_CREATED`、`USER_TEMPLATE` 覆盖项、已完成/已归档、`userPinned`、`userEdited`、`DependencyOverride`
- 完成后 Roadmap 域发布 `ROADMAP_RECOMPUTED` → Action 域二次消费

### JOB_GATE

- 调用 `JobGateRecomputePort.recompute(profileVersion)`
- 重算 Gate 投影（含 LanguageProficiency 对照 `mandatoryLanguages`）
- **不**修改 `JobDecision`、Gate 用户覆盖

### JOB_RANK

- 调用 `JobRankRecomputePort.recompute(profileVersion)`
- 重算 Rank tier 投影；**不**修改 Job 状态

## 4. RecomputeRequest 状态机

| 当前 | 事件 | 下一 |
|---|---|---|
| PENDING | preview+confirm | RUNNING |
| PENDING | defer | DEFERRED |
| PENDING | 新 Profile 版本 | SUPERSEDED |
| DEFERRED | confirm | RUNNING |
| DEFERRED | 新 Profile 版本 | SUPERSEDED |
| RUNNING | 全 scope 成功 | COMPLETED |
| RUNNING | 部分失败 | PARTIALLY_FAILED |
| RUNNING | 全失败 | FAILED |
| PARTIALLY_FAILED | 重试失败 scope | RUNNING |

## 5. Outbox 事件（保存时，非执行时）

Profile 保存成功后按 diff 写入（事务内）：

- 时间线变更 → `PROFILE_TIMELINE_CHANGED`
- 目标变更 → `PROFILE_GOALS_CHANGED`
- 语言/技能/经历 → `PROFILE_SKILLS_CHANGED`

Roadmap 可订阅以展示「建议重算」徽章；执行仍以用户 confirm 为准。

## 6. 幂等与失败

- 同一 `(profileVersion, selectedScopes, roadmapPreviewToken)` 重复 confirm：返回相同 `resultSummary`
- 部分失败：成功 scope 结果保留；`resultSummary.failedScopes[]` 可单独重试
- 任一失败路径不产生半保存 Profile

## 7. 首次 Profile 与 Roadmap 基线

首次合法保存若 scope 含 `ROADMAP`：

1. `RecomputeRequest` 为 `PENDING`
2. 预览展示**首次生成**候选（非 merge），仍须用户确认
3. 确认后 Roadmap 域 `generate/confirm` 创建基线（见 `roadmap-workflow-generate`）

PlantUML：[`workflow-profile-recompute.puml`](workflow-profile-recompute.puml)
