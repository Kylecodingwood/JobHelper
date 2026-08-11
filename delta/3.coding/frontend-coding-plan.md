# 前端编码计划（M1）

## 1. 工程骨架

- Vite + React + TypeScript + React Router  
- 样式：从 `2.design/prototype/shared.css` 提取 CSS variables（Figtree / Fraunces）  
- API：`fetch` 或轻量 client；**路径只跟后端 `*-api`**（见 [`api-contract.md`](api-contract.md)）

## 2. 页面实现顺序

| 序 | 页 | 原型 | 要点 |
|---|---|---|---|
| 1 | 全局壳 + Nav | 各页共用 | sticky Nav、品牌、即将推出占位 |
| 2 | Profile onboarding + edit | profile*.html | PUT；侧栏 recompute/backup |
| 3 | Home | index.html | **仅** `GET /home`；navigation + decisions |
| 4 | Jobs Inbox | jobs.html | 分栏；DetailDto 优先；status / gate-override |
| 5 | Jobs Sources | jobs-sources.html | job-sources / job-source-runs |
| 6 | Roadmap | roadmap.html | `GET /roadmap`；preview→confirm |

## 3. 调用优化（已写入各页 api.md）

- Home 单请求；Jobs 详情少打 evidence；Sources diagnostics 内嵌；Roadmap 聚合一次；Profile 影响来自 PUT 响应。

## 4. 状态与错误

- 无 Profile → 强制 `/profile` onboarding  
- 409 → Toast「已变更，请刷新」+ refetch  
- Roadmap 404 → 空态 CTA 生成  

## 5. 验收

对照 `ui-baseline.md` 逐页走查结构与密度；契约抽检对照 `server/*/…-api/delta.md`。

## 6. 非目标（M1）

CV/Behavioral 真页；Company-forward；真实 AI。
