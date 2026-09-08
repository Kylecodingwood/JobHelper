# roadmap-api（Pivot 2026-08）

多文件夹 + 三类内容。权威契约：[`../../../../3.coding/api-contract.md`](../../../../3.coding/api-contract.md) Roadmap 节。

## 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/roadmap` | `?folderId=` → `{ folderId, kind, folders[], todos[], companies[], documents[] }`（按 kind 只填对应数组） |
| GET | `/api/v1/roadmap/folders` | `{ folders[] }` |
| POST | `/api/v1/roadmap/folders` | `{ name, kind? }` → 201；默认 kind=`todolist` |
| PATCH | `/api/v1/roadmap/folders/{folderId}` | `{ name }`；**不可改 kind** |
| DELETE | `/api/v1/roadmap/folders/{folderId}` | 204；不可删最后一个；级联内容 |
| POST/PATCH/DELETE/toggle | `/api/v1/roadmap/todos…` | 仅 todolist 夹 |
| POST/PATCH/DELETE | `/api/v1/roadmap/companies…` | 仅 companytracker 夹 |
| POST/PATCH/DELETE | `/api/v1/roadmap/documents…` | 仅 document 夹 |

## 校验

| code | HTTP | 何时 |
|---|---|---|
| `FOLDER_KIND` | 400 | 在错误 kind 的夹上写错资源 |
| `VALIDATION_ERROR` | 400 | name/title 空等 |
| `TODO_NOT_FOUND` / `DOCUMENT_NOT_FOUND` / … | 404 | |

## 废止

`generate/*`、`recompute/*`、`templates/*`、`template-updates/*`、`tasks/*`、`actionability`
