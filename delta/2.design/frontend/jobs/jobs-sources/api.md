# API — Job Sources

- 路由：`/jobs/sources`
- 契约：全局 searchTerms（Pivot B：一份词打到所有源）

| 场景 | 调用 |
|---|---|
| 列表 | `GET /job-sources`（各项 `searchTerms` 同为全局词） |
| 读搜索词 | `GET /job-sources/search-terms` |
| 保存搜索词 | `PUT /job-sources/search-terms` `{ "searchTerms": ["…"] }` |
| 启停源 | `PATCH /job-sources/{id}` `{ "enabled": true\|false }` |
| 手动同步 | `POST /job-source-runs` `{ "sourceId" }` |
| 空词首次同步 | **400 `SEARCH_TERMS_REQUIRED`** → 提示先填全局搜索框 |

大搜索框内容即**全局** `searchTerms`（逗号/换行拆成数组）；FreeHire 与 JobSpy 同步均使用同一份。
