# roadmap-model（Pivot 2026-08）

多文件夹；内容按 `kind` 分表。Flyway：`V13` folders、`V14` kind+company、`V17` document。

## `roadmap_folder`

| 列 | 类型 | 说明 |
|---|---|---|
| folder_id | UUID PK | |
| name | VARCHAR(255) NOT NULL | |
| kind | VARCHAR | `todolist` \| `companytracker` \| `document` |
| sort_order | INT | |
| created_at / updated_at | TIMESTAMPTZ | |

## `roadmap_todo`

| 列 | 类型 | 说明 |
|---|---|---|
| todo_id | UUID PK | |
| folder_id | UUID FK → folder ON DELETE CASCADE | |
| name | VARCHAR(512) NOT NULL | |
| due_at | TIMESTAMPTZ | 可空 |
| comment | TEXT | 可空 |
| done | BOOLEAN NOT NULL DEFAULT FALSE | |
| sort_order | INT | 创建时序号；列表展示不按此列 |
| created_at / updated_at | TIMESTAMPTZ | 无 due 时按 `created_at` 降序 |

列表顺序见 [`../roadmap-api/delta.md`](../roadmap-api/delta.md) 与 [`../../../../3.coding/api-contract.md`](../../../../3.coding/api-contract.md)。

## `roadmap_company`

| 列 | 类型 | 说明 |
|---|---|---|
| company_id | UUID PK | |
| folder_id | UUID FK CASCADE | |
| company_name | VARCHAR NOT NULL | |
| status | VARCHAR NOT NULL | watching/applied/interview/offer/rejected/on_hold |
| contact / note | TEXT | 可空 |
| sort_order | INT | |
| created_at / updated_at | TIMESTAMPTZ | |

## `roadmap_document`

| 列 | 类型 | 说明 |
|---|---|---|
| document_id | UUID PK | |
| folder_id | UUID FK CASCADE | |
| title | VARCHAR(512) NOT NULL | |
| body_html | TEXT | TipTap HTML |
| sort_order | INT | |
| created_at / updated_at | TIMESTAMPTZ | |

**删除概念**：template、template_version、task_dependency、generation_record、GRS/STAMP 锚点。
