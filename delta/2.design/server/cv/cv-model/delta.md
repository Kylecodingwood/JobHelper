# cv-model（文件管理）

## `cv_document`

| 列 | 类型 | 说明 |
|---|---|---|
| document_id | UUID PK | |
| display_name | VARCHAR(512) | |
| original_filename | VARCHAR(512) | |
| original_path | VARCHAR(1024) | 本地 DOCX 路径 |
| pdf_path | VARCHAR(1024) | 本地 PDF 路径（主预览对象） |
| content_type_original | VARCHAR(128) | |
| size_bytes_original | BIGINT | |
| size_bytes_pdf | BIGINT | |
| pdf_ready | BOOLEAN | |
| created_at / updated_at | TIMESTAMPTZ | |

存储根目录：`./data/cv/`（可配 `jobhelper.cv.dir`）。  
不变量：业务预览与后续 AI **只读 PDF**；DOCX 为上传源备份。
