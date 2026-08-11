# Behavioral 实体说明

- 状态：已细化（逻辑需求模型，不指定数据库/ORM）
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 用例：[usecase-desc.md](usecase-desc.md)
- 实体图：[entity.puml](entity.puml)

## 1. 聚合与所有权

`BehavioralQuestion` 表达策展题或用户自定义题；v1 策展种子见 [curated-question-bank.md](curated-question-bank.md)（`curationVersion=v1`）。`STAREvidence` 是可独立复用的敏感经历聚合，使用不可变修订保存历史。`BehavioralAnswer` 面向一个问题并拥有不可变 `AnswerVersion`；每个版本可引用零个或多个 Evidence 修订。`AnswerFeedback` 固定绑定一个答案版本并拥有可逐项决策的反馈项。AI 同意由[全局 AI 同意注册表](../ai-consent.md)统一管理；本地反馈规则见 [local-feedback-rules.md](local-feedback-rules.md)。

## 2. 实体定义

### 2.1 BehavioralQuestion

| 属性 | 含义 / 约束 |
|---|---|
| `questionId` | 唯一标识 |
| `text` | 问题正文，非空 |
| `competencyTopic` | 如 teamwork、leadership、conflict、failure 等可配置主题 |
| `sourceType` | `CURATED` / `USER_DEFINED` |
| `curationVersion` | 策展内容版本；自定义题为空 |
| `visibility` | `ACTIVE` / `HIDDEN` |
| `createdAt`、`updatedAt` | 时间 |

**不变量：** 用户不能原地覆盖 `CURATED` 正文；可隐藏。`USER_DEFINED` 可编辑、可单独隐藏或删除（见 UC-BHV-007）；删除带答案的自定义题须预览影响，但不得改变既有答案版本内已固定的 `questionTextSnapshot`。

### 2.2 STAREvidence

**业务含义：** 可在多个答案中复用的一段个人经历聚合。

| 属性 | 含义 / 约束 |
|---|---|
| `evidenceId` | 聚合标识 |
| `title` | 用户可识别标题 |
| `currentRevisionId` | 当前修订 |
| `status` | `DRAFT` / `COMPLETE` |
| `createdAt`、`updatedAt` | 时间 |

### 2.3 STAREvidenceRevision

| 属性 | 含义 / 约束 |
|---|---|
| `revisionId` | 不可变修订标识 |
| `evidenceId` | 所属 Evidence |
| `parentRevisionId` | 父修订，可空 |
| `situation` | 情境 |
| `task` | 任务/责任 |
| `action` | 用户实际行动 |
| `result` | 结果 |
| `competencyTags` | 能力/技能标签 |
| `createdAt` | 创建时间 |

**不变量：**

1. 修订创建后正文不可覆盖。
2. 缺失 STAR 部分可保存为 `DRAFT`，但不得标记 `COMPLETE`。
3. 系统不得自动添加用户未提供的个人事实、职责或指标。

### 2.4 BehavioralAnswer

| 属性 | 含义 / 约束 |
|---|---|
| `answerId` | 聚合标识 |
| `questionId` | 对应问题 |
| `currentVersionId` | 当前答案版本 |
| `status` | `DRAFT` / `READY_FOR_FEEDBACK` / `FEEDBACK_AVAILABLE` / `REVISED` |
| `createdAt`、`updatedAt` | 时间 |

一个问题可有多个独立答案聚合，以支持不同故事或练习方向。

### 2.5 AnswerVersion

| 属性 | 含义 / 约束 |
|---|---|
| `answerVersionId` | 唯一标识 |
| `answerId` | 所属答案 |
| `parentVersionId` | 父版本，可空 |
| `versionNumber` | 答案内递增序号 |
| `questionTextSnapshot` | 保存时的问题快照 |
| `answerText` | 纯文本答案 |
| `revisionSource` | `USER_DRAFT` / `USER_EDIT` / `FEEDBACK_ASSISTED` |
| `createdAt` | 时间 |

**不变量：** 已保存版本不可覆盖；反馈只接受非空版本；v1 不含音频或音频转写属性。

### 2.6 AnswerEvidenceLink

**业务含义：** 答案版本对 Evidence 确切修订的引用。

| 属性 | 含义 / 约束 |
|---|---|
| `answerVersionId` | 答案版本 |
| `evidenceRevisionId` | Evidence 修订 |
| `usageNote` | 可选：该经历在答案中的用途 |

固定修订而非“当前 Evidence”，保证历史可复现。

### 2.7 AnswerFeedback

| 属性 | 含义 / 约束 |
|---|---|
| `feedbackId` | 唯一标识 |
| `answerVersionId` | 固定输入版本 |
| `ruleVersion`、`promptVersion` | 反馈依据版本 |
| `status` | `REQUESTED` / `RUNNING` / `COMPLETED` / `FAILED` / `CANCELLED` |
| `createdAt`、`completedAt` | 时间 |

不得包含或推导面试成功概率字段。

### 2.8 AnswerFeedbackItem

| 属性 | 含义 / 约束 |
|---|---|
| `feedbackItemId` | 唯一标识 |
| `feedbackId` | 所属反馈 |
| `dimension` | `STAR_COMPLETENESS` / `EVIDENCE_SPECIFICITY` |
| `targetQuote`、`targetLocation` | 被反馈的答案片段及定位 |
| `issue` | 缺失部分或泛化问题 |
| `rationale` | 判断依据 |
| `suggestion` | 可执行建议 |
| `origin` | `DETERMINISTIC_RULE` / `AI` |
| `factCheckStatus` | `PASSED` / `FACT_CONFLICT` / `REQUIRES_USER_VERIFICATION` |
| `decision` | `UNDECIDED` / `ACCEPTED` / `REJECTED` / `IGNORED` |
| `decidedAt` | 决定时间 |

**不变量：** 必须引用答案片段；`FACT_CONFLICT` 不能接受；决定仅由用户产生。

### 2.9 AppliedFeedback

| 属性 | 含义 / 约束 |
|---|---|
| `answerVersionId` | 新修订版本 |
| `feedbackItemId` | 用户采纳的反馈项 |

用于追踪 `FEEDBACK_ASSISTED` 版本实际采纳的反馈，不表示系统自动改文。

### 2.10 AIConsent（全局注册表投影）

对[全局 AI 同意注册表](../ai-consent.md)的域内读/写投影。本域相关类别：`ANSWER_TEXT`、`STAR_EVIDENCE`、`QUESTION_TEXT`。

| 属性 | 含义 / 约束 |
|---|---|
| `consentId` | 与全局注册表记录一致 |
| `dataCategory` | `ANSWER_TEXT`、`STAR_EVIDENCE`、`QUESTION_TEXT` 等 |
| `scopeVersion` | 同意说明版本 |
| `status` | `GRANTED` / `REVOKED` |
| `grantedAt`、`revokedAt` | 时间 |

按类别独立授权；撤销即时阻止后续相关调用。管理入口见 UC-BHV-006。

### 2.11 AIInvocation

| 属性 | 含义 / 约束 |
|---|---|
| `invocationId` | 唯一标识 |
| `feedbackId` | 所属反馈 |
| `provider`、`model` | 实际提供方与模型 |
| `inputVersionRefs` | 答案、Evidence、问题输入版本 |
| `promptVersion` | 提示版本 |
| `inputDigest`、`cacheKey` | 完整性和缓存 |
| `cacheStatus` | `MISS` / `HIT` |
| `costMetadata` | 成本/用量；不可得时 `UNKNOWN` |
| `outputRef` | 输出内容引用 |
| `status` | `PENDING` / `SUCCEEDED` / `FAILED` / `CANCELLED` |
| `startedAt`、`finishedAt` | 时间 |

普通日志不得记录完整答案、Evidence、提示或输出。

### 2.12 SensitiveDeletionAudit

| 属性 | 含义 / 约束 |
|---|---|
| `auditId` | 唯一标识 |
| `eventType` | `ANSWER_HARD_DELETED` / `STAR_EVIDENCE_HARD_DELETED` |
| `opaqueObjectToken` | 不可逆匿名标识 |
| `deletedCounts` | 对象数量摘要 |
| `result` | `SUCCEEDED` / `PARTIAL_FAILED` |
| `occurredAt` | 时间 |

不得包含题目回答、STAR 内容、提示、AI 输出或任何可还原内容。

## 3. 关系与基数

- `BehavioralQuestion 1 -- 0..* BehavioralAnswer`。
- `STAREvidence 1 -- 1..* STAREvidenceRevision`。
- `BehavioralAnswer 1 -- 1..* AnswerVersion`。
- `AnswerVersion * -- * STAREvidenceRevision`，由 `AnswerEvidenceLink` 固定引用。
- `AnswerVersion 1 -- 0..* AnswerFeedback`。
- `AnswerFeedback 1 -- 0..* AnswerFeedbackItem`。
- `AnswerVersion * -- * AnswerFeedbackItem`，由 `AppliedFeedback` 表示用户实际采纳。
- `AnswerFeedback 1 -- 0..* AIInvocation`；重试和缓存命中均可追踪。

## 4. 删除与保留

1. 所有内容默认保留到用户手动删除，不设自动 TTL。
2. 删除 Answer：硬删除全部版本、Evidence 引用关系、反馈、反馈项、采纳关系、相关 AIInvocation 和缓存内容。
3. **删除 STAR Evidence（硬删除）**：硬删除全部 Evidence 修订；移除所有 `AnswerEvidenceLink`；**保留**关联 `AnswerVersion.answerText` 与 `questionTextSnapshot`。凡依赖被删 Evidence 修订的 `AnswerFeedback`、`AnswerFeedbackItem`、`AIInvocation` 及其缓存/输出须删除或标记失效。受影响 `BehavioralAnswer` 状态标记为需复核（如 `REVISED` 或等价“需重新链接 Evidence/重新获取反馈”）。
4. 删除 `USER_DEFINED` 问题：可独立于 Answer/Evidence 隐藏或删除；若仍有关联 Answer，须预览影响；历史答案保留其问题快照，不随题文删除而丢失。
5. 相关 Action 移除或标记源已删除，不得复制保留敏感正文。
6. 删除后仅保留无内容 `SensitiveDeletionAudit`。

## 5. 实体级验收

- 可从答案版本还原当时的问题快照与 Evidence 修订引用。
- 可从反馈项追溯到确切答案版本、维度、引用片段和 AI/规则来源。
- 模型中没有成功概率和音频字段。
- 撤销数据类别同意后无法创建相关外部调用。
- 级联删除后除无内容审计外无法恢复答案、Evidence、反馈或 AI 内容。
