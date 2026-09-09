# Coding 阶段入口

| 项 | 内容 |
|---|---|
| 状态 | **2026-09-08**：契约与 `jobAssitant/` 实现已对齐（M1 ~80%） |
| API 契约 | [`api-contract.md`](api-contract.md) — **改 API 先改此文件** |
| 进度 | [`../SRS/progress-log.md`](../SRS/progress-log.md) |
| 代码 | 同仓 `jobAssitant/backend` + `jobAssitant/frontend` |

## 文档清单

| 文件 | 用途 |
|---|---|
| [`specification.md`](specification.md) | 横切规约：错误体、分页、乐观锁、迁移 |
| [`server-coding-plan.md`](server-coding-plan.md) | 后端包 ↔ 设计域 |
| [`frontend-coding-plan.md`](frontend-coding-plan.md) | 前端页 ↔ 原型/设计 |
| [`api-contract.md`](api-contract.md) | FE↔BE 冻结摘要 |

## 后端包 ↔ 设计域

根包：`com.jobhelper`

| 设计目录 | Java 包 | 状态 |
|---|---|---|
| `server/profile/*` | `…profile.*` | ✅ |
| `server/roadmap/*` | `…roadmap.*` | ✅ 多夹 |
| `server/job/*` | `…job.*` | ✅ FreeHire + JobSpy |
| `server/action/*` | `…action.*` | ✅ Home + today-priority-v1 |
| `server/cv/*` | `…cv.*` | ✅ 文件管理 |
| `server/behavioral/*` | `…behavioral.*` | ✅ 本地域 |
| `server/leetcode/*` | `…leetcode.*` | ✅ |
| 跨域 | `…shared.outbox` / `…shared.web` | ✅ 骨架 |

## 前端路由 ↔ 源码

| 路由 | 设计页 | 源码（实际） |
|---|---|---|
| `/` | `frontend/home/home/` | `src/pages/HomePage.tsx` |
| `/jobs` | `frontend/jobs/jobs/` | `src/pages/JobsPage.tsx` |
| `/jobs/sources` | `frontend/jobs/jobs-sources/` | `src/pages/JobsSourcesPage.tsx` |
| `/roadmap` | `frontend/roadmap/roadmap/` | `src/pages/RoadmapPage.tsx` |
| `/leetcode` | `frontend/leetcode/leetcode/` | `src/pages/LeetCodePage.tsx` |
| `/profile` | `frontend/profile/profile/` | `src/pages/ProfilePage.tsx` |
| `/cv` | `frontend/cv/cv/` | `src/pages/CvPage.tsx` |
| `/behavioral` | `frontend/behavioral/behavioral/` | `src/pages/BehavioralPage.tsx` |

Nav / 布局：[`../2.design/frontend/ui-baseline.md`](../2.design/frontend/ui-baseline.md)

## 本地运行

```bash
# 方式 A：Docker（推荐新同学）
docker compose up --build

# 方式 B：本机进程
cd jobAssitant/backend && ./mvnw spring-boot:run
cd jobAssitant/frontend && npm install && npm run dev
```

## 约束

- API 变更同步 `api-contract.md` + 对应 `2.design/**/api.md`
- 不提交密钥、真实 CV/Profile 数据
- 旧 Roadmap generate/recompute：**文档已废止**；代码清理见 `progress-log` §2.2

## 收尾（当前）

见 [`../SRS/progress-log.md`](../SRS/progress-log.md) §2.2：测试、CI、死代码清理、SRS 正文 reconciliation
