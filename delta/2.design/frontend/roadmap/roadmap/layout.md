# Layout — Roadmap（Pivot 2026-08）

- 路由：`/roadmap`
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)
- 原型 `roadmap.html` 时间线为**历史参考**；现行为左右分栏。

## 布局目标

- **左夹右内容**：文件夹列表固定左侧；右侧按 `kind` 切换表或文档编辑器。
- **单页无子路由**：不拆 `/roadmap/:folderId` 独立页（可用 query `?folderId=`）。

## 区域划分

| 区域 ID | 名称 | 职责 |
|---|---|---|
| `RoadmapPageShell` | 页面壳 | 加载/错误/Saving hint |
| `RoadmapFolderPane` | 左栏 | 文件夹列表、重命名、删除、新建（含 kind） |
| `RoadmapContentPane` | 右栏 | 按 kind 渲染 |
| `TodoTable` | todolist | checkbox / name / due / comment |
| `CompanyTable` | companytracker | name / status / contact / note |
| `DocumentSplit` | document | 文档列表 + title + TipTap |
| `DocumentEditor` | TipTap | toolbar + surface；ErrorBoundary |

## 交互要点

- 切换文件夹先 flush 未保存字段。  
- Todo 表默认按 due / 新建时间排序（见 `page.md`）；无手动拖拽排序。  
- Document 仅切换文档时重置编辑器内容。
