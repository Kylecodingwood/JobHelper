# roadmap-api（Pivot）

独立 Notion 式待办。无模板 / 生成 / 重算 / 依赖。

## 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/roadmap` | `{ "todos": [ TodoDto ] }` |
| POST | `/api/v1/roadmap/todos` | 创建 |
| PATCH | `/api/v1/roadmap/todos/{todoId}` | 更新字段 |
| DELETE | `/api/v1/roadmap/todos/{todoId}` | 删除 → 204 |
| POST | `/api/v1/roadmap/todos/{todoId}/toggle` | checkbox 完成翻转 |

### TodoDto

```json
{
  "todoId": "uuid",
  "name": "搭好 LinkedIn (ireland)",
  "dueAt": "2026-09-01T00:00:00Z",
  "comment": "experience; skills; github",
  "done": false,
  "sortOrder": 0,
  "updatedAt": "2026-08-09T10:00:00Z"
}
```

### POST body

```json
{ "name": "CV (ireland)", "dueAt": null, "comment": null }
```

### PATCH body

```json
{ "name": "…", "dueAt": "…", "comment": "…", "done": true }
```

## 错误

| code | HTTP |
|---|---|
| `TODO_NOT_FOUND` | 404 |
| `VALIDATION_ERROR` | 400（name 空） |

## 废止

`generate/*`、`recompute/*`、`templates/*`、`template-updates/*`、`tasks/*`、`actionability`
