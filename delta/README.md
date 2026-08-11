# Delta 工作空间

Job Helper 的需求—设计—编码 Delta 工作空间。所有产品规格与细化文档集中在此目录。

## 目录结构

```text
delta/
├── SRS/           # 产品需求规格（单一事实来源）
├── 1.req/         # 按业务域细化的用例、流程、实体
├── 2.design/      # 前端/服务端设计入口
└── 3.coding/      # 编码阶段规范与映射
```

## 当前进度（2026-08-09）

| 阶段 | 状态 | 说明 |
|------|------|------|
| SRS | **v0.4 已确认** | Gate、Rank、Roadmap、CV、Action、备份与同步规则已冻结 |
| 1.req | **门禁通过** | 六域 UC/流程/实体、跨域事件、种子模板与策展题库已同步 |
| 2.design | **M1 完成** | 文档 + 原型 UI 基线；前端 API **已按后端对齐** |
| 3.coding | **映射已完成** | README / specification / api-contract / 前后端 coding-plan；**无应用源码** |
| 数据源验证 | **已完成** | 见 `../scripts/datasource/`，结果见下 |
| 实现仓 | **未开始** | 下一步：独立前后端仓按 `3.coding` 开工 |

### 数据源验证结论

M1 自动来源已从「EURES + 公司 ATS」调整为：

1. **FreeHire API**（主发现源）— 公开 REST，`countries=ie` facet + 关键词；验证脚本 `scripts/datasource/freehire-fetch.js`
2. **JobSpy**（补充源）— 本地 Python，`linkedin` / `indeed` 等 8 站点；验证脚本 `scripts/datasource/jobspy_fetch.py`

**2026-07-29 跑数结果**（`scripts/datasource/out/`）：

| 源 | 唯一岗位 | 备注 |
|----|----------|------|
| FreeHire | 1,345 | 8 组 IE 查询，去重后；多数 URL 指向 Greenhouse 等 ATS |
| JobSpy LinkedIn | 115 | |
| JobSpy Indeed | 107 | |
| JobSpy 其他站点 | 0 | ZipRecruiter/Glassdoor 403，Naukri recaptcha，BDJobs library bug |

EURES 与独立 Greenhouse/Lever 探针**已弃用**，不再作为 M1 数据源。

### 2026-07-30 已确认业务规则

- Gate 是清洗后的硬资格判断；失败岗位保留、默认隐藏并可覆盖；缺入职日 → 工作授权 `NEEDS_CONFIRMATION`。
- Rank 五项因素正/中/负投票聚合为 `High/Medium/Low`；Shortlist 只由用户确认。
- Ireland 工作许可不推断为 EU 工作许可；EU 岗位默认待确认。
- Roadmap：系统种子模板 + 结构化 Markdown 导入 + 领域事件追加；招聘季=毕业日前最近 9 月 1 日；生成/重算先预览确认。
- Home Action 由 Action 域唯一拥有；Roadmap 只维护任务并发布事件。
- CV 支持通用与 Job 定向 Review；本地规则始终可用；无 OCR。
- Behavioral 策展题库 v1（24 题）；STAR Evidence 可复用。
- AI provider / Cursor Skill/SDK 后选且默认关闭；全局按敏感数据类别授权。
- 每周本地备份 + 完整导出 + 恢复预览；敏感删除级联硬删除。
- JobSpy 每日全站点尝试；失败只进入 SourceRun 诊断。
- 搜索词默认来自 TargetRole；语言使用独立 LanguageProficiency。

## 阅读顺序

1. [`SRS/SRS.md`](SRS/SRS.md) — 完整需求
1a. [`SRS/progress-log.md`](SRS/progress-log.md) — 进度 / M1 范围 / 能否开工代码
2. [`1.req/requirements.md`](1.req/requirements.md) — 域入口与细化顺序
3. [`1.req/cross-domain-events.md`](1.req/cross-domain-events.md) — 跨域事件契约
4. [`1.req/job/datasource-validation.md`](1.req/job/datasource-validation.md) — 数据源验证记录
5. [`1.req/traceability.md`](1.req/traceability.md) — UC ↔ FR ↔ Workflow ↔ Entity 追踪
6. [`1.req/roadmap/system-template-v1.md`](1.req/roadmap/system-template-v1.md) — Roadmap 种子模板
7. [`1.req/behavioral/curated-question-bank.md`](1.req/behavioral/curated-question-bank.md) — Behavioral 策展题库
8. [`2.design/design-check.md`](2.design/design-check.md) — 设计总检
9. [`3.coding/README.md`](3.coding/README.md) — 实现映射与切片顺序
10. [`3.coding/api-contract.md`](3.coding/api-contract.md) — API 对齐冻结

## 仓库其他目录

- `scripts/datasource/` — Phase 0 数据源验证脚本（不在 Delta 编码阶段内，但为 M1 Job 域前置验证）
- `PRODUCT_CHARTER.md` / `DEVELOPMENT_PLAN.md` — 产品宪章与开发计划（仓库根目录）
