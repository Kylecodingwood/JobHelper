# Page — Roadmap

Notion 风格表：checkbox | name | due | comment | 删除。

1. 加载 `GET /roadmap`，表格渲染 `todos`。  
2. 「新建」→ `POST` 空行或弹层填 name。  
3. 单元格失焦 / Enter → `PATCH`。  
4. 勾选 → `toggle`。  
5. 行删除确认 → `DELETE`。  

无模板抽屉、无重算、无依赖覆盖。视觉参考 Notion Tasks：紧凑行、表头图标、深色可选。
