# roadmap-model（Pivot）

## `roadmap_todo`

| 列 | 类型 | 说明 |
|---|---|---|
| todo_id | UUID PK | |
| name | VARCHAR(512) NOT NULL | |
| due_at | TIMESTAMPTZ | 可空 |
| comment | TEXT | 可空 |
| done | BOOLEAN NOT NULL DEFAULT FALSE | checkbox |
| sort_order | INT NOT NULL DEFAULT 0 | |
| created_at / updated_at | TIMESTAMPTZ | |

无 Roadmap 聚合容器亦可：单用户全局 todos 表即可。  
**删除概念**：template、template_version、task_dependency、generation_record、GRS 锚点、SYSTEM_INCOMPLETE_ONLY merge。
