# API — Behavioral / Interview

- 路由：`/behavioral`（导航名 Interview / Behavioral）
- 契约：[`../../../server/behavioral/behavioral-api/delta.md`](../../../server/behavioral/behavioral-api/delta.md)

| 场景 | 调用 |
|---|---|
| 题库 | `GET /behavioral/questions` |
| 自定义题 | `POST /behavioral/questions` |
| Evidence CRUD | `/behavioral/evidence` |
| 写答案 | `POST /behavioral/answers` + versions |
| 本地反馈 | `POST .../feedback` |
| AI | 不调用；若误触 → 501 |
