# Profile 流程说明

- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 覆盖用例：`UC-PRO-001`～`UC-PRO-007`

## WF-PRO-001 Profile 配置与影响重算

### 目标与边界

将单一本地用户的 Profile 以可校验、可版本追踪的方式保存，并将派生结果更新拆为独立、显式确认的重算步骤。保存 Profile 不得自动覆盖 Roadmap 编辑、任务完成记录或 Job Decision。

### 参与者与输入输出

- **参与者**：求职者、Profile 域；下游为 Roadmap 与 Job 域。
- **输入**：教育日期、目标岗位/职级/地点、语言熟练度、技能与经历、`WorkAuthorization` 时间线。
- **输出**：新 `profileVersion`、影响摘要、零个或一个 `RecomputeRequest`、重算结果摘要。

### 主流程

1. **加载**：无 Profile 时进入 `Empty` onboarding；已有 Profile 时加载当前版本。
2. **编辑**：用户录入或修改 Profile 聚合。
3. **校验**：检查必填项、日期区间、工时范围、枚举值及重复记录。
4. **影响分析**：比较保存前后差异，识别 Roadmap、Gate、Rank 影响。
5. **保存确认**：展示差异和影响；用户确认后原子化保存新版本。
6. **登记重算**：有影响时创建 `PENDING` 的 `RecomputeRequest`；无影响则流程结束。
7. **预览与显式确认**：用户选择重算范围；`ROADMAP` 范围须先展示模板/锚点/任务预览，用户确认后才进入执行；用户也可推迟（状态置为 `DEFERRED`）。
8. **执行重算**：按最新 Profile 版本运行确定性规则；分别记录各范围结果。
9. **结果处理**：展示建议与冲突，保留所有用户决定，由用户自行采纳；Roadmap 合并语义见 UC-PRO-003。

### 边界与失败路径

- 输入无效：停留在编辑步骤，不产生新版本。
- 用户取消保存：丢弃本次提交，不创建重算请求。
- 乐观并发冲突：拒绝覆盖最新版本，要求刷新并重新确认。
- 本地写入失败：事务回滚，保留表单输入供重试。
- 用户推迟：请求进入 `DEFERRED`（非保持 `PENDING`），可再次打开并确认执行。
- Profile 在重算前再次更新：旧请求进入 `Superseded`，不得对新版本执行。
- 部分重算失败：请求进入 `PartiallyFailed`，成功范围保留，失败范围可幂等重试。

## 状态说明

### Profile 生命周期

`Empty` → `Active`；每次有效保存增加 `profileVersion`。v1 不定义多用户切换，也不以删除 Profile 作为常规业务流程。

### RecomputeRequest 生命周期

| 状态 | 含义 | 可迁移至 |
|---|---|---|
| `PENDING` | 等待用户确认/预览 | `RUNNING`, `DEFERRED`, `SUPERSEDED` |
| `DEFERRED` | 用户推迟 | `RUNNING`, `SUPERSEDED` |
| `RUNNING` | 正在按选定范围计算 | `COMPLETED`, `PARTIALLY_FAILED`, `FAILED` |
| `COMPLETED` | 所选范围均成功 | 终态 |
| `PARTIALLY_FAILED` | 部分范围失败 | `RUNNING`（仅重试失败范围） |
| `FAILED` | 所选范围均失败 | `RUNNING` |
| `SUPERSEDED` | 已有更新 Profile 版本 | 终态 |

## 流程不变量

1. 重算输入必须引用不可歧义的 `profileVersion`。
2. 同一版本与范围的重复执行必须幂等。
3. Profile 保存与重算分离；没有用户明确确认（含 Roadmap 预览确认）不得进入 `RUNNING`。
4. 首次合法 Profile 保存后，若 Roadmap 受影响，须创建 `PENDING` 且 scope 含 `ROADMAP` 的请求，展示模板/锚点/任务预览；仅用户确认后才生成/合并 Roadmap。
5. 活动 `TargetRole.roleName` 为 Job 搜索词默认值；用户可在 Source 设置增删覆盖项。
6. 重算输出只能更新派生判断或提出建议，不得改写用户决定。
7. Roadmap 重算合并：更新当前 `ACTIVE` Roadmap；仅重算系统生成且未完成任务；保留用户创建任务、已完成/已归档任务、用户编辑、置顶与依赖覆盖。
8. AI 不参与该流程的必需路径。

## 可测试流程断言

- 首次合法保存后存在唯一 Profile，且有影响时只产生一个 `PENDING` 请求；含 `ROADMAP` 时须先展示预览再执行。
- 用户推迟后请求为 `DEFERRED`，非 `PENDING`。
- 无业务影响的编辑不产生重算请求。
- 旧版本请求不可执行。
- 任一失败路径均不产生半保存 Profile，也不丢失既有用户决定。
