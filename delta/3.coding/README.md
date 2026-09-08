# Coding 阶段入口


| 项      | 内容                                                                                                                                |
| ------ | --------------------------------------------------------------------------------------------------------------------------------- |
| 状态     | **已含 2026-08-25 增补**（Roadmap 多夹 / Document / 今日优先 / LeetCode） |
| API 契约 | [`api-contract.md`](api-contract.md)（与 `jobAssitant/` 实现对齐） |
| 本目录性质  | **实现映射文档**；可运行代码在 `jobAssitant/` |


## 文档清单


| 文件                                                   | 用途                                |
| ---------------------------------------------------- | --------------------------------- |
| `[specification.md](specification.md)`               | 横切规约：错误体、分页、乐观锁、幂等、日志、迁移          |
| `[server-coding-plan.md](server-coding-plan.md)`     | 后端包结构 ↔ `2.design/server`、M1 切片顺序 |
| `[frontend-coding-plan.md](frontend-coding-plan.md)` | 前端目录 ↔ 页面/原型、调用优化策略               |
| `[api-contract.md](api-contract.md)`                 | FE↔BE 对齐冻结摘要                      |




## 后端包 ↔ 设计域

实现仓建议根包：`com.jobhelper`（可改，须在仓内 README 固定）。


| 设计目录               | Java 包（建议）                                                      | 职责                     |
| ------------------ | --------------------------------------------------------------- | ---------------------- |
| `server/profile/*` | `…profile.*` | REST、应用服务、实体 |
| `server/roadmap/*` | `…roadmap.*` | 多夹 Todo/Company/Document |
| `server/job/*` | `…job.*` + adapters | FreeHire / JobSpy |
| `server/action/*` | `…action.*` | Home BFF + `today-priority-v1` |
| `server/cv/*` | `…cv.*` | 文件管理 |
| `server/behavioral/*` | `…behavioral.*` | 本地域 |
| `server/leetcode/*` | `…leetcode.*` | Hot 100 + review |
| 跨域 | `…shared.outbox` / `…shared.web` | Outbox、统一错误体 |


域内分层与 `component-*.puml` 一致：`api → crud/application → model → DB`；workflow 可为 application 内编排或独立服务类。

## 前端目录 ↔ 页面

实现仓：Vite + React + React Router。


| 路由              | 原型                          | 设计页                           | 建议源码                      |
| --------------- | --------------------------- | ----------------------------- | ------------------------- |
| `/`             | `prototype/index.html`      | `frontend/home/home/`         | `src/pages/home/`         |
| `/jobs`         | `jobs.html`                 | `frontend/jobs/jobs/`         | `src/pages/jobs/`         |
| `/jobs/sources` | `jobs-sources.html`         | `frontend/jobs/jobs-sources/` | `src/pages/jobs/sources/` |
| `/roadmap`      | `roadmap.html`              | `frontend/roadmap/roadmap/`   | `src/pages/roadmap/`      |
| `/profile`      | `profile.html` + onboarding | `frontend/profile/profile/`   | `src/pages/profile/`      |


共享：`src/shared/api/`（按后端路径封装）、`src/shared/ui/`（令牌来自 `prototype/shared.css`）。

## 约束

- 不绕过 Domain / Source Adapter 边界
- 不提交密钥、真实 CV/Profile/申请记录
- 实现与测试可追溯到 UC / FR
- CV / Behavioral / Company-forward：**不在 M1 编码范围**



## 下一步（工作空间外）

1. 创建后端仓：Spring Boot + PostgreSQL + 四域骨架
2. 创建前端仓：Vite React，按原型还原壳与五页
3. 第一条纵向切片建议：**Profile onboarding → PUT/GET → 路由守卫 → Home 空态**

