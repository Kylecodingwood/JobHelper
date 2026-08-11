# AI Provider 方向（设计期输入）

- 状态：Provider 选择延期，不阻塞 1.req
- 适用域：CV、Behavioral；Roadmap 仅允许可选建议
- 默认：未配置、关闭
- 同意：[ai-consent.md](ai-consent.md)（全局按类别）
- 本地兜底：CV [local-review-rules.md](cv/local-review-rules.md)；Behavioral [local-feedback-rules.md](behavioral/local-feedback-rules.md)

## 需求边界

核心职位同步、清洗、去重、Gate、基础 Rank 和 Roadmap 必须在没有 AI 时运行。AI 只处理需要语言理解或写作的任务：

1. 通用 CV 健康检查（**本地规则始终可用**；AI 为增强）；
2. CV 与具体 JD 的证据对齐和修改建议（**岗位定制评审不依赖外部 AI**；本地 JD 规则为基线）；
3. STAR 答案的结构缺口与证据具体性反馈（**本地规则始终可用**）；
4. 可选的 Roadmap 补充建议，不得覆盖确定性模板。

## 本地优先与可选增强

| 层级 | 说明 |
|---|---|
| **基线（Must）** | 各域 `local-*-rules.md` 确定性检查；AI 关闭或无 consent 时仍须完成 UC-CV-003/004、UC-BHV-004 |
| **Cursor Skill（可选）** | 设计期可接入 Cursor Agent Skill，在用户显式启用且 consent 满足时增强评审/反馈；不改变本地规则契约 |
| **Cursor SDK（后续）** | 与 Skill 同契约的编程式调用路径；provider-neutral 抽象的实现候选之一 |
| **云端 Provider（可选）** | OpenAI / Anthropic / Gemini 等；经同一 `AIProvider` 契约与全局 consent |

业务域只依赖 provider-neutral 能力契约，不直接绑定某一 SDK；Skill/SDK 视为可选实现，而非评审/反馈的前置条件。

## Provider-neutral 能力契约

后续设计的 `AIProvider` 至少需要表达：

- provider 与 model 标识；
- 结构化输入/输出能力；
- 超时、取消、重试和错误分类；
- token/费用元数据（provider 支持时）；
- 输入版本、prompt 版本、输出与缓存键；
- 敏感数据类别与有效授权；
- 不支持某能力时的明确降级，而非静默更换模型。

业务域只依赖此能力契约，不直接引用某一家 SDK 或模型字段。

## 候选方向

| 方向 | 适用场景 | 主要权衡 |
|---|---|---|
| OpenAI | 结构化输出、广泛 SDK 生态 | 云端发送敏感内容、费用与模型版本变化 |
| Anthropic | 长文本 CV/JD 分析、可解释反馈 | 同样需要云端授权和成本控制 |
| Google Gemini | 长上下文与多模态后续扩展 | Provider 契约差异、输出稳定性需评估 |
| Ollama / 本地模型 | 隐私优先、离线实验 | 本机性能、结构化输出和质量可能不足 |

此表不是选型结论；具体模型必须在设计阶段用同一组匿名化 CV/JD/STAR 样本比较。

## 选型验收方法

1. 建立不含真实个人信息的评估集。
2. 比较事实保真、证据引用、结构化输出通过率、延迟和成本。
3. 对“编造经历/指标”设为一票否决。
4. Provider 失败时保留用户原文和已有版本，可重试或切换 provider。
5. 最终决策记录为 ADR，并明确预算和数据保留条款。
