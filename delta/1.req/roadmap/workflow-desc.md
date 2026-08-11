# Roadmap 流程说明

- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 覆盖用例：`UC-RDM-001`～`UC-RDM-007`
- Markdown 语法：[`markdown-template-spec.md`](markdown-template-spec.md)
- 系统种子：[`system-template-v1.md`](system-template-v1.md)

## WF-RDM-001 模板到基线 Roadmap

1. **选择模板**：系统提供版本化、`templateType = SYSTEM` 的 `RoadmapTemplate`（首版内容见 `system-template-v1.md`）；用户可在 UI 上传或粘贴 Markdown 并预览为 `templateType = USER` 版本。
2. **合并模板**：用户模板按稳定任务键覆盖系统模板；不修改任一原模板版本。
3. **读取 Profile**：固定读取已确认的 `profileVersion`。
4. **解析锚点**：
   - `COURSE_START`、`GRADUATION`：来自 Profile 主教育/毕业日期。
   - `GRADUATE_RECRUITMENT_SEASON`：**`RDM-ANCHOR-GRS-001` v1** — 不晚于 `expectedGraduationDate` 的最近 9 月 1 日（毕业日为 9 月 1 则取该日；晚于当年 9 月 1 则取上一年 9 月 1 日）。
   - `STAMP_EXPIRY`：**`RDM-ANCHOR-STAMP-001` v1** — Profile `WorkAuthorization` 中与 Ireland Stamp 相关的最早适用 `validUntil`（优先 Stamp 2，若未来 Stamp 1G 更早则采用）。
5. **生成候选任务**：按 `anchor + relativeOffset` 解析日期，携带阶段、依赖、完成标准、origin 与 priority。
6. **完整性校验**：检查任务键唯一、依赖引用存在且无环；Markdown 错误按行号报告（见 `markdown-template-spec.md`）。
7. **预览与确认**：展示候选任务、锚点快照与规则版本；用户确认后才写入。
8. **原子创建**：生成 `Roadmap`、`RoadmapTask`、`TaskDependency` 和 `GenerationRecord`。
9. **发布 Action 同步事件**：见 WF-RDM-005；不在 Roadmap 域排序 Action。

### Profile 变更重算

- 须同样经过预览与确认。
- **仅** merge-recompute **系统模板来源且未完成**的任务（日期及可安全合并字段）。
- **保留**：用户创建任务、用户编辑、完成、归档、pin、依赖覆盖、用户模板覆盖项。

### 失败与边界

- 非必需锚点缺失：保留任务并标为 `UNSCHEDULED`，显示缺失原因。
- 必需 Profile 数据、模板版本或规则版本缺失：停止生成，不创建半成品。
- 依赖环或 Markdown 解析错误：展示具体任务/行号，等待用户修正。
- 相同输入重试：返回既有 Generation 结果，不重复创建 Roadmap。

## WF-RDM-002 任务推进与依赖覆盖

1. 用户打开任务，查看完成标准、origin、依赖和可执行性依据。
2. 用户将任务置为 `IN_PROGRESS`，或提交完成证据。
3. 系统检查完成标准及依赖：
   - 均满足：置为 `COMPLETED`；
   - 依赖未满足：阻止常规完成并列出 blocker；
   - 用户填写理由并二次确认：创建 `DependencyOverride` 后允许完成。
4. 系统重新计算后续阻塞与 `actionability`；发布 Action 同步事件。
5. 用户可恢复为 `TODO` 或归档；状态变化均保留历史。

### 状态迁移

| 当前状态 | 允许迁移 | 约束 |
|---|---|---|
| `TODO` | `IN_PROGRESS`, `COMPLETED`, `ARCHIVED` | 完成需满足标准与依赖，或有效覆盖 |
| `IN_PROGRESS` | `TODO`, `COMPLETED`, `ARCHIVED` | 同上 |
| `COMPLETED` | `TODO`, `ARCHIVED` | 恢复后重算下游 |
| `ARCHIVED` | `TODO` | 恢复需显式操作 |

`BLOCKED` 与 `UNSCHEDULED` 是派生可行动性，不替代任务业务状态。

## WF-RDM-003 领域事件追加

1. CV、Job 或 Behavioral 发送带 `eventId`、`sourceObjectId` 的后续事项。
2. 系统按 `sourceDomain + eventId` 幂等校验（`SourceDomain`：`CV` | `JOB` | `BEHAVIORAL`）。
3. 将事项追加为 origin 为 `CV_EVENT` / `JOB_EVENT` / `BEHAVIORAL_EVENT` 的任务；不重建基线。
4. 保存完成标准、截止日期、来源链接与建议优先级。
5. 发布 Action 同步事件；失败不影响既有 Roadmap。

## WF-RDM-004 系统模板保护性更新

1. 检测新 `templateType = SYSTEM` 的 `RoadmapTemplateVersion`，按 `templateTaskKey` 计算差异。
2. 将任务分类为：
   - **SafeAdd**：新任务且无同键用户内容；
   - **Protected**：已编辑、完成、归档或由用户模板覆盖；
   - **Conflict**：依赖或键无法安全合并。
3. 向用户预览分类及原因。
4. 用户确认后仅应用 `SafeAdd`；`Protected` 保持原样；`Conflict` 不自动应用。
5. 保存 `TemplateApplication` 并发布 Action 同步事件。

## WF-RDM-005 可执行性与 Action 事件（`UC-RDM-006`）

Roadmap 域职责：

1. 根据任务状态、依赖、截止日期与用户 pin 计算 `actionability` 与 blocker 证据。
2. 发布领域事件供 **Action 域**消费；Action 域按 deadline → blocker → user pin → system suggestion 排序（见 Action workflow）。
3. Roadmap UI 展示本地可执行性摘要；用户可导航至 Action Home 查看跨域优先项。

Roadmap **不**持久化或排序 `Action` 实体。

## 流程不变量

1. 系统模板版本不可变，且必须含来源 URL、license/使用依据和更新时间。
2. 用户模板与用户编辑优先；模板更新不得覆盖用户编辑、完成或归档。
3. 基线采用相对锚点；CV/Job/Behavioral 事件只追加任务。
4. 依赖图必须为 DAG；覆盖只改变当前任务完成资格，不修改依赖任务。
5. 生成、事件处理与模板应用必须幂等。
6. 首次生成与 Profile 重算必须预览并经用户确认。
7. v1 不依赖 AI，不提供 Notion sync 或 calendar sync。
