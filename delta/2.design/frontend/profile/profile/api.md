# API — Profile（画像）

- 路由：`/profile`
- 契约：[`../../../server/profile/profile-api/delta.md`](../../../server/profile/profile-api/delta.md)

| 场景 | 调用 |
|---|---|
| 打开 / 保存 | `GET` / `PUT /profile` |
| 校验 | `POST /validate`（可选） |
| 字段用途 | `GET /field-usage` |
| 备份 | `/profile/backups/*` |

**禁止**：Target Roles 编辑；recompute-requests；把角色当搜索词展示。
