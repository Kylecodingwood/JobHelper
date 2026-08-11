# API — Roadmap（Notion 待办）

- 路由：`/roadmap`
- 契约：[`../../../server/roadmap/roadmap-api/delta.md`](../../../server/roadmap/roadmap-api/delta.md)

| 场景 | 调用 |
|---|---|
| 打开页 | `GET /roadmap` |
| 新建 | `POST /roadmap/todos` |
| 行内改 name/due/comment | `PATCH /roadmap/todos/{id}` |
| 勾选完成 | `POST /roadmap/todos/{id}/toggle` |
| 删除 | `DELETE /roadmap/todos/{id}` |

**禁止**：generate / templates / recompute / dependencyOverride。
