# Job Assistant 补充调研：`job-scan` 借鉴点与爱尔兰数据源

**调研日期：** 2026-07-26（数据源策略于 2026-07-29 更新；同背景经验贴调研于 2026-07-31 增补；2026-08-09：经验贴与 Company-forward 归入 AI 域预留）

> **当前 M1 数据源（SRS v0.4）：** FreeHire API（主）+ JobSpy（补充）。EURES 与独立 ATS 探针已弃用。  
> **经验贴 / Company-forward：** 不作为 Job 主源；已归入下文 **§7 AI 域预留**，不进当前 `1.req` / `2.design`。

## 1. `job-scan` 值得借鉴的设计

### 1.1 Gate → Cap → Tier

该项目不会立即让 LLM 给所有岗位打分，而是分三步处理：

1. **Gate：** 先用确定性代码检查语言、公民身份或工签、安全审查、岗位类别等条件；
2. **Cap：** 对职级、核心技术栈等明显不匹配的岗位设置评分上限；
3. **Tier：** 最后才让 LLM 根据候选人档案、岗位证据和缺口判断匹配层级。

这一流程符合本项目“确定性代码优先、AI 可选且受控”的原则。实现时，每项判断都应保存触发规则、JD 原文证据和规则版本，不能只保存最终分数。

### 1.2 确定性处理与 AI 解耦

抓取、标准化、去重和规则过滤即使没有 AI 也能运行。AI 失败时，岗位仍然进入待评审队列，之后可以重试，而不是丢失整次扫描结果。

### 1.3 用户决定优先

用户设置的 `shortlisted`、`ignored`、`applied` 等状态不会被后续扫描覆盖。后台任务只能补充岗位信息和刷新最后发现时间，不能撤销用户决定。

### 1.4 硬去重与软提示分离

- 有可靠来源 ID 或相同规范 URL：作为确定重复处理；
- 只有公司和标题相似：标记为“可能重复”或“可能已经申请”，交给用户确认。

本项目不应直接以原始 URL 作为唯一标识，而应同时保存来源岗位 ID、规范 URL 和基于公司、标题、地点生成的岗位指纹。

### 1.5 用户反馈校准

当用户认为结果错误时，该项目保存原分数、修正分数和原因，供后续判断参考。本项目可以将反馈进一步结构化：

- 用户最终决定；
- 不同意的规则或 AI 判断；
- 纠正原因；
- 修正后的资格、偏好或匹配结论；
- 当时使用的档案、规则和模型版本。

### 1.6 私有运行数据与代码分离

职业档案、偏好、扫描结果和反馈数据不会提交到 Git，仓库只提供示例配置。本项目应继续执行这一原则，并使用数据库保存真实运行数据。

### 1.7 首次配置向导

该项目会引导用户配置个人背景、语言、工作许可、搜索方向、目标公司和扫描时间。本项目可以将其设计成网页 onboarding，并允许用户之后修改和版本化。

## 2. 不应直接照搬的部分

- `job-scan` 是 Claude Code 插件，本项目目标是标准全栈应用，不能依赖单一 AI 工具；
- JSONL 适合脚本原型，但不适合关系查询、迁移、事务和后续扩展，本项目应使用关系型数据库；
- 固定的评分上限缺乏验证依据，第一版应优先展示资格、能力、偏好和风险等独立维度；
- Regex gate 可能误判，必须展示命中证据并允许用户覆盖；
- 直接抓取公司页面容易受页面变化和使用条款影响，应优先使用公开 API、ATS feed、用户授权邮件和手动导入。

## 3. `job-scan` 的瑞典数据源方案

该项目的主要数据源不是 LinkedIn，而是瑞典公共就业服务相关的 **JobTech JobSearch API**：

```text
GET https://jobsearch.api.jobtechdev.se/search
```

它按照职业类别、城市和关键词查询，并返回结构化岗位数据，例如：

- 稳定岗位 ID 和网页链接；
- 公司、标题、地点和完整描述；
- 结构化职业分类；
- 雇主声明的必需语言。

项目随后进行分页、标准化和链接去重。目标公司 Career Page 只是补充来源，不是唯一基础；LinkedIn 也不是核心依赖。

这种方案的关键优势不是“爬得更多”，而是先找到一个合法、稳定、结构化的公共岗位源作为系统基线。

## 4. 爱尔兰是否有类似方案

### 4.1 JobsIreland 与 EURES

[JobsIreland](https://jobsireland.ie/) 是爱尔兰 Department of Social Protection 管理的公共就业服务，但目前没有找到面向开发者、具有稳定公开契约的 JobsIreland 职位 API。

不过，JobsIreland 官方说明：

> 所有发布在 JobsIreland 的付费岗位也会出现在 EURES。

[EURES](https://eures.europa.eu/) 是欧洲就业服务门户，可以按国家搜索爱尔兰岗位。因此，最接近瑞典 JobTech 思路的候选方案是：

```text
JobsIreland 雇主岗位
        ↓
      EURES
        ↓
按 Ireland + 关键词获取岗位
```

EURES 网页当前使用的搜索接口包括：

```text
POST https://europa.eu/eures/api/jv-searchengine/public/jv-search/search
GET  https://europa.eu/eures/api/jv-searchengine/public/jv/id/{id}
```

可以使用 Ireland 地区代码以及 `software engineer`、`backend developer`、`graduate software` 等关键词进行查询。

需要注意：这些接口虽然可从公开 EURES 门户访问，但目前找到的 API 文档是社区根据网页请求整理的，并非欧盟提供的正式公共 API 契约。它适合做技术验证，但在正式采用前必须确认使用条款、限流、字段稳定性和长期可用性。

### 4.2 公司 ATS 的公开职位接口

第二类可靠来源是公司用于 Career Page 的 ATS 公共接口：

- **Greenhouse Job Board API**  
  `GET https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs?content=true`
- **Lever Postings API**  
  `GET https://api.lever.co/v0/postings/{site_slug}?mode=json`  
  欧盟区域也可能使用 `https://api.eu.lever.co/`。

这些 GET 接口专门用于读取企业公开发布的职位，通常不需要身份验证。可以维护一份爱尔兰目标公司清单，识别其 ATS 类型和 board token，然后定期拉取并只保留爱尔兰岗位。

这种方式覆盖面不如大型招聘网站，但数据质量高、来源明确，尤其适合重点公司监控。

### 4.3 用户授权的 Job Alert 邮件

LinkedIn、IrishJobs、Indeed 等平台可以继续由用户正常创建 Job Alert。系统只读取用户明确授权的邮箱或转发邮件，从邮件中提取岗位链接和基本信息，再访问原始公开职位页。

这不是绕过平台抓取，而是处理用户自己收到的通知。它可以作为覆盖商业招聘平台的重要补充，但仍需检查邮件和链接的使用限制。

### 4.4 手动 URL 或 JD 导入

手动粘贴链接或 JD 应始终保留为可靠基线。任何自动来源遗漏的岗位都可以进入相同的标准化、去重和判断流程。

### 4.5 不作为第一阶段主数据源

- 直接自动登录或大规模抓取 LinkedIn；
- 依赖反爬代理、验证码绕过或第三方 LinkedIn scraper；
- 未确认条款就抓取 JobsIreland、IrishJobs 或 Indeed；
- 将搜索引擎结果当成稳定岗位数据库。

## 5. 推荐的数据源优先级

### 第一层：Phase 1 基线

1. 手动 URL / JD 导入；
2. EURES Ireland 技术验证；
3. 5–10 家目标公司的 Greenhouse 或 Lever 适配器。

### 第二层：Phase 2 扩展

4. 用户授权的 Job Alert 邮件；
5. 更多公开 ATS 适配器；
6. 经条款和稳定性评估后的其他爱尔兰来源。

### 第三层：暂缓

7. JobsIreland 或 IrishJobs 页面解析；
8. 任何需要模拟登录或对抗反爬机制的数据源。

## 6. 建议的首个数据源实验

先写一个一次性 EURES 可行性验证，而不是立即把它做成正式功能：

1. 查询 Ireland 的 Software Engineering 岗位；
2. 验证分页、更新时间、详情字段和稳定 ID；
3. 随机检查 30 个岗位是否真实有效、是否仍可申请；
4. 检查其中多少来自 JobsIreland，以及对初级软件岗位的覆盖率；
5. 连续运行七天，观察重复、失效、限流和字段变化；
6. 记录使用条款与风险评估。

若验证通过，EURES 可以作为爱尔兰公共岗位基线；若覆盖不足，则仍可保留它，同时用目标公司 ATS 和 Job Alert 邮件补充，而不需要依赖 LinkedIn 爬虫。

## 7. AI 域预留（经验贴 + Company-forward）

**状态：** 想法 / **非 M1**；不进当前 `1.req` / `2.design`。  
**归类（2026-08-09 确认）：** 同背景经验贴与 Company-forward **统一归入 AI 域**（预留模块），不再当作独立 Job / Insights 产品域推进。

### 7.0 预留模块说明与开放问题

| 项 | 约定 |
|---|---|
| 模块名（草案） | `ai`（Delta 日后可落为 `1.req/ai` / `2.design/server/ai`；**当前仅本文预留，未建目录**） |
| 已确定 | 经验贴、Company-forward 的产品意图与合规边界记在本域下；二者都不是 Job 主数据源 |
| 仍在讨论 | **仅做 AI API 接入**（Consent + provider 调用，无本地语料库） vs **AI 知识库**（本地收藏/公司人脉笔记等可检索语料，可选再喂给模型） |
| 与 M1 | AI 默认关闭；未选定上列形态前，不实现本域、不改 Job Inbox / Roadmap 基线模板 |

选定「仅 API」或「知识库」后，再决定是否把本节约草稿升格进 Delta 需求。

### 7.1 同背景网友经验贴 / 感受贴（Reddit 等）

#### 7.1.1 要解决什么问题

职位列表只回答「有什么岗」；同背景网友的经验贴回答「像我这样的人实际怎么走过来」——例如：

- 一年制硕士 + Stamp 2 / 1G 时间线下的求职节奏；
- Graduate / Junior / Internship 在爱尔兰大厂与本地公司的真实门槛；
- CV、OA、HR、技术面常见坑与准备顺序；
- 签证、租房、薪资预期、拒信后的心态调整。

这类内容适合作为 **AI 域情境语料**（亦可日后供 Roadmap / Action / Behavioral 引用），不是 Job 主数据源：不替代 FreeHire/JobSpy，也不自动变成可申请职位。

#### 7.1.2 目标平台（优先公开可读）

| 平台 | 典型社区 / 入口 | 内容类型 | 备注 |
|---|---|---|---|
| Reddit | `r/ireland`、`r/MovingToIreland`、`r/IrishJobs`、`r/cscareerquestionsEU`、`r/ExperiencedDevs`、学校相关 sub | 经历、吐槽、时间线、问答 | 有公开 JSON / 搜索；需遵守 ToS 与限流 |
| Blind / Levels 类 | 公司内部向讨论 | 薪资、流程感受 | 多为登录墙，不适合第一阶段自动抓 |
| 小红书 / 知乎 / 即刻 | 爱尔兰留学求职话题 | 中文经历贴 | 条款与反爬差异大；宜手动收藏或用户粘贴 |
| LinkedIn 帖文 | 公开帖 | 经验分享 | 不作为自动抓取主源（与 JobSpy 岗位策略分离） |
| Discord / Slack 求职群 | 私密 | 实时经验 | 仅用户主动粘贴摘要，系统不爬私群 |

若走知识库形态，第一阶段建议：**Reddit 公开帖 + 用户手动粘贴链接/全文** 作为双通道；其它平台只做「用户导入」。

#### 7.1.3 「同背景同状态」怎么匹配

不要用黑箱「相关性分数」直接排序。先用 Profile 做可解释过滤，再展示帖子：

1. **硬标签（来自 Profile）**：目标地区 Ireland、职级 Intern/Graduate/Junior、许可类型 Stamp 2/1G、教育阶段（在读/将毕业）、目标角色（SWE 等）；
2. **帖子侧标签**：作者自述签证/学校/职级、发帖时间、主题（签证 / CV / 面试 / 拒信 / offer）；
3. **匹配规则**：标签交集越多越靠前；缺标签标为「背景未确认」；过旧帖默认降权但可查看；
4. **用户校正**：用户可标记「与我相关 / 不相关」，只校准展示，不覆盖 Profile。

示例检索意图（人工或 API 查询词，非最终实现）：

```text
Ireland graduate software engineer Stamp 1G
Ireland masters internship visa job search
Dublin junior developer rejection OR offer
Moving to Ireland Chinese OR non-EU software job
```

#### 7.1.4 建议的产品形态（AI 域内；相对 Job 独立）

| 实体（草案） | 含义 |
|---|---|
| `PeerPost` | 一条经验/感受贴：标题、摘要、原文链接、平台、抓取/导入时间、作者背景标签 |
| `PeerPostSnapshot` | 原文快照（防删帖后无法追溯；敏感字段按同意策略） |
| `PeerMatchEvidence` | 为何推给当前用户：命中了哪些 Profile 标签 |
| `PeerPostDecision` | 用户：已读 / 收藏 / 不相关 / 生成 Roadmap 备忘 |

流程建议（偏「知识库」草案；若最终仅 API 接入则本节实体可缩减为调用上下文，无持久语料）：

```text
公开搜索或用户粘贴 URL
        ↓
保存链接 + 可选全文快照
        ↓
打标签（规则优先；AI 可选且需同意）
        ↓
按 Profile 过滤 / 排序
        ↓
用户阅读 → 可一键「据此加一条 Roadmap 备忘或 Action」
```

#### 7.1.5 接入边界（必须遵守）

- **不做**：自动登录 Reddit、绕过验证码、批量抓取私密社区、把帖子当「职位」写入 Jobs Inbox；
- **优先**：官方/公开可读接口、RSS、用户粘贴 URL、用户导出；
- **AI**：默认关闭；若用于摘要、标签或问答，须走全局 AI Consent，且不得编造作者经历；
- **引用**：界面展示来源链接与抓取时间；导出备份时可含快照，日志不落全文；
- **与现有产品关系**：归属 **AI 域预留**；若进 Roadmap，只追加用户确认的任务，不静默改基线模板。

#### 7.1.6 首个可行性实验（建议）

1. 手工收集 20～30 条 Reddit 帖：覆盖 Stamp、Graduate 求职、拒信、offer 四类；
2. 用当前 Profile 字段做标签匹配表，看「同背景」是否可解释；
3. 试 Reddit 公开搜索/JSON 是否稳定、限流如何；
4. 评估删帖率：是否必须保存快照；
5. 再决定：长期「用户收藏夹 + 轻量推荐」（知识库一侧），还是仅作 AI 调用时的临时上下文。

### 7.2 Company-forward approach（公司优先）

**目标：** 先拿到一份 **company 清单**，再沿人脉链推进，而不是只从 Job Inbox 正向刷岗。作为 AI 域内的策略/语料（公司与触达笔记），与 Job-forward 互补。

```text
Company list
    → 找 referral（内推 / 熟人引荐）
    → 找 connect（可触达连接：校友、前同事、二度人脉等）
    → recommend（请对方推荐到具体团队或岗位）
```

| 现有 Job-forward（M1） | 本策略 Company-forward（AI 域） |
|---|---|
| 岗位发现 → Gate/Rank → Shortlist → 申请 | 定目标公司 → 人脉链 → 内推 / 推荐 |

**边界（与现有原则一致）：** 不自动登录 LinkedIn、不自动发消息；清单与触达记录若落地，宜本地笔记/CRM 式并受 AI Consent 约束。

**待澄清：** 公司清单来源（手动 / 从已抓岗位聚合 / 外部名单）；与 Home Action、Job Shortlist 如何汇合；在「仅 API」形态下是否只作提示词材料、不建持久实体。

## 8. 参考资料

- [job-scan README](https://github.com/1carusalwayswa/job-scan)
- [Swedish JobTech JobSearch API](https://jobsearch.api.jobtechdev.se/)
- [JobsIreland](https://jobsireland.ie/)
- [JobsIreland 对 EURES 的说明](https://jobsireland.ie/en-US/EmploymentSupports-Employer)
- [EURES Ireland 搜索](https://europa.eu/eures/portal/jv-se/search?lang=en&locationCodes=ie)
- [社区整理的 EURES API 文档](https://github.com/rorar/eures-api-documentation)
- [Greenhouse Job Board API](https://developers.greenhouse.io/job-board)
- [Lever Postings API](https://github.com/lever/postings-api)
- [Reddit API 文档](https://www.reddit.com/dev/api/)（正式接入前核对 ToS 与限流）
- 示例社区：`r/MovingToIreland`、`r/ireland`、`r/cscareerquestionsEU`

---

## 9. Enterprise 展示面 · 待办（To-Do）

**写入日期：** 2026-09-10  
**目的：** 对照 enterprise 常见栈，补齐 portfolio / 面试可讲的工程成熟度；与 [`delta/SRS/progress-log.md`](delta/SRS/progress-log.md) §2.2 收尾项、`DEVELOPMENT_PLAN.md` Phase 6 对齐。  
**GitHub 仓库：** [Kylecodingwood/JobHelper](https://github.com/Kylecodingwood/JobHelper) — 待办落地后在此仓 PR / push；CI 优先用 **GitHub Actions**（与 GitHub 原生集成），Jenkins 可作为 ADR 对比项文档化，不必双轨维护。

### 9.1 现有单元测试（6 个文件）

根目录：`jobAssitant/backend/src/test/java/com/jobhelper/`

| 文件 | 覆盖域 |
|---|---|
| `BackendApplicationTests.java` | Spring 上下文加载 smoke test |
| `action/application/ActionPriorityServiceTest.java` | Home / Action 优先级 |
| `job/application/GateRankServiceTest.java` | Jobs Gate/Rank |
| `job/application/GateRankAndAnchorTest.java` | Gate/Rank + Anchor |
| `job/application/DuplicateUrlNormalizeTest.java` | URL 去重规范化 |
| `roadmap/domain/GrsAnchorResolverTest.java` | Roadmap GRS anchor（历史域逻辑） |

**缺口：** 无集成测、无前端 E2E；**无 CI 自动跑上述测试**（push 到 GitHub 不会触发 build）。

### 9.2 当前 vs Enterprise（差距摘要）

| 维度 | 现状 | Enterprise 常见 |
|---|---|---|
| CI/CD | ❌ 无 `.github/workflows` | GitHub Actions / Jenkins；PR 门禁 |
| 编排 | ✅ Docker Compose | K8s + Ingress + HPA |
| 消息 | ⚠️ DB Outbox + 进程内 poll | RabbitMQ / Kafka + 独立 worker |
| 缓存 | ❌ | Redis（热点读、锁、Session） |
| 可观测 | 普通 log | Actuator、Prometheus、结构化日志 |
| 认证 | 单用户本地 | OAuth2 / JWT |
| 对象存储 | CV 本地 `./data/cv` | S3 / MinIO |
| API 文档 | 手写 `api-contract.md` | OpenAPI / Swagger |
| Secrets | compose 明文密码 | `.env` + GitHub Secrets + K8s Secret |

**已有、可演进叙事：** Flyway、Outbox 表、`OutboxPublisher` → 升级到 MQ 是自然路径，不是重写。

### 9.3 待办清单（按优先级）

#### Tier 1 — 必做（面试 ROI 最高）

- [ ] **GitHub Actions CI**：backend `./mvnw test` + package；frontend `npm run lint` + `npm run build`；可选 Docker build 校验
- [ ] **测试底线**：Profile / Jobs Gate-Rank / Outbox→Action 至少各 1 条集成测；扩展现有 6 个单测覆盖边界
- [ ] **Spring Actuator**：`/actuator/health`、`/metrics`（Compose 暴露或文档说明）
- [ ] **架构 README**：Compose 架构图 + ADR（Outbox 为何先进程内、何时上 MQ/K8s）
- [ ] **package-lock 修复**：Docker 内可 `npm ci`（见 `frontend/Dockerfile` 注释）
- [ ] **progress-log §2.2 文档项**：SRS 正文 reconciliation；`prototype/roadmap.html` 标 ARCHIVED；删 Roadmap 旧 generate/recompute 死代码

#### Tier 2 — Infra 展示（Redis / MQ / 存储）

- [ ] **Redis**：`docker-compose` 加 `redis`；LeetCode 题面 / Home BFF 短 TTL 缓存（Spring Cache）
- [ ] **RabbitMQ（或同类）**：Outbox Publisher 改为发 MQ；独立 `worker` 容器消费 → `ActionProjector`
- [ ] **MinIO**：CV 文件对象存储，替代纯本地 `./data/cv`（Compose profile 可选）
- [ ] **JobSync 异步化**：FreeHire / JobSpy 长任务进队列，API 立即返回 runId

#### Tier 3 — 部署与「企业感」

- [ ] **K8s manifests / Helm**：backend、frontend、worker、redis、rabbitmq（本地 kind/minikube 演示即可）
- [ ] **CD**：main 分支 build 镜像 push **GHCR**（`ghcr.io/kylecodingwood/jobhelper-*`）
- [ ] **OpenAPI**：从 Controller 生成 Swagger UI；与 `api-contract.md` 交叉引用
- [ ] **多环境配置**：`application-dev.yml` / `prod` + 环境变量；compose override 示例

#### Tier 4 — 可选加深

- [ ] **Jenkinsfile**：与 GHA 平行示例 + ADR「为何生产选 GHA」
- [ ] **Prometheus + Grafana**：docker compose profile
- [ ] **简单 JWT 登录**：M2 多用户预留（progress-log 明确 M1 不做 SaaS，但可 ADR 预留）
- [ ] **Playwright E2E**：Home → Jobs 一条 happy path
- [ ] **依赖 / 安全扫描**：CI 内 `npm audit`、OWASP dependency-check

### 9.4 GitHub 相关备忘

| 项 | 说明 |
|---|---|
| 远程 | `git@github.com:Kylecodingwood/JobHelper.git` |
| 贡献图 | 本仓 `.git/config` 已设 `user.email = Kylezjyno1@gmail.com`；**仅本仓**生效，其它项目建议 `git config --global user.email` |
| CI | 待 Tier 1：添加 `.github/workflows/ci.yml` 后，PR 页面会显示 check 状态 |
| 镜像 / CD | 可选 GHCR + GitHub Actions deploy workflow；Secrets 存于 repo Settings → Secrets |
| 展示 | README 徽章：`build` / `tests`（CI 就绪后）；链接 live demo（若部署 Railway/Fly.io） |

### 9.5 建议实施顺序

```text
GitHub Actions + 测试 + Actuator
        ↓
Redis 缓存
        ↓
Outbox → RabbitMQ + worker 容器
        ↓
K8s 编排 + GHCR CD
        ↓
OpenAPI / E2E / 可观测（按需）
```

**原则：** 每项能讲清「M1 为什么需要 / 为什么还没上」；避免空堆 Jenkins/K8s 而无测试与架构说明。
