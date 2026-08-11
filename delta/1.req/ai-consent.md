# 全局 AI 同意注册表

- 状态：已冻结（跨域需求）
- 权威需求：[../SRS/SRS.md](../SRS/SRS.md) 第 10 节
- 适用域：Profile、CV、Behavioral、Roadmap（可选建议）

## 1. 设计原则

**单一注册表**：全应用仅维护一份按敏感数据类别索引的 AI 同意记录；各业务域不各自持有长期独立的同意所有权。

**按类别授权与撤销**：用户对某一 `dataCategory` 的同意可复用、可单独撤销；撤销只阻止后续外部调用，不改写历史输出。

1. **域内投影**：CV、Behavioral 等域文档中的 `AIConsent` 实体表示对全局注册表的**读/写投影或引用**（同一 `dataCategory` + `scopeVersion` 语义），而非域内副本。
2. **本地优先**：未配置 provider、AI 关闭、或对应类别未授权时，依赖本地确定性规则的能力仍必须可用（见各域 `local-*-rules.md`）。

## 2. 数据类别


| `dataCategory`  | 典型内容               | 主要消费域              |
| --------------- | ------------------ | ------------------ |
| `CV_TEXT`       | 已确认 CV 结构化/纯文本     | CV                 |
| `JOB_JD`        | Job 描述/JD 快照       | CV（岗位定制评审）、Job     |
| `PROFILE`       | 教育、许可、目标、技能与经历字段   | Profile、CV、Roadmap |
| `ANSWER_TEXT`   | Behavioral 答案版本纯文本 | Behavioral         |
| `STAR_EVIDENCE` | STAR 四部分及标签        | Behavioral         |
| `QUESTION_TEXT` | 策展题或自定义题正文         | Behavioral         |


同意某一类别**不隐含**同意其他类别。每次外部调用仍须展示实际发送范围并允许用户取消。

## 3. 注册表记录（逻辑模型）


| 属性             | 含义 / 约束               |
| -------------- | --------------------- |
| `consentId`    | 稳定唯一标识                |
| `dataCategory` | 上表六类之一                |
| `scopeVersion` | 同意说明文案版本；变更后需重新授权     |
| `status`       | `GRANTED` / `REVOKED` |
| `grantedAt`    | 授予时间                  |
| `revokedAt`    | 撤销时间；未撤销为空            |


**不变量：**

- 每个 `dataCategory` 在任意时刻最多一条有效 `GRANTED` 记录（或等价的状态机表达）。
- 撤销后，新的含该类别的 `AIInvocation` 不得创建；历史调用审计保留至相关敏感对象被删除。
- 域内 UC（如 UC-CV-007、UC-BHV-006）通过全局注册表读写同意，不在域聚合根下嵌套长期 consent 副本。



## 4. 与 AIInvocation 的关系

- `AIInvocation` 仍归属发起调用的业务域（如 `CVReview`、`AnswerFeedback`），但执行前必须查询全局注册表中相关 `dataCategory` 均为 `GRANTED`。
- 缓存键与输入摘要不得绕过同意检查。



## 5. 域文档引用


| 域          | 同意相关 UC                       | 本地规则（AI 关闭/无同意时）                                                         |
| ---------- | ----------------------------- | ------------------------------------------------------------------------ |
| CV         | UC-CV-007                     | [cv/local-review-rules.md](cv/local-review-rules.md)                     |
| Behavioral | UC-BHV-006                    | [behavioral/local-feedback-rules.md](behavioral/local-feedback-rules.md) |
| Profile    | UC-PRO-004（用途说明，非 consent 本身） | —                                                                        |


可选增强路径见 [ai-provider-directions.md](ai-provider-directions.md)（Cursor Skill / 后续 Cursor SDK）。