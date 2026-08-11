# CV 实体说明

- 状态：已细化（逻辑需求模型，不指定数据库/ORM）
- 权威需求：[../../SRS/SRS.md](../../SRS/SRS.md)
- 用例：[usecase-desc.md](usecase-desc.md)
- 实体图：[entity.puml](entity.puml)

## 1. 聚合与所有权

`CVDocument` 是 CV 内容聚合根，拥有原文件及其全部 `CVVersion`。`CVReview` 固定引用一个输入版本，可选引用一个 Job/JD 快照，并拥有 `CVSuggestion`。AI 同意由[全局 AI 同意注册表](../ai-consent.md)按数据类别统一管理；本域 `AIConsent` 为对该注册表的投影/引用。AI 调用与缓存必须能追溯到输入和输出，但在删除所属敏感对象时级联硬删除内容。本地评审规则见 [local-review-rules.md](local-review-rules.md)，AI 关闭或无同意时仍须可用。

## 2. 实体定义

### 2.1 CVDocument

**业务含义：** 一次用户上传的 CV 原始文档及其版本树。

| 属性 | 含义 / 约束 |
|---|---|
| `documentId` | 稳定唯一标识 |
| `originalFileRef` | 本地原文件引用；内容不可被派生版本覆盖 |
| `originalFileName` | 展示用途；日志与删除审计不得记录 |
| `mediaType` | `APPLICATION_PDF` / `DOCX` |
| `fileHash` | 完整性和重复提示依据，不自动合并 |
| `createdAt` | 上传成功时间 |

**生命周期：** 上传成功创建；无自动过期；仅 UC-CV-008 手动级联硬删除。

### 2.2 CVVersion

**业务含义：** CV 文本的不可变快照，包括初始提取/确认版本及根据接受建议形成的派生结构化文本版本。

| 属性 | 含义 / 约束 |
|---|---|
| `versionId` | 唯一标识 |
| `documentId` | 所属 `CVDocument` |
| `parentVersionId` | 派生版本的直接父版本；初始版本为空 |
| `versionNumber` | 文档内可读序号；唯一且只增不改 |
| `extractedText` | 解析器原始提取文本，可为空 |
| `confirmedText` | 用户确认或派生的不可变结构化文本 |
| `extractionStatus` | `SUCCEEDED` / `INCOMPLETE_SUSPECTED` / `FAILED` |
| `versionStatus` | `EXTRACTION_FAILED` / `CONFIRMATION_REQUIRED` / `CONFIRMED` |
| `textSource` | `EXTRACTED_CONFIRMED` / `USER_CORRECTED` / `USER_ENTERED` / `SUGGESTION_DERIVED` |
| `extractionDiagnostic` | 不含不必要正文的诊断摘要 |
| `confirmedAt`、`createdAt` | 审计时间 |

**不变量：**

1. `CONFIRMED` 后的 `confirmedText` 不可原地修改；修改必须创建新版本。
2. `SUGGESTION_DERIVED` 必须有父版本和至少一个 `AppliedSuggestion`。
3. 未确认版本不得作为评审输入。
4. 原提取文本与用户确认文本必须可区分。

### 2.3 CVReview

**业务含义：** 对确定 CV 输入版本的一次通用或岗位定制评审。

| 属性 | 含义 / 约束 |
|---|---|
| `reviewId` | 唯一标识 |
| `inputVersionId` | 不可变输入版本 |
| `reviewType` | `GENERIC_HEALTH` / `TAILORED_TO_JOB` |
| `jobId` | 定制评审的 Job 引用；通用评审为空 |
| `jobDescriptionSnapshotRef` | 定制评审使用的 JD 快照引用/摘要标识 |
| `ruleVersion`、`promptVersion` | 可解释与复现依据 |
| `status` | `REQUESTED` / `RUNNING` / `COMPLETED` / `FAILED` / `CANCELLED` |
| `createdAt`、`completedAt` | 时间 |

**不变量：**

- `TAILORED_TO_JOB` 必须同时具有 `jobId` 和固定 JD 快照。
- `GENERIC_HEALTH` 不依赖 Job。
- 评审不得修改输入版本或 Job 决定。

### 2.4 CVSuggestion

**业务含义：** 可独立决策的一条评审建议。

| 属性 | 含义 / 约束 |
|---|---|
| `suggestionId` | 唯一标识 |
| `reviewId` | 所属评审 |
| `targetText`、`targetLocation` | 被建议修改的原文及稳定定位 |
| `issue` | 问题说明 |
| `rationale` | 判断依据；定制评审还应含 JD 依据 |
| `proposedText` | 建议文本 |
| `origin` | `DETERMINISTIC_RULE` / `AI` |
| `factCheckStatus` | `PASSED` / `FACT_CONFLICT` / `REQUIRES_USER_VERIFICATION` |
| `decision` | `UNDECIDED` / `ACCEPTED` / `REJECTED` / `DEFERRED` |
| `decisionReason`、`decidedAt` | 可选原因与时间 |

**不变量：**

- 目标原文、问题、依据和建议文本缺一不可。
- `FACT_CONFLICT` 不得转为 `ACCEPTED`。
- 决定只能来自用户操作。

### 2.5 AppliedSuggestion

**业务含义：** 派生版本与实际应用建议之间的不可变追踪关系。

| 属性 | 含义 / 约束 |
|---|---|
| `versionId` | 派生 CV 版本 |
| `suggestionId` | 应用的建议 |
| `appliedOrder` | 确定性应用顺序 |

仅 `ACCEPTED` 且事实检查未冲突的建议可建立此关系。

### 2.6 AIConsent（全局注册表投影）

**业务含义：** 对[全局 AI 同意注册表](../ai-consent.md)的域内读/写投影；CV 域不持有独立长期 consent 副本。本域相关类别：`CV_TEXT`、`JOB_JD`、`PROFILE`。

| 属性 | 含义 / 约束 |
|---|---|
| `consentId` | 与全局注册表记录一致 |
| `dataCategory` | `CV_TEXT`、`JOB_JD`、`PROFILE` 等 |
| `scopeVersion` | 同意说明版本 |
| `status` | `GRANTED` / `REVOKED` |
| `grantedAt`、`revokedAt` | 时间 |

同意某类别不隐含同意其他类别；撤销只阻止后续调用，不复活或改写历史。管理入口见 UC-CV-007。

### 2.7 AIInvocation

**业务含义：** 一次实际 provider 调用或缓存命中的完整追踪记录。

| 属性 | 含义 / 约束 |
|---|---|
| `invocationId` | 唯一标识 |
| `reviewId` | 所属评审 |
| `provider`、`model` | 实际提供方与模型 |
| `inputVersionRefs` | CV/JD/Profile 等输入版本引用 |
| `promptVersion` | 提示版本 |
| `inputDigest`、`cacheKey` | 缓存与完整性用途 |
| `cacheStatus` | `MISS` / `HIT` |
| `costMetadata` | provider 返回的成本/用量；不可得时为 `UNKNOWN` |
| `outputRef` | 输出内容引用 |
| `status` | `PENDING` / `SUCCEEDED` / `FAILED` / `CANCELLED` |
| `startedAt`、`finishedAt` | 时间 |

不得在普通日志中记录完整输入、提示或输出。

### 2.8 SensitiveDeletionAudit

**业务含义：** 敏感对象硬删除后唯一允许保留的无内容审计事件。

| 属性 | 含义 / 约束 |
|---|---|
| `auditId` | 唯一标识 |
| `eventType` | `CV_DOCUMENT_HARD_DELETED` |
| `opaqueObjectToken` | 不可逆匿名标识 |
| `deletedCounts` | 各对象类型数量，不含正文 |
| `result` | `SUCCEEDED` / `PARTIAL_FAILED` |
| `occurredAt` | 时间 |

不得包含文件名、CV 文本、建议、提示、AI 输出或可还原个人内容。

## 3. 关系与基数

- `CVDocument 1 -- 1..* CVVersion`：一个文档至少有一个初始版本。
- `CVVersion 0..1 -- 0..* CVVersion`：父子版本树。
- `CVVersion 1 -- 0..* CVReview`：同一版本可多次、按不同类型评审。
- `CVReview 1 -- 0..* CVSuggestion`：失败评审可没有建议。
- `CVVersion * -- * CVSuggestion`：通过 `AppliedSuggestion` 表达实际应用。
- `CVReview 1 -- 0..* AIInvocation`：重试和缓存命中均形成调用追踪。
- 全局 `AIConsent`（本域投影）按数据类别约束 `AIInvocation` 的可执行性；见 [ai-consent.md](../ai-consent.md)。

## 4. 删除与保留

1. 默认无限期保留，直到用户手动删除，不实现自动 TTL。
2. 删除 `CVDocument` 必须硬删除原文件、全部文本/版本、评审、建议、应用关系，以及以其内容为输入/输出的 AIInvocation 与缓存内容。
3. Job、Action 等跨域引用必须移除或标记源已删除，不得保留 CV 正文副本。
4. 删除后只保留 `SensitiveDeletionAudit`，且不得包含内容。

## 5. 实体级验收

- 可从任意派生版本追溯至父版本和实际应用建议。
- 可从任意建议追溯至确切输入 CV 版本、评审类型及规则/AI 调用。
- 撤销 `CV_TEXT` 同意后，新的含 CV 文本外部调用无法创建。
- 级联删除后，除无内容审计事件外不存在任何可恢复 CV 内容。
