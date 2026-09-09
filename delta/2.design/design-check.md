# Design 阶段总检

当前状态：**M1 设计文档已完成；`jobAssitant/` 已实现主体功能（2026-09-08）。**

**一致性规则：**

- **API** → [`../3.coding/api-contract.md`](../3.coding/api-contract.md) ↔ Controllers  
- **进度** → [`../SRS/progress-log.md`](../SRS/progress-log.md)  
- **现行 Roadmap UI** → [`frontend/roadmap/roadmap/page.md`](frontend/roadmap/roadmap/page.md)（非 `prototype/roadmap.html` 时间线）

## 已确认决策

| 项 | 决策 |
|---|---|
| delta 角色 | 规格工作空间；代码在同仓 `jobAssitant/` |
| 前端 | Vite + React SPA；**8 路由**（含 LeetCode / CV / Behavioral） |
| UI | 纸感 Nav + 分栏；Roadmap 为**左夹右内容** |
| API | 以后端为准；`api-contract` 冻结 |
| 同步 / 备份 | 06:00 同步；周日 03:00 备份（代码已实现 `@Scheduled`） |
| 本地运行 | 可选 `docker compose up --build` |

## 规范符合性

- [x] M1 核心页设计目录 + PlantUML
- [x] 后端域 model/api（含 cv、behavioral、leetcode 增补）
- [x] `api-contract` 与 Controller 对齐（2026-09-08 抽检）
- [x] `progress-log` 功能矩阵与代码对齐
- [ ] `prototype/roadmap.html` 与现 UI 对齐（**待标 ARCHIVED 或更新**）
- [ ] SRS 正文 FR 与 Pivot 全面 reconciliation

## 产物索引

| 层 | 入口 |
|---|---|
| 进度 | [`../SRS/progress-log.md`](../SRS/progress-log.md) |
| API | [`../3.coding/api-contract.md`](../3.coding/api-contract.md) |
| 前端总览 | [`frontend/pages.md`](frontend/pages.md)、[`frontend/ui-baseline.md`](frontend/ui-baseline.md) |
| 后端总览 | [`server/delta.md`](server/delta.md) |
| 原型（部分过时） | [`prototype/`](prototype/) |
| Coding | [`../3.coding/README.md`](../3.coding/README.md) |

## 不做 / 已废止

- Roadmap 模板时间线 / generate / recompute（Pivot 废止）
- AI provider 默认集成
- 多租户 SaaS、经验贴抓取
