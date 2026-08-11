# API — CV

- 路由：`/cv`
- 契约：[`../../../server/cv/cv-api/delta.md`](../../../server/cv/cv-api/delta.md)

| 场景 | 调用 |
|---|---|
| 列表 | `GET /cv/documents` |
| 上传 | `POST /cv/documents` multipart `file` |
| 预览 | iframe / 新窗口 `GET /cv/documents/{id}/pdf` |
| 删除 | `DELETE /cv/documents/{id}` |
