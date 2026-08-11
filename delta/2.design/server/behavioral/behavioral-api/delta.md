# behavioral-api（完整 A；AI 仅接口）

对齐 UC-BHV-001～005（本地反馈）；AI 调用 **只定义 Port/接口，默认不实现**。

## Questions

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/behavioral/questions?topic=&sourceType=&visibility=ACTIVE` |
| POST | `/api/v1/behavioral/questions` | 自定义题 `{ text, competencyTopic }` |
| PATCH | `/api/v1/behavioral/questions/{id}` | 自定义可改 text/topic；策展仅 `visibility` |
| DELETE | `/api/v1/behavioral/questions/{id}` | 仅 USER_DEFINED → 204 |

## STAR Evidence

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/behavioral/evidence` |
| POST | `/api/v1/behavioral/evidence` | `{ title, situation, task, action, result, competencyTags[] }` |
| PUT | `/api/v1/behavioral/evidence/{id}` | 新建修订（不可原地覆盖） |
| DELETE | `/api/v1/behavioral/evidence/{id}` | 204 |

## Answers

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/behavioral/answers?questionId=` |
| POST | `/api/v1/behavioral/answers` | `{ questionId, bodyText, evidenceIds? }` 创建答案+首版 |
| POST | `/api/v1/behavioral/answers/{id}/versions` | 新版本 `{ bodyText, evidenceIds?, parentVersionId? }` |
| GET | `/api/v1/behavioral/answers/{id}` | 含当前版本 |

## Feedback（本地）

| 方法 | 路径 |
|---|---|
| POST | `/api/v1/behavioral/answers/{id}/versions/{versionId}/feedback` | 跑本地规则 `bhv-local-v1` |
| GET | `/api/v1/behavioral/feedback/{feedbackId}` | |
| POST | `/api/v1/behavioral/feedback/{feedbackId}/items/{itemId}/decision` | `{ decision: ACCEPT\|REJECT\|DEFER }` |

## AI Port（不实现）

```text
BehavioralAiFeedbackPort.enhance(answerVersionId, consentToken) → Optional<AiFeedbackBundle>
```

默认 NoOp / 未启用；无全局 Consent 时不得调用。HTTP 可选 stub：`POST .../feedback/ai` → **501 `AI_NOT_ENABLED`**。

## 错误

`QUESTION_NOT_FOUND`、`EVIDENCE_NOT_FOUND`、`ANSWER_NOT_FOUND`、`CURATED_IMMUTABLE`、`AI_NOT_ENABLED`
