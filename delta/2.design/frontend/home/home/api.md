# API — Home

- 路由：`/`
- **主入口** `GET /api/v1/home?actionLimit=3`（默认 3）
- 进入时服务端运行 `HomeTodayPriorityService.refresh()`（`priorityRuleVersion=today-priority-v1`）

| 场景 | 调用 |
|---|---|
| 进入 | `/home`（三槽优先） |
| 展开更多 | `GET /actions?status=OPEN&active=true&size=20` 或 `/home?actionLimit=20` |
| 导航 / 决定 / 证据 | navigation、decisions、priority-evidence |

三槽规则摘要：

1. `REVIEW_TODAY_JOBS` — NEW 待审  
2. `ADVANCE_TODAY_TODO` / `ADVANCE_SHORTLIST` — 投递闭环  
3. `FOLLOW_COMPANY` / `UPLOAD_CV` / CompanyTracker 引导  

响应可含 `actions.hasMore: boolean`。详见 [`../../../../3.coding/api-contract.md`](../../../../3.coding/api-contract.md)。
