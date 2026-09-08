# Page — Roadmap（Pivot 2026-08）

左文件夹列表 + 右内容区（按 `kind` 切换）。

## 布局

```text
┌────────────┬──────────────────────────────────┐
│ 文件夹列表  │  Todo 表 / Company 表 / Document  │
│ + 新建夹    │  （含文档列表 + 标题 + TipTap）   │
└────────────┴──────────────────────────────────┘
```

## 行为

1. `GET /roadmap?folderId=` 加载 folders + 当前夹内容。  
2. 创建文件夹：名称 + kind（`todolist` / `companytracker` / `document`）；入口在文件夹列表底部。  
3. **TodoList**：Notion 式表；comment/name **防抖 ~700ms** + blur flush；切页 `keepalive` 冲刷。  
4. **CompanyTracker**：company / status / contact / note；同样自动保存。  
5. **Document**：文档列表 → 选中后编辑 title + TipTap `bodyHtml`；自动保存；**仅 `documentId` 变化时 `setContent`**，避免 HTML 规范化回灌白屏。  
6. 顶栏可显示 `Saving…` / `Saved`。  
7. 不可删最后一个文件夹；删夹前确认级联。

无模板抽屉、无重算、无依赖。
