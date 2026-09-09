# Delta 工作空间

Job Helper 的需求—设计—编码 Delta 工作空间。所有产品规格与细化文档集中在此目录。

## 目录结构

```text
delta/
├── SRS/           # 产品需求规格 + progress-log（进度真相源）
├── 1.req/         # 按业务域细化的用例、流程、实体
├── 2.design/      # 前端/服务端设计入口
└── 3.coding/      # 编码阶段规范与 API 契约
```

## 当前进度（2026-09-08）

| 阶段 | 状态 | 说明 |
|------|------|------|
| SRS | **v0.4 + Pivot** | 决策见 `SRS/SRS.md` §0.1；**进度以 `SRS/progress-log.md` 为准** |
| 1.req | **现行实体已回写** | Roadmap 多夹、LeetCode、Action 今日优先；旧 Roadmap 模板用例标废止 |
| 2.design | **核心页已回写** | Roadmap/LeetCode/Home/Nav；`prototype/roadmap.html` 为历史 |
| 3.coding | **契约与代码对齐** | `api-contract.md` ↔ `jobAssitant/` Controllers |
| 实现 | **M1 ~80% 可运行** | 同仓 `jobAssitant/`；可选 `docker compose up --build` |

**不必通读全部文件。** 优先：`progress-log.md` → `api-contract.md` → 各域 `entity.md`。

### 数据源（仍有效）

1. **FreeHire API**（主）— `scripts/datasource/freehire-fetch.js`
2. **JobSpy**（补充）— `scripts/datasource/jobspy_fetch.py`；后端 `JobSyncService` 已接子进程

### 仍有效的业务规则（2026-07-30，未被 Pivot 覆盖部分）

- Gate / Rank / Shortlist 用户确认
- Home Action 由 Action 域投影
- 每周备份 + 导出 + 恢复预览；JobSpy 失败进 SourceRun 诊断
- AI 默认关闭；全局同意注册表

### 已被 Pivot 取代（读 SRS §0.1，勿按下列实现）

- ~~Roadmap 系统模板 / GRS 锚点 / Profile→Roadmap 重算~~ → **多夹 Todo/Company/Document**
- ~~TargetRole 驱动搜索词~~ → **用户全局 searchTerms**
- ~~CV/Behavioral「M2/M3 未做」~~ → **本地文件域 / 题库域已落地**（完整 AI Review 链仍未接）

## 阅读顺序

1. [`SRS/progress-log.md`](SRS/progress-log.md) — **进度与功能矩阵**
2. [`3.coding/api-contract.md`](3.coding/api-contract.md) — **API 真相源**
3. [`SRS/SRS.md`](SRS/SRS.md) — 完整需求（正文含历史 FR，以 §0.1 Pivot 为准）
4. [`1.req/traceability.md`](1.req/traceability.md) — UC 索引
5. [`1.req/requirements.md`](1.req/requirements.md) — 域入口
6. [`2.design/design-check.md`](2.design/design-check.md) — 设计总检
7. [`3.coding/README.md`](3.coding/README.md) — 包/页映射

**历史参考（废止实现）：** `1.req/roadmap/system-template-v1.md`、`roadmap/workflow-desc.md`、`prototype/roadmap.html`

## 仓库其他目录

- `jobAssitant/` — 可运行前后端 + Docker
- `scripts/datasource/` — Phase 0 数据源验证
- 根 [`README.md`](../README.md) — Quick start
