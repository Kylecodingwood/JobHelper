# API 契约冻结（Pivot 2026-08-09 · 增补 2026-08-25）

| 项 | 内容 |
|---|---|
| 冻结日 | 2026-08-09；增补 CV / Behavioral；**再增补** Roadmap 多夹（Company/Document）、Home `today-priority-v1`、LeetCode Hot 100 |
| 真相源 | 本文件 + 下列 `2.design` api/model |
| 前版 | 系统模板 / GRS / Profile→Roadmap 重算 / TargetRole→searchTerms **已废止** |

## 对齐规则

1. Roadmap：**多文件夹**；`kind` ∈ todolist / companytracker / document；内容分表 CRUD；前端防抖自动保存  
2. Document：`bodyHtml` 为 TipTap HTML；**保存回写不得重置编辑器**（仅切换 `documentId` 时 `setContent`）  
3. Profile：**用户画像**；无 TargetRole；身份含原 Work Auth  
4. Sources：搜索词**仅**用户在 Source 上填写；首次同步前必填  
5. Home：`GET /home` 默认 `actionLimit=3`；进入时刷新 **今日优先三槽**（`today-priority-v1`）  
6. 资源名：`/job-sources`、`/job-source-runs`；Roadmap 单数 `/roadmap`  
7. **CV**：PDF / DOCX 原样上传；无抽文本产品能力  
8. **Behavioral**：题库 + Evidence + 本地反馈；AI Port → `AI_NOT_ENABLED`  
9. **LeetCode**：独立域 `/leetcode`；Hot 100 + review；题面 GraphQL 缓存 

---

## Roadmap

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/roadmap` | `?folderId=` → `{ folderId, kind, folders[], todos[], companies[], documents[] }` |
| GET | `/api/v1/roadmap/folders` | `{ folders[] }` |
| POST | `/api/v1/roadmap/folders` | `{ name, kind? }` → 201；`kind`: `todolist`（默认）\|`companytracker`\|`document` |
| PATCH | `/api/v1/roadmap/folders/{folderId}` | `{ name }` 重命名（不可改 kind） |
| DELETE | `/api/v1/roadmap/folders/{folderId}` | 204；不可删最后一个；级联删内容 |
| POST | `/api/v1/roadmap/todos` | `{ name, folderId?, dueAt?, comment? }` → 201（仅 todolist） |
| PATCH | `/api/v1/roadmap/todos/{todoId}` | 改 name/dueAt/comment/done/folderId |
| DELETE | `/api/v1/roadmap/todos/{todoId}` | 204 |
| POST | `/api/v1/roadmap/todos/{todoId}/toggle` | checkbox 翻转 done |
| POST | `/api/v1/roadmap/companies` | `{ companyName, folderId?, status?, contact?, note? }` → 201（仅 companytracker） |
| PATCH | `/api/v1/roadmap/companies/{companyId}` | 改 companyName/status/contact/note/folderId |
| DELETE | `/api/v1/roadmap/companies/{companyId}` | 204 |
| POST | `/api/v1/roadmap/documents` | `{ title?, folderId?, bodyHtml? }` → 201（仅 document） |
| PATCH | `/api/v1/roadmap/documents/{documentId}` | `{ title?, bodyHtml? }` |
| DELETE | `/api/v1/roadmap/documents/{documentId}` | 204 |

**FolderDto**：`folderId`, `name`, `kind`, `itemCount`, `sortOrder`, `updatedAt`

**TodoDto**：`todoId`, `folderId`, `name`, `dueAt`, `comment`, `done`, `sortOrder`, `updatedAt`

**CompanyDto**：`companyId`, `folderId`, `companyName`, `status`, `contact`, `note`, `sortOrder`, `updatedAt`

**Company status**：`watching` \| `applied` \| `interview` \| `offer` \| `rejected` \| `on_hold`

**DocumentDto**：`documentId`, `folderId`, `title`, `bodyHtml`（所见即所得 HTML，TEXT）, `sortOrder`, `createdAt`, `updatedAt`

**已删除端点**：`generate/*`、`recompute/*`、`templates/*`、`template-updates/*`、`tasks/*`、`actionability`

---

## Profile

| 方法 | 路径 |
|---|---|
| GET/PUT | `/api/v1/profile` |
| POST | `/api/v1/profile/validate` |
| GET | `/api/v1/profile/field-usage` |
| GET/POST | `/api/v1/profile/backups`…（导出/预览/恢复保留） |

**PUT body（摘要）**

```json
{
  "expectedProfileVersion": 3,
  "nationality": "CN",
  "identityStatus": "Stamp 1G",
  "identityValidUntil": "2027-03-01",
  "targetCountry": "IE",
  "jobSeekingGoal": "Graduate backend roles in Dublin",
  "educationPeriods": [{ "institutionName": "…", "programmeName": "…", "startDate": null, "expectedGraduationDate": null, "countryCode": "IE", "isPrimary": true }],
  "workExperiences": [{ "company": "…", "title": "…", "startDate": null, "endDate": null, "summary": "…" }],
  "skills": ["Java", "SQL"],
  "languageProficiencies": [{ "languageCode": "en", "proficiency": "B2" }, { "languageCode": "zh", "proficiency": "NATIVE" }]
}
```

**已删除**：`targetRoles`、`workAuthorizations`、`/recompute-requests/*`

Gate 读 `identityStatus` + `identityValidUntil` + languages（不再读 TargetRole）。

---

## Sources / Sync

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/job-sources` | 列表；每项 `searchTerms` 为**全局同一份**（只读展示） |
| GET | `/api/v1/job-sources/search-terms` | `{ searchTerms: string[] }` |
| PUT | `/api/v1/job-sources/search-terms` | `{ searchTerms: string[] }` → 全局写入，**所有源同步共用** |
| PATCH | `/api/v1/job-sources/{id}` | `{ enabled? }`；若带 `searchTerms` 则写入全局（兼容旧客户端） |
| POST | `/api/v1/job-source-runs` | `{ sourceId }`；FreeHire= facet 矩阵+分页；JobSpy= junior 扩展词；全局 `searchTerms` 空 → **400 `SEARCH_TERMS_REQUIRED`** |

`JobSourceDto.searchTerms` = 全局用户自管词；**无** per-source 覆盖 / Profile 合并。  
调度同步使用全局 `searchTerms`；仍空则跳过并记 diagnostic。

---

## Home

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/home?actionLimit=3&newJobLimit=10` | 默认 **actionLimit=3**；进入时刷新「今日优先」三槽 |
| GET | `/api/v1/actions` | 展开时分页拉更多 |

**今日优先三槽**（`priorityRuleVersion=today-priority-v1`）：
1. 岗位动作：`REVIEW_TODAY_JOBS`（NEW 待审摘要）
2. 投递闭环：`ADVANCE_TODAY_TODO` / `ADVANCE_SHORTLIST`
3. 材料/公司：`FOLLOW_COMPANY` / `UPLOAD_CV` / CompanyTracker 引导

旧 Roadmap 模板 Action（`COMPLETE_ROADMAP_TASK`）进入 Home 时会被 supersede，不再占满前三。

响应可含 `actions.hasMore`（total > limit 时 true）。

---

## LeetCode

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/leetcode/problems` | `?q=&difficulty=&mastery=&review=` → `{ problems[], total }`（Hot 100 catalog） |
| GET | `/api/v1/leetcode/problems/{problemId}` | 题目 + optional `review`；若库中无正文则拉取并写入 `statementHtml`/`examples` |
| POST | `/api/v1/leetcode/problems/{problemId}/content/refresh` | 强制从 LeetCode 刷新正文并落库 |
| PUT | `/api/v1/leetcode/problems/{problemId}/review` | `{ confusion*, approach?, keyCode?, mastery, nextReviewAt? }` |
| DELETE | `/api/v1/leetcode/problems/{problemId}/review` | 204 |

**mastery**：`confident` \| `partial` \| `weak`  
**review filter**：`all` \| `reviewed` \| `unreviewed`  
UI 全英；疑惑点 `confusion` 必填。

---

## 前端调用策略

| 页 | 策略 |
|---|---|
| Home | `/home`；折叠其余，展开再 `GET /actions` 或提高 limit |
| Roadmap | 左文件夹（todolist / companytracker / document）/ 右对应表或 TipTap；字段 **~700ms 防抖 + blur + pagehide keepalive** 自动保存；Document 保存后不回灌编辑器 |
| LeetCode | `/leetcode` Hot 100 列表 + review 编辑（EN）；题面懒加载缓存 |
| Profile | 右上角圆形「P」入口；PUT 兼创建 |
| Nav | Home / Jobs / Sources / Roadmap / LeetCode / CV / Behavioral；无「M1 · local」角标 |
| Sources | 大搜索框写**全局** `PUT /job-sources/search-terms`；再按源手动/定时 run |
| CV | 列表 + multipart 上传 PDF/DOCX；预览打 PDF URL |
| Behavioral | Questions / Evidence / Practice 三区；反馈走本地规则 |

---

## CV

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/cv/documents` |
| POST | `/api/v1/cv/documents` | multipart `file`（PDF 或 DOCX） |
| GET | `/api/v1/cv/documents/{id}` |
| GET | `/api/v1/cv/documents/{id}/pdf` | `application/pdf`（仅 PDF 文档） |
| GET | `/api/v1/cv/documents/{id}/file` | 原文件：PDF `inline`，DOCX `attachment` |
| DELETE | `/api/v1/cv/documents/{id}` | 204 |

## Behavioral

| 方法 | 路径 |
|---|---|
| GET/POST/PATCH/DELETE | `/api/v1/behavioral/questions`… |
| GET/POST/PUT/DELETE | `/api/v1/behavioral/evidence`… |
| GET/POST | `/api/v1/behavioral/answers`… + `/versions` |
| POST | `.../versions/{versionId}/feedback` | 本地 `bhv-local-v1` |
| POST | `.../feedback/{id}/items/{itemId}/decision` |
| POST | `.../feedback/ai` | **501 `AI_NOT_ENABLED`** |
