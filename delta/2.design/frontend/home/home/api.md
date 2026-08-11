# API — Home

- 路由：`/`
- **主入口** `GET /api/v1/home?actionLimit=3`（默认 3）

| 场景 | 调用 |
|---|---|
| 进入 | `/home`（只要 3 条优先） |
| 展开更多 | `GET /actions?status=OPEN&active=true&size=20` 或 `/home?actionLimit=20` |
| 导航 / 决定 / 证据 | 同前（navigation、decisions、priority-evidence） |

响应建议：`actions.hasMore: boolean`。
