## Pivot 2026-08-09 · CV / Behavioral

- CV：DOCX 上传 → PDF 管理（列表/预览/删除）；无抽文本产品能力；实现用 POI+PDFBox 生成预览 PDF（非排版级 Word 渲染）
- Behavioral：完整本地域（题库/Evidence/答案/本地反馈）；AI 仅 Port → `501 AI_NOT_ENABLED`
- 代码：`V7__cv_behavioral.sql` + `/cv` `/behavioral` 前后端已落地

## Pivot 2026-08-09

- Roadmap → 独立 Notion 待办（name/due/comment/checkbox），去模板/依赖/重算
- Profile → 画像基座；去 TargetRole；身份合并 Work Auth；多语言/skills/经验
- Sources → 用户自管 searchTerms + 大搜索框；上下布局；首次同步必填
- Home → 默认 3 条优先，其余折叠展开

# Job Helper — 进度与实现范围日志

| 项目 | 内容 |
|---|---|
| 日志版本 | 2026-08-09 |
| 对应需求 | [`SRS.md`](SRS.md) v0.4 |
| 产品阶段 | M1（Personal-first / Local-first / Ireland） |
| 文档状态 | **1.req → 2.design → 3.coding 已完成；应用仓 M1 API/契约对齐已落地（duplicates/依赖/模板/备份 zip/预览持久化/FE 合同页）** |

---

## 1. 当前能否直接生成代码？

**可以开始生成应用代码。**

Delta 三阶段文档门禁对 M1 已齐：

| 阶段 | 状态 | 含义 |
|---|---|---|
| 1 · Requirement（SRS + `1.req`） | ✅ 完成 | 需求与用例可追溯 |
| 2 · Design（`2.design`） | ✅ 完成 | 前后端设计、原型 UI、API 以后端为准已对齐 |
| 3 · Coding 映射（`3.coding`） | ✅ 完成 | 包/页面映射、横切规约、切片顺序；**不是**可运行代码 |

下一步：在本仓 `jobAssitant/backend` + `jobAssitant/frontend` 按 `3.coding` 与 `2.design`（含 `prototype/`）继续纵向切片。

---

## 2. 还有什么没做？

### 2.1 必须做（进入可运行 M1）

- [x] 创建/初始化 **后端工程**（Spring Boot；PostgreSQL / Flyway）
- [x] 创建/初始化 **前端工程**（Vite + React），UI 对齐 `prototype/`
- [x] 纵向切片 1：Profile → Home → Jobs/Actions stubs；Roadmap 404
- [x] Profile 保存驱动 Roadmap 生成 + Home Actions；FreeHire 同步 + Gate/Rank（JobSpy 入口保留，同步以 FreeHire 为主）
- [ ] JobSpy 子进程实装、Outbox 完整、备份调度、测试底线
- [ ] 本地联调加深与 CI
- [ ] 测试底线（见 `3.coding/specification.md`）

### 2.2 文档层可选补强（不挡开工）

- [ ] `3.coding/jpa/`、`custom/` 细表映射（规范可选，可边写代码边补）
- [ ] 人工再抽检一轮 API 与 Controller 一致性（实现中做）
- [ ] 更新本日志随每次里程碑交付

### 2.3 明确不在当前版本（M1）

| 项 | 说明 |
|---|---|
| AI 集成 | 默认关闭；M1 不接 provider |
| CV 完整域 | M2 |
| Behavioral 完整域 | M3 |
| Company-forward / referral 链 | 见仓库根 `add.md` §8 |
| 经验贴 / Reddit 自动抓取 | v1 不做 |
| 桌面壳 | 可选增强，非 M1 |
| 多租户 / 云 SaaS | 不做 |

---

## 3. 当前进度总览

```text
SRS v0.4 ──► 1.req（门禁通过）
                │
                ▼
           2.design（M1 填满）
           · 前端 5 页 + UI 原型基线
           · 后端四域 model/crud/api/workflow
           · FE API 按 BE 对齐（2026-08-09）
                │
                ▼
           3.coding（映射完成）
           · README / specification / api-contract
           · server & frontend coding-plan
                │
                ▼
           【下一跳】独立仓生成应用代码  ← 你现在在这里
```

| 日期 | 里程碑 |
|---|---|
| 2026-07-29 | Phase 0 数据源：FreeHire + JobSpy；弃用 EURES/独立 ATS |
| 2026-07-30 | SRS v0.4 / 1.req 门禁；技术栈 ADR；业务规则冻结 |
| 2026-07-31 | 2.design M1 正文 + PlantUML 可预览；UI 原型验收为基线 |
| 2026-08-08 | add.md 增补 Company-forward 未来策略 |
| 2026-08-09 | FE↔BE API 对齐；3.coding 映射写完；**本日志建立** |

---

## 4. 当前版本（M1）功能清单

### 4.1 用户可见

| 能力 | 说明 |
|---|---|
| Profile | Onboarding + 编辑；教育/工签/目标岗/语言/技能经历；保存影响与重算预览确认 |
| Home | 今日优先 Action + New Job 条带；为何优先；pin/complete/ignore；导航源对象 |
| Jobs Inbox | 高密度列表 + 详情；Gate/Rank 证据；状态决定；Gate 覆盖；去重；保存 URL |
| Jobs Sources | FreeHire / JobSpy 来源；手动同步；SourceRun 诊断 |
| Roadmap | 单页时间线；模板/生成/重算/系统更新（preview→confirm）；任务推进 |
| 备份 | 周日 03:00 自动（保留 4）+ 手动导出 + 恢复预览 |
| UI | 对齐 `2.design/prototype/`（墨绿色纸感、Nav、分栏密度） |

### 4.2 系统能力（非菜单）

| 能力 | 说明 |
|---|---|
| 同步 | 每日 06:00；FreeHire 主源 + JobSpy 补充 |
| Gate | LOCATION / WORK_AUTH / SENIORITY / LANGUAGE；缺入职日 → WORK_AUTH 待确认 |
| Rank | 五因素投票 → HIGH/MEDIUM/LOW；不自动 Shortlist |
| Action | 由事件投影；跨域优先级；不拥有 Job 状态 |
| 乐观锁 | Profile / Job / Action 写带 expected*Version（设计已定，代码待写） |
| Outbox / 事件 | 域间异步协作骨架 |

### 4.3 技术栈（实现目标）

Java Spring Boot · PostgreSQL · React (Vite) · Python JobSpy · 本地 Web

---

## 5. 当前版本「实现内容」指什么？

| 层 | 已有产物 | 未有产物 |
|---|---|---|
| 需求 | SRS、1.req UC/流程/实体 | — |
| 设计 | 2.design 全文、component/api puml、prototype HTML | — |
| 编码说明 | 3.coding 映射与规约 | — |
| **可运行应用** | 本仓 `jobAssitant/backend` + `jobAssitant/frontend`（切片 1） | Outbox / Gate-Rank / 同步 / Roadmap 生成 / CI |

因此：**文档 1→2→3 已完成；M1 应用切片 1 已可本地跑通 Profile→Home→Jobs。**

---

## 6. 给后续版本的预留（非本版实现）

- Nav / 路由：`/cv`、`/behavioral`「即将推出」
- Action：`REVIEW_CV_*` / `COMPLETE_BEHAVIORAL_*` 与事件消费规则预留
- 后端包规划：`(M2) cv` / `(M3) behavioral`
- SRS：AIConsent / AIInvocation；AI 默认关闭
- `add.md`：Company-forward strategy

---

## 7. 权威入口速查

| 文档 | 路径 |
|---|---|
| 需求 | [`SRS.md`](SRS.md) |
| 设计总检 | [`../2.design/design-check.md`](../2.design/design-check.md) |
| API 冻结 | [`../3.coding/api-contract.md`](../3.coding/api-contract.md) |
| 实现入口 | [`../3.coding/README.md`](../3.coding/README.md) |
| UI 原型 | [`../2.design/prototype/`](../2.design/prototype/) |
| 工作空间总览 | [`../README.md`](../README.md) |
