# profile-crud

- 对齐：[`../../../1.req/profile/usecase-desc.md`](../../../1.req/profile/usecase-desc.md) UC-PRO-001～007
- 分层：`ProfileApplicationService`（用例编排）→ `ProfileDomainService`（校验/影响分析）→ Repository
- 事务：保存 Profile 与子实体同一事务；Outbox 同事务写入
- 状态：**设计完成**

## 1. 服务清单

### 1.1 `ProfileQueryService`

| 方法 | 说明 | UC |
|---|---|---|
| `getProfile()` | 返回唯一 Profile 聚合（含全部子实体 + 派生许可状态） | UC-PRO-001～002 |
| `getProfileOrEmpty()` | 无 Profile 时返回 Empty 占位，供 onboarding | UC-PRO-001 |
| `getFieldUsageMetadata()` | 字段用途/敏感性说明（不含个人值） | UC-PRO-004 |
| `getActiveTargetRoleSearchTerms()` | 活动 `TargetRole.roleName` 列表，供 Job Source 默认 | UC-PRO-001 |

### 1.2 `ProfileCommandService`

| 方法 | 说明 | UC |
|---|---|---|
| `saveProfile(SaveProfileCommand)` | 校验 → 影响分析 → 原子保存 → 递增 version → 登记 RecomputeRequest（若有影响）→ 写 Outbox | UC-PRO-001～002 |
| `validateProfileDraft(ProfileDraft)` | 仅校验，不落库 | UC-PRO-001 E1 |

**SaveProfileCommand 要点**：

- `expectedProfileVersion`：乐观锁；null 表示首次创建
- 子实体采用 **replace-by-id** 语义：请求体携带完整子集合，缺失 id 视为新建，未出现 id 视为删除
- 保存成功后若 `impact.hasAnyScope()`，创建 `PENDING` 的 `RecomputeRequest`（scopes 为受影响集合）

### 1.3 `ProfileImpactAnalyzer`

| 方法 | 说明 |
|---|---|
| `analyze(before, after)` | 比较聚合 diff，输出 `ImpactSummary` |
| `resolveAffectedScopes(impact)` | 映射为 `RecomputeScope[]` |

**Scope 映射规则**：

| 变更字段 | Scope |
|---|---|
| 主教育 `startDate` / `expectedGraduationDate` | `ROADMAP`（锚点）、可选 `JOB_GATE` |
| `WorkAuthorization` 时间线 | `ROADMAP`、`JOB_GATE` |
| `TargetRole` active/location/seniority | `ROADMAP`、`JOB_RANK`、`JOB_GATE` |
| `LanguageProficiency` | `JOB_GATE` |
| `SkillEvidence` / `ExperienceEvidence` | `JOB_RANK`（可选 `ROADMAP` 技能相关，v1 默认仅 RANK） |

无映射命中时不创建 `RecomputeRequest`（UC-PRO-002 A1）。

### 1.4 `RecomputeRequestService`

| 方法 | 说明 | UC |
|---|---|---|
| `getPendingOrDeferred()` | 当前用户可见的待处理请求 | UC-PRO-003 |
| `getById(id)` | 详情含 change/impact summary | UC-PRO-003 |
| `buildPreview(id, selectedScopes)` | 各 scope 预览；`ROADMAP` 委托 `RoadmapMergePreviewPort` | UC-PRO-003 |
| `defer(id)` | `PENDING` → `DEFERRED` | UC-PRO-003 A1 |
| `confirmAndExecute(id, ConfirmRecomputeCommand)` | 版本校验 → `RUNNING` → 分 scope 执行 → 终态 | UC-PRO-003 |
| `supersedeStaleRequests(profileVersion)` | 旧版本请求 → `SUPERSEDED` | WF-PRO-001 |

**ConfirmRecomputeCommand**：

- `selectedScopes: RecomputeScope[]`（用户勾选子集）
- `roadmapPreviewToken`：含 `ROADMAP` 时必填，来自 Roadmap 预览 API
- `confirmRoadmapPreview: boolean`：显式确认 Roadmap 预览

**分 scope 执行（Port 调用，非本域持久化）**：

| Scope | Port | 行为 |
|---|---|---|
| `ROADMAP` | `RoadmapRecomputePort.confirmMerge(previewToken)` | 仅 merge 系统模板来源且未完成任务 |
| `JOB_GATE` | `JobGateRecomputePort.recompute(profileVersion)` | 重算 Gate 投影，不改 JobDecision |
| `JOB_RANK` | `JobRankRecomputePort.recompute(profileVersion)` | 重算 Rank 投影 |

执行幂等：同一 `(profileVersion, scopes)` 重复 confirm 返回既有 `resultSummary`。

### 1.5 `ProfileBackupService`

| 方法 | 说明 | UC |
|---|---|---|
| `listBackups()` | 调度 + 手动导出索引 | UC-PRO-005～006 |
| `runScheduledBackup()` | 周日 03:00 触发；写全库快照；轮转保留 4 | UC-PRO-005 |
| `exportManual(ExportCommand)` | 用户指定路径导出 | UC-PRO-006 |
| `previewRestore(backupId)` | 校验包、版本、域清单；返回影响预览 | UC-PRO-007 |
| `confirmRestore(backupId, RestoreCommand)` | 二次确认后覆盖恢复 | UC-PRO-007 |

详见 [`../profile-workflow-backup/delta.md`](../profile-workflow-backup/delta.md)。

## 2. 领域校验（ProfileDomainService）

| 规则 ID | 校验 |
|---|---|
| BR-PRO-001 | 拒绝第二个 Profile 创建 |
| BR-PRO-002 | 许可时间线完整字段；禁止压缩为单布尔 |
| 实体不变量 | 日期区间、primary 唯一、language/skill 唯一、至少一个 active TargetRole |
| BR-PRO-004 | 影响摘要必须在 save 响应中返回 |
| 并发 | `expectedProfileVersion != current` → `409 PROFILE_VERSION_CONFLICT` |

## 3. 对外 Port（供其他域 / infra）

| Port | 方向 | 用途 |
|---|---|---|
| `ProfileSnapshotPort.getCurrentVersion()` | 出 | Roadmap/Job 读取 `profileVersion` |
| `ProfileAnchorPort.resolveAnchors()` | 出 | Roadmap 锚点（含 GRS/STAMP 规则） |
| `LanguageProficiencyPort.listForGate()` | 出 | Job Gate 语言维 |

PlantUML：[`service-profile.puml`](service-profile.puml)
