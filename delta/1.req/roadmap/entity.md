# roadmap entity（Pivot）

> **现行产品模型（2026-08）**：多文件夹 Roadmap；废止 Template / Dependency / GenerationRecord / GRS·STAMP 锚点 / Profile→Roadmap 重算。旧 `usecase-desc.md` / `workflow-desc.md` / 模板规格仅作历史参考。

## 1. 聚合边界

- **`RoadmapFolder`**：顶层容器（不嵌套）。`kind` 决定夹内内容类型。
- 每个文件夹只承载 **一种** 内容：TodoList / CompanyTracker / Document。
- 删除文件夹 **级联** 删除其全部内容；至少保留一个文件夹。

## 2. RoadmapFolder

| 属性 | 说明 |
|---|---|
| `folderId` | UUID |
| `name` | 显示名（如 `todolist`、`AI tips`） |
| `kind` | `todolist` \| `companytracker` \| `document`（创建后不可改） |
| `sortOrder` | 列表顺序 |
| `itemCount` | 派生：该夹内条目数 |

迁移：原扁平 todos → 种子夹「默认」后改名为 `todolist`（见 Flyway V13/V14）。

## 3. RoadmapTodo（`kind=todolist`）

| 属性 | 说明 |
|---|---|
| `todoId`, `folderId` | |
| `name` | 必填 |
| `dueAt?`, `comment?` | |
| `done` | checkbox |
| `sortOrder` | |

## 4. RoadmapCompany（`kind=companytracker`）

| 属性 | 说明 |
|---|---|
| `companyId`, `folderId` | |
| `companyName` | 必填 |
| `status` | `watching` \| `applied` \| `interview` \| `offer` \| `rejected` \| `on_hold` |
| `contact?`, `note?` | |
| `sortOrder` | |

用途：目标公司跟进；Home「今日优先」第三槽可引用 `watching` 公司。

## 5. RoadmapDocument（`kind=document`）

| 属性 | 说明 |
|---|---|
| `documentId`, `folderId` | |
| `title` | 默认 `Untitled` |
| `bodyHtml` | 所见即所得 HTML（TEXT）；服务端不解析排版 |
| `sortOrder` | |

用途：长文笔记（如 AI tips）。前端 TipTap 编辑 + 防抖自动保存；**不以保存回写 HTML 重置编辑器**（避免规范化反馈环）。

## 6. 不变量

1. Todo / Company / Document 的 `folderId` 必须指向对应 `kind` 的文件夹。
2. 不可删除最后一个文件夹。
3. 无嵌套文件夹；无跨 kind 混放内容。
