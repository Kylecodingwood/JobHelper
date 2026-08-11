> **OBSOLETE (Pivot 2026-08-09)** — 系统模板 / GRS / Profile→Roadmap 重算已废止。见 `delta/3.coding/api-contract.md`。

# roadmap-workflow-generate

- 对齐：[`../../../1.req/roadmap/workflow-desc.md`](../../../1.req/roadmap/workflow-desc.md) WF-RDM-001～005
- 覆盖：首次基线生成、Profile 变更 merge 重算、模板保护性更新、领域事件追加、Action 事件发布
- 原则：**预览 → 用户确认 → 原子写入**；**无 Action 实体**
- 状态：**设计完成**

## 1. 工作流总览

| 工作流 ID | 名称 | 触发 |
|---|---|---|
| WF-RDM-GEN-001 | 首次基线生成 | Profile 首次保存 + 用户确认 ROADMAP / `generate/confirm` |
| WF-RDM-GEN-002 | Profile merge 重算 | Profile RecomputeRequest 含 ROADMAP + preview confirm |
| WF-RDM-GEN-003 | 系统模板保护性更新 | 新 SYSTEM 版本可用 + 用户 apply |
| WF-RDM-GEN-004 | 领域事件追加 | CV/JOB/BEHAVIORAL Outbox |
| WF-RDM-GEN-005 | 可执行性与 Action 事件 | 任务/依赖/merge 后 |

## 2. WF-RDM-GEN-001 首次基线生成

| 步骤 | 行为 |
|---|---|
| G1 | 读取已确认 `profileVersion`；拒绝无 Profile |
| G2 | 加载 SYSTEM `activeVersion` + 可选 USER 版本 |
| G3 | USER 模板按 `templateTaskKey` 覆盖 SYSTEM 定义（不修改原版本实体） |
| G4 | `AnchorResolver` 解析锚点（见 §5） |
| G5 | 对每个 TemplateTaskDefinition 计算 `dueAt`；缺失 → 候选 `UNSCHEDULED` |
| G6 | 展开 dependsOn → TaskDependency；拓扑校验 DAG |
| G7 | `buildGeneratePreview` → 返回 candidateTasks + anchorSnapshot + previewToken |
| G8 | 用户取消 → 不写库 |
| G9 | `confirmGenerate`：单事务创建 Roadmap(ACTIVE)、RoadmapTask、TaskDependency、GenerationRecord |
| G10 | `ActionabilityEngine.recalculate` |
| G11 | Outbox：`ROADMAP_TASK_*` + `ROADMAP_RECOMPUTED` |

**失败**：必需输入无效或依赖环 → 不创建半成品 Roadmap（GenerationRecord=FAILED）。

**幂等**：相同 `inputHash` 重复 confirm → 返回既有 `roadmapId`。

## 3. WF-RDM-GEN-002 Profile merge 重算

与 Profile `profile-workflow-recompute` 联动。

| 步骤 | 行为 |
|---|---|
| M1 | `buildMergePreview(profileVersion)` |
| M2 | 筛选 **仅** `origin=SYSTEM_TEMPLATE` 且 `status∈{TODO,IN_PROGRESS}` 且 `!userEdited` |
| M3 | **排除**：USER_CREATED、USER_TEMPLATE 覆盖、COMPLETED、ARCHIVED、userPinned、有 DependencyOverride |
| M4 | 对新 anchorSnapshot 重算 `dueAt`；展示 old/new 对比 |
| M5 | 用户确认 → `confirmMerge` |
| M6 | 批量更新选中任务；不触碰 protected 列表 |
| M7 | 重算 actionability + Outbox |

**不变量**：不新建并行 Roadmap；更新当前 ACTIVE 实例。

## 4. WF-RDM-GEN-003 系统模板保护性更新

| 步骤 | 行为 |
|---|---|
| U1 | 比较 fromVersion → toVersion by `templateTaskKey` |
| U2 | 分类 SafeAdd / Protected / Conflict |
| U3 | 预览给用户 |
| U4 | 确认后仅插入 SafeAdd 新任务（新 taskId，origin=SYSTEM_TEMPLATE） |
| U5 | Protected：已编辑/完成/归档/USER 覆盖 — 保持原样 |
| U6 | 写 TemplateApplication(APPLIED) + Action 同步事件 |

## 5. 锚点解析（确定性）

### GRADUATE_RECRUITMENT_SEASON — `RDM-ANCHOR-GRS-001` v1

```text
输入: expectedGraduationDate (来自 isPrimary EducationPeriod)
令 Y = 毕业日年份
候选1 = Y-09-01
候选2 = Y-09-01 若毕业日 >= 当年9月1 则再考虑 Y-1-09-01
输出: 不超过 expectedGraduationDate 的最近 9月1
```

| 毕业日 | GRS |
|---|---|
| 2026-06-15 | 2025-09-01 |
| 2026-09-01 | 2026-09-01 |
| 2026-11-20 | 2026-09-01 |

### STAMP_EXPIRY — `RDM-ANCHOR-STAMP-001` v1

从 Profile WorkAuthorization 取 Ireland Stamp 相关条目最早 `validUntil`（Stamp 2 优先；未来 Stamp 1G 更早则采用）。

### COURSE_START / GRADUATION

主教育 `startDate` / `expectedGraduationDate`。

## 6. WF-RDM-GEN-004 领域事件追加

| 步骤 | 行为 |
|---|---|
| E1 | 接收信封：`sourceDomain`, `eventId`, `sourceObjectId`, `payload` |
| E2 | `(sourceDomain, eventId)` 查 receipt；已 PROCESSED → 返回既有 task |
| E3 | 按事件类型映射建议任务（title、criteria、dueAt、priority） |
| E4 | 创建 RoadmapTask：`CV_EVENT` / `JOB_EVENT` / `BEHAVIORAL_EVENT` |
| E5 | receipt=PROCESSED；Outbox ROADMAP_TASK_ACTIONABLE |

**Behavioral 事件名**（非 Interview）：`BEHAVIORAL_ANSWER_GAP_DETECTED` 等。

不重建基线、不删除用户任务。

## 7. WF-RDM-GEN-005 可执行性与 Action 事件

Roadmap 域职责边界：

1. 计算 `actionability` 与 `blockingTaskIds`
2. 发布 `ROADMAP_TASK_ACTIONABLE` / `BLOCKED` / `COMPLETED` / `ARCHIVED` / `ROADMAP_RECOMPUTED`
3. **不**创建、排序或持久化 Action
4. Roadmap UI 展示摘要；Home 跨域优先级 → `GET /api/v1/actions`

Action 关闭 `COMPLETE_ROADMAP_TASK` 由 Action 域消费上述事件（见 cross-domain-events §4.2）。

## 8. 与 Profile 首次保存链

```text
PUT /profile (首次)
  → PENDING RecomputeRequest (ROADMAP)
  → POST /profile/recompute-requests/{id}/preview (ROADMAP)
      → 内部 POST /roadmap/generate/preview
  → POST .../confirm + generate/confirm
  → ACTIVE Roadmap 创建
```

PlantUML：[`workflow-roadmap-generate.puml`](workflow-roadmap-generate.puml)
