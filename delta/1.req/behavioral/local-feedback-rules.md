# Behavioral 本地反馈规则（最小集）

- 状态：已冻结（v1 确定性规则）
- 关联用例：UC-BHV-004
- 规则版本字段：`ruleVersion`（如 `bhv-local-v1`）

## 1. 适用范围

在 AI 关闭、无对应类别同意、或 provider 不可用时，系统**必须**仍运行以下本地检查并产出 `AnswerFeedbackItem`（`origin=DETERMINISTIC_RULE`）：

- `STAR_COMPLETENESS`
- `EVIDENCE_SPECIFICITY`

## 2. STAR 完整性（STAR_COMPLETENESS）

### 2.1 基于链接 Evidence 修订

对答案版本引用的每个 `STAREvidenceRevision`：

| 规则 ID | 检查项 | 触发条件 |
|---|---|---|
| `BHV-L-S01` | Situation 缺失 | `situation` 为空或仅空白 |
| `BHV-L-S02` | Task 缺失 | `task` 为空 |
| `BHV-L-S03` | Action 缺失 | `action` 为空 |
| `BHV-L-S04` | Result 缺失 | `result` 为空 |

反馈须引用答案中与该 Evidence 相关的片段或说明“答案引用了不完整 Evidence”。

### 2.2 基于答案正文启发式

未引用 Evidence 或正文未覆盖 STAR 四要素时：

| 规则 ID | 检查项 | 触发条件（轻量启发式） |
|---|---|---|
| `BHV-L-S10` | 无情境 | 答案过短（如 &lt; 80 词）且无时间/背景 cues |
| `BHV-L-S11` | 无行动 | 全文缺少第一人称行动动词模式 |
| `BHV-L-S12` | 无结果 | 无结果/影响相关词且 Evidence Result 为空 |

不得因启发式失败而自动补写 STAR 内容。

## 3. 证据具体性（EVIDENCE_SPECIFICITY）

| 规则 ID | 检查项 | 触发条件 |
|---|---|---|
| `BHV-L-E01` | 模糊词 | 命中词表：`significantly`、`greatly`、`improved`、`better`、`successful`、`helped`、`various` 等且同句无数字/比例/时间盒 |
| `BHV-L-E02` | 缺少数值 | 声称提升/降低/增长但未出现 `%`、数量、倍数或明确指标名 |
| `BHV-L-E03` | 缺少时间盒 | 描述持续改进但无 `week/month/quarter` 或具体日期范围 |
| `BHV-L-E04` | 职责堆砌 | 连续多句仅列职责、无“我做了什么”的行动细节 |

每项须含 `targetQuote`、`issue`、`rationale`、`suggestion`（引导用户补充可验证细节，不虚构数字）。

## 4. 与 AI 的关系

- 本地规则不输出成功概率或总分。
- AI 可在本地结果之上追加更细建议；无 AI 时 UC-BHV-004 仍应完成本地维度检查。
- 可选增强：Cursor Skill / SDK，见 [ai-provider-directions.md](../ai-provider-directions.md)。

## 5. 验收要点

- 无 Result 的答案至少产生一条 `STAR_COMPLETENESS` 本地项。
- 含“显著提升效率”类表述至少产生一条 `EVIDENCE_SPECIFICITY` 本地项。
- 所有本地项 `origin=DETERMINISTIC_RULE`。
