# API — Roadmap

- 路由：`/roadmap`
- 契约：[`../../../server/roadmap/roadmap-api/delta.md`](../../../server/roadmap/roadmap-api/delta.md) + [`../../../../3.coding/api-contract.md`](../../../../3.coding/api-contract.md)

| 场景 | 调用 |
|---|---|
| 打开 / 切换夹 | `GET /roadmap?folderId=` |
| 文件夹 CRUD | `POST/PATCH/DELETE /roadmap/folders…` |
| Todo | `POST/PATCH/DELETE` + `toggle` |
| Company | `POST/PATCH/DELETE /roadmap/companies…` |
| Document | `POST/PATCH/DELETE /roadmap/documents…` |

**前端自动保存**：字段变更 debounce → `PATCH`；`visibilitychange` / `pagehide` 用 `keepalive` flush。Document 正文以编辑器为权威，保存成功后的 `bodyHtml` **不回灌** TipTap。

**禁止**：generate / templates / recompute / dependencyOverride。
