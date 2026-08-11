# Behavioral 流程说明

- 状态：已细化
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 用例入口：[usecase-desc.md](usecase-desc.md)
- 流程图：[workflow.puml](workflow.puml)

## 1. 流程边界

本域包含题库选择、STAR Evidence 维护与复用、答案文本版本、双维度反馈、建议决策和敏感对象删除。v1 只处理文本，不请求麦克风权限，不录音、不做语音分析，也不计算面试成功概率。

## 2. WF-BHV-01 选择或创建问题

**关联用例：** UC-BHV-001。

1. 系统从 [curated-question-bank.md](curated-question-bank.md)（`curationVersion=v1`）加载或同步策展题。
2. 用户按能力主题浏览并选择 `CURATED` 问题；或创建 `USER_DEFINED` 问题。
3. 用户可隐藏策展题或隐藏/删除自定义题（删除带 Answer 的题须预览）；不能覆盖策展原文。
4. 策展题库不可用时，自定义题的浏览、创建和练习继续可用。
5. 疑似重复的自定义题只提示，不自动合并。

## 3. WF-BHV-02 Evidence 与答案版本

**关联用例：** UC-BHV-002、UC-BHV-003。

1. 用户选择题目。
2. 用户从库中选择零个或多个 `STAREvidence`，或新建经历。
3. 新建 Evidence 时分别输入 Situation、Task、Action、Result：
   - 四部分完整：保存为完整 Evidence 修订；
   - 有缺失：允许保存草稿，但显著标识缺口。
4. 用户编写非空答案纯文本。
5. 用户保存时创建不可变 `AnswerVersion`，固定问题快照和所引用 Evidence 修订。
6. 后续修改创建子版本；不覆盖历史版本。
7. 未完成练习可生成可追溯 Action。

**边界与失败：**

- Evidence 可跨问题、跨答案复用；一个答案也可引用多个 Evidence。
- Evidence 更新形成修订；已保存答案仍指向当时版本。
- 空答案不能送审；保存失败时保留输入且不覆盖最新已保存版本。
- 系统不自动补充用户未提供的 STAR 事实或指标。

## 4. WF-BHV-03 反馈运行

**关联用例：** UC-BHV-004、UC-BHV-006。

1. 用户选择不可变答案版本并发起反馈。
2. 系统固定答案、问题及 Evidence 输入版本。
3. 系统**始终**运行 [local-feedback-rules.md](local-feedback-rules.md)；若需 AI 增强：
   - provider 抽象已配置并启用；
   - 查询[全局 AI 同意注册表](../ai-consent.md)相关类别；
   - 展示实际发送范围，允许脱敏或取消；
   - 检查输入版本、提示版本和 provider/model 构成的缓存键。
4. 缓存命中则记录并复用；未命中则调用 provider，并记录 provider、model、输入、提示、成本、输出与状态。
5. 系统验证输出不得引入新事实。
6. 系统按两个独立维度形成反馈（本地 + 可选 AI）：
   - `STAR_COMPLETENESS`：指出缺失的 Situation/Task/Action/Result；
   - `EVIDENCE_SPECIFICITY`：指出缺少主体、行为、约束、结果或可验证细节的泛化表述。
7. 每项反馈必须引用答案片段并给出依据；不生成总成功概率。

**失败与恢复：**

- Provider 默认关闭、未同意或不可用：不外发；运行本地检查或明确提示能力不可用。
- 调用失败：反馈运行 `FAILED`，允许重试；答案和历史反馈不变。
- 输出发明事实：对应项 `FACT_CONFLICT`，不得接受。

## 5. WF-BHV-04 建议决策与修订

**关联用例：** UC-BHV-005。

1. 用户逐项查看维度、答案片段、问题、依据和建议。
2. 用户选择 `ACCEPTED`、`REJECTED` 或 `IGNORED`。
3. 系统保存每项决定；忽略与拒绝语义分离。
4. 接受建议可作为编辑指引，但系统不得未经确认改变答案。
5. 用户确认修订文本后创建新的 `AnswerVersion`，记录父版本和已采纳反馈项。
6. 历史答案、反馈和决定保持不变。

## 6. WF-BHV-05 AI 同意生命周期

1. AI provider 默认禁用；题库、Evidence、版本管理和基本本地检查不依赖 AI。
2. 用户启用 provider 后，按数据类别分别同意，不能从一种类别推导另一种。
3. 类别同意可复用于后续调用，但每次调用仍展示实际发送范围。
4. 用户可在调用前脱敏或取消。
5. 撤销某类别后，新的含该类别调用立即被阻止。
6. 历史调用保持可追踪，直到所属答案/Evidence 被用户手动删除。

## 7. WF-BHV-06 敏感对象删除

**关联用例：** UC-BHV-007。

1. 用户选择删除 Answer、STAR Evidence 或（可选）`USER_DEFINED` 问题。
2. 系统计算并预览全部影响：
   - Answer：所有版本、反馈、反馈项、决定、AI 内容和 Action；
   - Evidence：所有修订、`AnswerEvidenceLink`、依赖反馈/AI 内容；**Answer 正文保留**并标记需复核；
   - USER_DEFINED 问题：关联 Answer 数量与快照保留策略。
3. 用户取消则无变化。
4. 用户二次确认后执行幂等级联硬删除（Evidence 删除不删 Answer 文本）。
5. 全部成功后只保留无内容审计事件；部分失败则报告剩余项并允许重试。

## 8. 业务状态与技术状态

### 8.1 BehavioralAnswer

| 状态 | 含义 |
|---|---|
| `DRAFT` | 有未完成文本或尚未请求反馈 |
| `READY_FOR_FEEDBACK` | 当前版本非空，可请求反馈 |
| `FEEDBACK_AVAILABLE` | 当前版本存在已完成反馈 |
| `REVISED` | 已基于反馈或用户编辑创建后续版本 |

状态用于导航，不得覆盖 `AnswerVersion` 历史。

### 8.2 AnswerFeedback

| 状态 | 含义 |
|---|---|
| `REQUESTED` | 输入已固定 |
| `RUNNING` | 正在检查 |
| `COMPLETED` | 可展示反馈项 |
| `FAILED` | 运行失败，可重试 |
| `CANCELLED` | 用户在发送前取消 |

### 8.3 技术状态分离

- AI 超时、缓存命中、成本元数据和重试属于 `AIInvocation`，不改变答案业务文本。
- `FACT_CONFLICT` 是反馈可执行性校验，不是用户决定。
- 删除只有所有目标内容删除后才可标记成功。

## 9. 流程级验收

- 策展和自定义题均能启动完整文本练习。
- Evidence 可复用；答案每次保存形成不可变版本并固定 Evidence 修订。
- 反馈同时覆盖 STAR 完整性和证据具体性，逐项引用答案片段。
- 页面和输出中不存在成功概率，也不存在语音入口。
- AI 未启用/未同意时无外发；调用、缓存和成本均可追踪。
- 数据不自动过期；手动删除先预览影响，成功后只留无内容审计事件。
