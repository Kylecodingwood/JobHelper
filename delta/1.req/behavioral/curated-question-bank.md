# Behavioral 策展题库（种子）

- `curationVersion`: **v1**
- `sourceType`: **CURATED**（全题）
- **来源说明**：常见软件工程/技术岗位行为面试主题归纳；不复制任何商业题库或雇主专有题库原文。
- **关联用例**：UC-BHV-001 加载本文件作为 v1 种子；应用内 `BehavioralQuestion` 由导入/同步产生，`curationVersion=v1`。

## 索引

| # | `questionId`（建议） | `competencyTopic` | 问题正文 |
|---|---|---|---|
| 1 | `CUR-v1-TM-01` | teamwork | Tell me about a time you worked on a cross-functional team where members had different priorities. How did you align everyone toward delivery? |
| 2 | `CUR-v1-TM-02` | teamwork | Describe a situation where you had to rely on a teammate who was struggling to meet their commitments. What did you do? |
| 3 | `CUR-v1-TM-03` | teamwork | Give an example of when you received constructive feedback from a peer during a project. How did you respond? |
| 4 | `CUR-v1-TM-04` | teamwork | Tell me about a time you had to onboard or mentor a new team member while still meeting your own deadlines. |
| 5 | `CUR-v1-LD-01` | leadership | Describe a time you took the lead on a technical initiative without being formally assigned as the lead. |
| 6 | `CUR-v1-LD-02` | leadership | Tell me about a decision you made that affected your team's roadmap or priorities. How did you communicate it? |
| 7 | `CUR-v1-LD-03` | leadership | Give an example of when you delegated work to others. How did you choose who did what and follow up? |
| 8 | `CUR-v1-LD-04` | leadership | Tell me about a time you had to motivate others during a difficult sprint or release cycle. |
| 9 | `CUR-v1-CF-01` | conflict | Describe a disagreement you had with a colleague about a technical approach. How was it resolved? |
| 10 | `CUR-v1-CF-02` | conflict | Tell me about a time you pushed back on a stakeholder request that you believed was unrealistic or risky. |
| 11 | `CUR-v1-CF-03` | conflict | Give an example of when two teammates were in conflict and you helped de-escalate the situation. |
| 12 | `CUR-v1-CF-04` | conflict | Tell me about a time you received feedback you strongly disagreed with. What did you do next? |
| 13 | `CUR-v1-FL-01` | failure | Describe a project or feature that did not go as planned. What happened and what did you learn? |
| 14 | `CUR-v1-FL-02` | failure | Tell me about a mistake you made in production or near-production. How did you detect and fix it? |
| 15 | `CUR-v1-FL-03` | failure | Give an example of when you missed a deadline or commitment. What caused it and how did you handle it? |
| 16 | `CUR-v1-FL-04` | failure | Tell me about a time an experiment or proof-of-concept failed. Why did you pursue it and what changed afterward? |
| 17 | `CUR-v1-CM-01` | communication | Describe a time you had to explain a complex technical topic to a non-technical audience. |
| 18 | `CUR-v1-CM-02` | communication | Tell me about a situation where unclear requirements caused rework. How did you improve clarity? |
| 19 | `CUR-v1-CM-03` | communication | Give an example of written communication (design doc, RFC, status update) that influenced a team decision. |
| 20 | `CUR-v1-CM-04` | communication | Tell me about a time you had to deliver difficult news about delays or scope cuts to stakeholders. |
| 21 | `CUR-v1-OW-01` | ownership | Describe a time you noticed a problem outside your immediate scope and took initiative to fix it. |
| 22 | `CUR-v1-OW-02` | ownership | Tell me about the most ambiguous task you owned end-to-end. How did you define success? |
| 23 | `CUR-v1-OW-03` | ownership | Give an example of when you improved code quality, reliability, or developer experience without being asked. |
| 24 | `CUR-v1-OW-04` | ownership | Tell me about a time you balanced short-term delivery pressure with long-term maintainability. |

## 导入约束（UC-BHV-001）

1. 策展题正文只读；用户可 `HIDDEN` 隐藏，不可原地修改 `CURATED` 正文。
2. 每条导入记录的 `curationVersion` 必须为 `v1`（直至发布 v2 种子文件）。
3. 用户 `USER_DEFINED` 题与本文件独立；见 UC-BHV-001 / UC-BHV-007。

## 主题分布

| `competencyTopic` | 题数 |
|---|---:|
| teamwork | 4 |
| leadership | 4 |
| conflict | 4 |
| failure | 4 |
| communication | 4 |
| ownership | 4 |
| **合计** | **24** |
