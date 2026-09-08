# Page — Home

1. 「今日优先」默认渲染 **最多 3** 条，卡片宽度收紧（避免一行过宽留白）。  
2. 其余 OPEN Action **折叠**；展开再拉 `GET /actions` 或提高 `actionLimit`。  
3. 进入页 `GET /home` 时后端刷新 **今日优先三槽**（`today-priority-v1`）：岗位动作 → 投递闭环 → 材料/公司。  
4. 卡片按 `routeHint` 导航（Jobs / Roadmap?folderId= / CV）。  
5. 「为何优先」抽屉仍展示 priority evidence。  
6. 旧 `COMPLETE_ROADMAP_TASK` 等噪声 Action 进入时 supersede / 降权，不占满前三。

布局细节见 [`layout.md`](layout.md)；API 见 [`api.md`](api.md)。
