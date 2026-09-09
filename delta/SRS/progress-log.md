## Pivot 2026-09-08 · Docker Compose

| 能力 | 要点 | 关键文档 |
|---|---|---|
| 本地一键栈 | `docker compose up --build` → db + backend + frontend | 根 `README.md`；`docker-compose.yaml` |
| 前端生产态 | Vite build + nginx；`/api` 反代 backend | `jobAssitant/frontend/Dockerfile`、`nginx.conf` |

---

## Pivot 2026-08-25 · Roadmap 多夹 / Document / 今日优先 / LeetCode

| 能力 | 要点 | 关键文档 |
|---|---|---|
| Roadmap 多文件夹 | kind=`todolist`\|`companytracker`\|`document`；级联删；至少一夹 | `1.req/roadmap/entity.md`；`2.design/server/roadmap/*` |
| CompanyTracker | status 六态；Home 第三槽可跟进 | 同上 |
| Document + TipTap | `bodyHtml`；防抖自动保存；禁止保存回灌编辑器 | `2.design/frontend/roadmap/*` |
| Home 今日优先 v1 | 三固定槽：审岗→推进→材料/公司；supersede 旧 Roadmap Action | `1.req/action/entity.md` §2.2 |
| LeetCode | Hot 100、review、题面 GraphQL 缓存；Nav `/leetcode` | `1.req/leetcode/*` |
| Nav | Profile→头像 P；Sources/CV/Behavioral/LeetCode 入主导航 | `2.design/frontend/pages.md` |

Flyway：`V13`–`V17`。契约：`3.coding/api-contract.md`。

---

## Pivot 2026-08-09 · CV / Behavioral / Persona / Sources

- **CV**：PDF/DOCX 上传、列表/预览/删除；无抽文本产品能力（`V7`、`V10`）
- **Behavioral**：题库 + Evidence + 答案 + 本地反馈；AI Port → `501 AI_NOT_ENABLED`
- **Roadmap**：独立 Notion 式待办 → 后扩展为多夹（见上）
- **Profile**：画像基座；废止 TargetRole；身份含原 Work Auth
- **Sources**：用户自管全局 `searchTerms`；首次同步前必填

---

# Job Helper — 进度与实现范围日志

| 项目 | 内容 |
|---|---|
| 日志版本 | **2026-09-09** |
| Git 作者 | 本仓 `user.email` = GitHub 已验证邮箱（贡献图计数） |
| 对应需求 | [`SRS.md`](SRS.md) v0.4 + 本日志 Pivot 增补 |
| 产品阶段 | M1 主功能已可本地日常使用；收尾项见 §2 |
| **进度真相源** | **本文件 Pivot 表 + [`../3.coding/api-contract.md`](../3.coding/api-contract.md)** |
| 实现仓 | `jobAssitant/backend` + `jobAssitant/frontend`（同仓） |

---

## 1. 当前阶段（2026-09-08）

**Delta 文档阶段已完成；M1 应用主体已实现。**

| 阶段 | 状态 | 说明 |
|---|---|---|
| 1 · Requirement（SRS + `1.req`） | ✅ | 门禁通过；Pivot 后见 `entity.md` / `traceability.md` |
| 2 · Design（`2.design`） | ✅ | 现行 UI 以代码 + 新版 `roadmap/page.md` 为准；`prototype/roadmap.html` 为历史 |
| 3 · Coding 映射（`3.coding`） | ✅ | `api-contract` 与 Controller 对齐 |
| **可运行应用** | ✅ **~80% M1** | 见 §4 功能矩阵；§2 为剩余项 |

**文档阅读建议（不必通读全部 Delta）：**

1. 本文件 Pivot 表 + §4  
2. [`../3.coding/api-contract.md`](../3.coding/api-contract.md)  
3. 各域现行 [`../1.req/*/entity.md`](../1.req/roadmap/entity.md)  
4. 旧 Roadmap 模板 / GRS 用例：**已废止**，仅作历史参考

---

## 2. 还有什么没做？

### 2.1 已实现（与代码一致）

- [x] 后端 Spring Boot + Flyway **V1–V17**
- [x] 前端 Vite React：**8 页**（Home / Jobs / Sources / Roadmap / LeetCode / Profile / CV / Behavioral）
- [x] Profile CRUD + field-usage
- [x] Home BFF + **今日优先三槽**（`today-priority-v1`）
- [x] Jobs Inbox：Gate / Rank / 状态 / 手动 URL / 去重
- [x] Jobs Sources：FreeHire + **JobSpy 子进程**、全局 searchTerms、SourceRun 诊断
- [x] Roadmap：**多夹** Todo / Company / Document（TipTap 自动保存）
- [x] LeetCode Hot 100 + review + 题面缓存
- [x] CV 文件管理；Behavioral 本地域
- [x] 备份：`BackupService` 周日 03:00 + 手动导出/恢复预览
- [x] 同步：`JobSyncService` 每日 06:00
- [x] Outbox：`OutboxPublisher` 定时 poll（骨架）
- [x] **Docker Compose** 一键本地栈

### 2.2 收尾 / 质量（仍开放）

- [ ] 单元/集成测试底线（见 `3.coding/specification.md`）
- [ ] CI 流水线
- [ ] 乐观锁 `expected*Version` 全面落地（部分域已有）
- [ ] Outbox 事件消费完整化、删 Roadmap 旧 generate/recompute **死代码**
- [ ] `package-lock.json` 修复以支持 Docker 内 `npm ci`
- [ ] SRS 正文 FR 与 Pivot 全面 reconciliation（§0.1 已覆盖决策，正文仍混有历史 FR）
- [ ] `prototype/roadmap.html` 与现 UI 对齐或标为 ARCHIVED

### 2.3 明确不做 / 未接

| 项 | 说明 |
|---|---|
| AI provider 集成 | 默认关闭；Behavioral/CV AI Port → `501` |
| CV **定向 Review** / 建议决策链 | 文件管理已做；完整 Review 工作流未做 |
| Roadmap **模板 / 生成 / 重算** | Pivot 废止；后端遗留 API 待清理 |
| Company-forward / referral | 见 `add.md` |
| 经验贴 / Reddit 抓取 | v1 不做 |
| 多租户 / 云 SaaS | 不做 |

---

## 3. 进度总览

```text
SRS v0.4 + Pivot ──► 1.req / 2.design / 3.coding ✅
                           │
                           ▼
              jobAssitant/ 可运行应用 ✅（M1 ~80%）
                           │
                           ▼
              测试 / CI / 文档大扫除 / 死代码清理  ← 当前收尾
```

| 日期 | 里程碑 |
|---|---|
| 2026-07-29 | Phase 0 数据源：FreeHire + JobSpy |
| 2026-07-30 | SRS v0.4；1.req 门禁 |
| 2026-07-31 | 2.design M1 + UI 原型基线 |
| 2026-08-09 | Pivot：Persona / Notion Roadmap / CV·Behavioral 本地域 |
| 2026-08-25 | Roadmap 多夹 / Document / 今日优先 / LeetCode；Delta 回写 |
| 2026-09-08 | Docker Compose 合并；**本日志与代码对齐** |

---

## 4. 功能矩阵（文档 ↔ 代码）

| 模块 | 文档 | 代码 | 一致 |
|---|---|---|---|
| Profile | `api-contract` Profile | `ProfileController` + `ProfilePage` | ✅ |
| Home / Action | `today-priority-v1` | `HomeController` + `HomeTodayPriorityService` | ✅ |
| Jobs | job-api | `JobController` + `JobsPage` | ✅ |
| Sources | job-api sync | `JobSyncService` + `JobsSourcesPage` | ✅ |
| Roadmap 多夹 | roadmap-api + entity | `RoadmapController` + `RoadmapPage` | ✅ |
| LeetCode | leetcode-api | `LeetCodeController` + `LeetCodePage` | ✅ |
| CV 文件 | cv-api | `CvController` + `CvPage` | ✅ |
| Behavioral | behavioral-api | `BehavioralController` + `BehavioralPage` | ✅ |
| 备份 / 同步调度 | profile/job workflow | `@Scheduled` in services | ✅ |
| Docker | 根 README | `docker-compose.yaml` | ✅ |
| Roadmap 模板生成 | 历史 workflow | `RoadmapService.generate*` 遗留 | ⚠️ 待删 |
| UI 原型 Roadmap | 时间线 | 左夹右表 | ⚠️ 原型过时 |

---

## 5. 权威入口速查

| 用途 | 路径 |
|---|---|
| **进度（本文）** | [`progress-log.md`](progress-log.md) |
| **API / 行为真相** | [`../3.coding/api-contract.md`](../3.coding/api-contract.md) |
| 需求索引 | [`../1.req/traceability.md`](../1.req/traceability.md) |
| 设计总检 | [`../2.design/design-check.md`](../2.design/design-check.md) |
| 怎么跑 | 仓库根 [`README.md`](../../README.md) |
| UI 基线 | [`../2.design/frontend/ui-baseline.md`](../2.design/frontend/ui-baseline.md) |
