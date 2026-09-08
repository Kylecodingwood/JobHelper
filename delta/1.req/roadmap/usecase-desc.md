# Roadmap 用例说明

> **Pivot 2026-08**：产品已改为「多文件夹 Todo / CompanyTracker / Document」。下文 UC-RDM-001～007（模板/生成/依赖）为**历史文档**，不再实现。现行实体见 [`entity.md`](entity.md)；API 见 [`../../3.coding/api-contract.md`](../../3.coding/api-contract.md)。

## 现行用例（摘要）

| 编号 | 名称 | 说明 |
|---|---|---|
| UC-RDM-F01 | 管理文件夹 | 创建（选 kind）/ 重命名 / 删除（不可删最后一个；级联内容） |
| UC-RDM-F02 | TodoList | 勾选、改 name/due/comment；防抖+失焦自动保存 |
| UC-RDM-F03 | CompanyTracker | 公司行 + status 枚举 + contact/note |
| UC-RDM-F04 | Document | 文档列表 + 标题 + TipTap HTML 正文；自动保存；切换文档才重置编辑器 |

---

## 历史用例索引（已废止实现）

| 编号 | 名称 | 优先级 | 主要参与者 | 关联 FR |
|---|---|---|---|---|
| `UC-RDM-001` | 管理 Roadmap Template | Must | 求职者、模板维护者 | `FR-RDM-001`, `FR-RDM-002`, `FR-RDM-007`, `FR-RDM-009`, `FR-RDM-010` |
| `UC-RDM-002` | 生成基线 Roadmap | Must | 求职者 | `FR-RDM-001`～`FR-RDM-003`, `FR-RDM-006`, `FR-RDM-011` |
| `UC-RDM-003` | 管理 Roadmap Task | Must | 求职者 | `FR-RDM-003`～`FR-RDM-005` |
| `UC-RDM-004` | 覆盖未满足依赖 | Must | 求职者 | `FR-RDM-004` |
| `UC-RDM-005` | 追加领域事件任务 | Must | 求职者、CV/Job/Behavioral 域 | `FR-RDM-003`, `FR-RDM-005`, `FR-RDM-012` |
| `UC-RDM-006` | 查看任务可执行性并触发 Action 事件 | Must | 求职者 | `FR-RDM-006` |
| `UC-RDM-007` | 应用系统模板更新 | Must | 求职者 | `FR-RDM-007` |

## `UC-RDM-001` 管理 Roadmap Template

- **优先级**：Must
- **参与者**：求职者；模板维护者（发布 `templateType = SYSTEM` 的 `RoadmapTemplate`）
- **触发条件**：应用内置模板首次可用、系统模板发布新版本，或用户在 UI 上传/粘贴 Markdown。
- **前置条件**：本地存储可用。
- **输入**：模板 Markdown（语法见 [`markdown-template-spec.md`](markdown-template-spec.md)）；`SYSTEM` 版本的来源 URL、license、更新时间；`USER` 版本的名称与内容。
- **主流程**：
  1. 系统列出只读版本化 `RoadmapTemplate`（`TemplateType = SYSTEM`）与用户 `RoadmapTemplate`（`TemplateType = USER`）。
  2. 用户选择上传 `.md` 或粘贴 Markdown；系统按行级语法解析任务、依赖、相对锚点与完成标准。
  3. 系统显示预览、解析警告及将覆盖的模板选择，不直接修改 Roadmap。
  4. 用户确认后保存新的 `RoadmapTemplateVersion`。
  5. Roadmap 生成时，用户模板内容优先于系统模板的同一逻辑任务。
- **替代/错误流程**：
  - A1：仅预览后取消，不保存模板。
  - E1：Markdown 无法解析或依赖引用不存在时，系统指出行/任务并禁止确认。
  - E2：缺少系统模板 URL、license 或更新时间时，该版本不得发布为可选系统模板。
- **后置条件**：存在可追溯模板版本；已有 Roadmap、编辑和完成记录不变。
- **业务规则**：
  - `BR-RDM-001`：系统模板原始任务必须来自信誉良好的爱尔兰官方、学校 Career Service 或等价来源，并保存 URL、license/使用依据、`updatedAt`。
  - `BR-RDM-002`：不得无许可整段复制来源内容；任务应为有来源引用的原创归纳。
  - `BR-RDM-003`：用户模板通过 UI 上传/粘贴并预览；v1 不含 Notion sync。
  - `BR-RDM-004`：用户模板覆盖系统模板，但系统更新不得覆盖用户内容。
- **关联 FR**：`FR-RDM-001`、`FR-RDM-002`、`FR-RDM-007`、`FR-RDM-009`、`FR-RDM-010`
- **验收场景**：
  - Given 用户粘贴包含三个合法任务的 Markdown，When 预览并确认，Then 系统保存用户模板版本且显示解析后的任务与依赖。
  - Given 系统模板缺少来源 URL，When 尝试发布，Then 系统拒绝将其标为可用。

## `UC-RDM-002` 生成基线 Roadmap

- **优先级**：Must
- **参与者**：求职者
- **触发条件**：首次完成 Profile，或用户确认 Profile 变化后的 Roadmap 重算。
- **前置条件**：Profile 含主教育日期、目标岗位/地点和工作许可时间线；至少一个可用 `RoadmapTemplateVersion`。
- **输入**：`profileVersion`、选定模板版本、`ruleVersion`（含 `RDM-ANCHOR-GRS-001` v1、`RDM-ANCHOR-STAMP-001` v1）；相对锚点 `COURSE_START`、`GRADUATE_RECRUITMENT_SEASON`、`GRADUATION`、`STAMP_EXPIRY`。
- **主流程**：
  1. 系统解析 Profile 锚点；`GRADUATE_RECRUITMENT_SEASON` 按 `RDM-ANCHOR-GRS-001` v1 计算；`STAMP_EXPIRY` 按 `RDM-ANCHOR-STAMP-001` v1 取最早适用 Ireland Stamp `validUntil`；显示规则依据。
  2. 系统合并 `templateType = SYSTEM` 与优先的 `templateType = USER` 模板。
  3. 系统将相对偏移解析为候选任务目标日期，展示阶段、任务、依赖及完成标准**预览**。
  4. 用户确认预览后，系统原子创建 Roadmap、任务及依赖，并记录每个任务的模板来源、逻辑键、规则版本、优先级与完成标准。
  5. 系统发布任务快照/可执行性变更事件，供 Action 域消费（见 `UC-RDM-006`）。
- **Profile 变更重算（替代主流程 3–4）**：
  1. 系统展示 merge 预览：哪些系统模板来源且未完成任务将重算日期/可合并字段。
  2. 用户确认后，**仅** merge-recompute 上述任务；保留用户创建任务、用户编辑、完成、归档、pin、依赖覆盖及用户模板覆盖项。
- **替代/错误流程**：
  - A1：某非必需锚点缺失时，相关任务标记 `UNSCHEDULED` 并说明缺失锚点。
  - A2：用户取消生成/重算预览，Roadmap 不变。
  - E1：必需锚点、模板或规则版本无效时，不生成半成品 Roadmap，并提供修正入口。
  - E2：出现依赖环时，拒绝生成并列出环路。
- **后置条件**：创建或更新 `Active` Roadmap；任务与依赖可追溯；Action 域收到事件。
- **业务规则**：
  - `BR-RDM-005`：生成以确定性规则为主，不依赖 AI。
  - `BR-RDM-006`：所有模板任务日期必须由相对锚点和偏移推导，不能把个人绝对日期写入系统模板。
  - `BR-RDM-014`：首次生成与 Profile 重算均须预览并经用户确认。
  - `BR-RDM-015`：Profile 重算不得覆盖用户任务、编辑、完成、归档、pin 或依赖覆盖。
- **关联 FR**：`FR-RDM-001`～`FR-RDM-003`、`FR-RDM-006`、`FR-RDM-011`
- **验收场景**：
  - Given 课程开始、毕业与 Stamp 到期日期完整，When 预览并确认生成，Then 每个可排期模板任务可追溯至锚点、偏移、模板版本和规则版本。
  - Given 用户已完成并 pin 某系统任务，When Profile 毕业日变化并确认重算，Then 该任务完成状态与 pin 不变，仅未完成系统任务日期更新。

## `UC-RDM-003` 管理 Roadmap Task

- **优先级**：Must
- **参与者**：求职者
- **触发条件**：用户查看或编辑 Roadmap。
- **前置条件**：存在 `Active` Roadmap。
- **输入**：任务新增/修改；状态 `TODO`、`IN_PROGRESS`、`COMPLETED`、`ARCHIVED`；完成证据；依赖；截止日期；用户 pin。
- **主流程**：
  1. 系统展示阶段、依赖、阻塞原因、完成标准、来源和可执行性依据。
  2. 用户新增自定义任务或编辑现有任务。
  3. 用户开始任务，或提交满足完成标准的证据并完成任务。
  4. 系统保存变更记录并重新计算下游阻塞；发布可执行性变更事件（`UC-RDM-006`）。
  5. 用户可恢复已完成任务或归档任务。
- **替代/错误流程**：
  - A1：完成标准声明无需证据时，用户可直接确认完成。
  - E1：新增依赖形成环时，拒绝保存。
  - E2：依赖未满足时转入 `UC-RDM-004`。
- **后置条件**：任务、依赖、完成记录一致；Action 域可同步。
- **业务规则**：
  - `BR-RDM-007`：每个任务必须有完成标准、origin、priority；模板任务还须保留 template task key。
  - `BR-RDM-008`：用户编辑、完成、恢复、归档均必须可追溯且优先于模板更新。
- **关联 FR**：`FR-RDM-003`～`FR-RDM-005`
- **验收场景**：
  - Given 一个未阻塞任务，When 用户提交符合标准的证据并完成，Then 下游阻塞更新且发布 Action 同步事件。

## `UC-RDM-004` 覆盖未满足依赖

- **优先级**：Must
- **参与者**：求职者
- **触发条件**：用户尝试完成仍有未满足依赖的任务。
- **前置条件**：任务状态非 `ARCHIVED`，至少一个依赖未满足。
- **输入**：覆盖理由。
- **主流程**：
  1. 系统阻止常规完成并列出阻塞依赖。
  2. 用户选择覆盖，填写非空理由并再次确认。
  3. 系统记录 `DependencyOverride`，将任务标为 `COMPLETED` 且带覆盖标识。
  4. 系统重新计算可执行性并发布事件（`UC-RDM-006`）。
- **替代/错误流程**：用户取消或理由为空时，任务保持未完成。
- **后置条件**：完成状态、未满足依赖快照、理由和时间可审计。
- **业务规则**：`BR-RDM-009`：覆盖不等于依赖已完成，依赖任务状态不得被连带修改。
- **关联 FR**：`FR-RDM-004`
- **验收场景**：
  - Given 任务被一个 `TODO` 依赖阻塞，When 用户提供理由并确认覆盖，Then 当前任务完成且保留覆盖记录，前置任务仍为 `TODO`。

## `UC-RDM-005` 追加领域事件任务

- **优先级**：Must
- **参与者**：求职者；CV、Job、Behavioral 域
- **触发条件**：CV review、Job decision/期限或 Behavioral preparation 产生可执行后续事项。
- **前置条件**：存在 `Active` Roadmap；事件具有稳定 `sourceObjectId` 与 `eventId`。
- **输入**：`sourceDomain`（`CV` \| `JOB` \| `BEHAVIORAL`）、`sourceObjectId`、原因、建议任务、截止日期、完成标准、优先级。
- **主流程**：
  1. 系统验证事件来源与幂等键。
  2. 系统将新事项追加为 `RoadmapTask`（origin 为 `CV_EVENT` / `JOB_EVENT` / `BEHAVIORAL_EVENT`），不重建基线 Roadmap。
  3. 系统保留 origin 与源对象链接，并计算依赖和优先级。
  4. 系统发布任务追加事件供 Action 域消费。
  5. 用户可按普通任务编辑、完成或归档。
- **替代/错误流程**：
  - A1：相同事件已处理时返回既有结果，不重复追加。
  - E1：源对象不存在或完成标准为空时拒绝追加并记录可诊断错误。
- **后置条件**：事件任务可追溯到 CV/Job/Behavioral；基线任务和用户决定不变。
- **业务规则**：`BR-RDM-010`：领域事件只追加，不得整体覆盖或再生 Roadmap。
- **关联 FR**：`FR-RDM-003`、`FR-RDM-005`、`FR-RDM-012`
- **验收场景**：
  - Given 某 Job 产生申请截止日前准备材料事件，When 事件首次处理，Then 追加带 Job 链接和截止日期的任务；重复处理不产生副本。

## `UC-RDM-006` 查看任务可执行性并触发 Action 事件

- **优先级**：Must
- **参与者**：求职者
- **触发条件**：Roadmap/任务/事件/Profile 重算完成，或用户从 Roadmap 请求查看“下一步”。
- **前置条件**：存在 `Active` Roadmap。
- **输入**：任务状态、依赖、截止日期、用户 pin、规则版本。
- **主流程**：
  1. 系统计算每个未完成任务的 `actionability`（`ACTIONABLE` / `BLOCKED` / `UNSCHEDULED`）及阻塞摘要。
  2. 系统发布 `RoadmapTaskActionabilityChanged`（或等价）领域事件，含任务 ID、可执行性、deadline、pin、blocker 证据；**不在 Roadmap 域创建或排序 `Action`**。
  3. 用户在 Roadmap 查看可执行性摘要；可选择导航至 **Action 域 Home**（`UC-ACT-001`）查看跨域优先 Action。
- **替代/错误流程**：
  - A1：Action 域暂不可用时，Roadmap 仍展示本地可执行性，不虚构 Action 列表。
  - A2：无可行动任务时显示空状态与阻塞摘要。
- **后置条件**：Action 域可幂等消费事件；Roadmap 任务状态不被 Action 排序改变。
- **业务规则**：
  - `BR-RDM-011`：Roadmap 只提供任务事实与可执行性证据；优先级排序归属 Action 域。
  - `BR-RDM-012`：AI 建议不得在本域创建不可撤销高优先 Action。
- **关联 FR**：`FR-RDM-006`（事件桥接）；完整 Action 体验见 Action 域 `FR-ACT-001`～`FR-ACT-007`
- **验收场景**：
  - Given 某任务因依赖未满足为 `BLOCKED`，When 前置任务完成，Then Roadmap 发布可执行性变更事件且用户可从 Roadmap 跳转 Home 看到更新后的 Action。

## `UC-RDM-007` 应用系统模板更新

- **优先级**：Must
- **参与者**：求职者
- **触发条件**：存在更高版本的 `templateType = SYSTEM` 的 `RoadmapTemplateVersion`。
- **前置条件**：存在由旧模板生成的 `Active` Roadmap。
- **输入**：旧/新模板版本差异、当前任务及用户变更记录。
- **主流程**：
  1. 系统按稳定 `templateTaskKey` 比较新增、修改和删除。
  2. 系统预览可安全新增项、冲突项和保留项。
  3. 用户确认应用。
  4. 系统仅新增未出现的系统任务；对已有用户编辑、完成、归档或用户模板覆盖项保持原样。
  5. 系统记录应用结果及未应用原因，并发布 Action 同步事件（`UC-RDM-006`）。
- **替代/错误流程**：
  - A1：用户取消，Roadmap 不变。
  - E1：新模板依赖环或元数据无效时，拒绝更新。
- **后置条件**：模板应用记录可追溯；所有用户内容和决定保持。
- **业务规则**：`BR-RDM-013`：模板更新采用保护性 merge，绝不覆盖用户编辑、完成、归档或用户模板内容。
- **关联 FR**：`FR-RDM-007`
- **验收场景**：
  - Given 用户已编辑并完成旧系统任务，且新模板修改该任务并新增另一任务，When 应用更新，Then 旧任务保持用户版本和完成状态，仅新增任务被加入。

## v1 明确排除

- 不提供 Notion sync、外部日历同步、重复任务或系统级推送。
- Roadmap 生成不要求 AI；AI 只能作为可选建议，不能覆盖模板、任务或用户决定。
- Roadmap 域不拥有 `Action` 聚合；不实现 Action 排序 UI（归属 Action/Home）。
