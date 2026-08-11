# cv-api（文件管理切片）

CV 域本切片**只做简历文件管理**：DOCX 上传 → 转存 PDF → 列表 / 预览 / 删除。  
不做文本提取、不做 Review、不做 Job 关联（后续 AI 解析围绕 PDF）。

## 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/cv/documents` | 文件列表 |
| POST | `/api/v1/cv/documents` | `multipart/form-data` 上传 DOCX |
| GET | `/api/v1/cv/documents/{documentId}` | 元数据 |
| GET | `/api/v1/cv/documents/{documentId}/pdf` | 预览/下载 PDF（`application/pdf`） |
| DELETE | `/api/v1/cv/documents/{documentId}` | 删除原件+PDF → 204 |

### POST 上传

- Content-Type: `multipart/form-data`
- 字段：`file`（必填，`.docx`）
- 可选：`displayName`

**201**

```json
{
  "documentId": "uuid",
  "displayName": "Kyle-CV.docx",
  "originalFilename": "Kyle-CV.docx",
  "contentTypeOriginal": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "pdfReady": true,
  "sizeBytesOriginal": 120000,
  "sizeBytesPdf": 98000,
  "createdAt": "2026-08-09T12:00:00Z"
}
```

### GET 列表

```json
{ "items": [ /* CvDocumentDto */ ] }
```

## 错误

| code | HTTP |
|---|---|
| `CV_INVALID_TYPE` | 400 非 DOCX |
| `CV_CONVERT_FAILED` | 422 DOCX→PDF 失败 |
| `CV_NOT_FOUND` | 404 |
| `CV_TOO_LARGE` | 400（默认 10MB） |

## 非目标（本切片）

文本提取 API/Review UI、健康检查、Job 关联、建议决策、AI 解析（仅预留后续读 PDF）。

## 实现备注（M1）

DOCX→PDF 使用 POI 读 body + PDFBox 写预览 PDF（可靠、无 LibreOffice 依赖）。原件 DOCX 仍落盘；不提供抽取文本对外 API。
