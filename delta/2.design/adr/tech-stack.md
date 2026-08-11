# ADR-001：技术栈与运行形态

| 项目 | 内容 |
|---|---|
| 状态 | **已确认**（2026-07-30） |
| 决策人 | 项目所有者 |
| 影响范围 | `2.design` / `3.coding`、本地部署与作品集表述 |

## 1. 决策

| 层 | 选型 | 用途 |
|---|---|---|
| 后端 | **Java + Spring Boot** | 领域服务、REST API、Gate/Rank/Roadmap 规则、事件消费、备份调度 |
| 数据库 | **PostgreSQL** | 持久化；本机安装或 Docker，单用户本地库 |
| 前端 | **React** | SPA；对接后端 API；**UI 布局/视觉必须对齐** [`../frontend/ui-baseline.md`](../frontend/ui-baseline.md) 与 [`../prototype/`](../prototype/) |
| 抓取脚本 | **Python** | JobSpy 子进程；与现有 `scripts/datasource/jobspy_fetch.py` 对齐 |
| HTTP 集成 | Java HTTP 客户端 | FreeHire API；超时/重试/幂等在 Adapter 中实现 |
| 调度 | Spring 调度（或等价） | 每日 Job 同步、每周备份；具体钟点设计期量化 |

**明确不做（M1）：** 多租户账号、云部署必选项、自动投递、桌面壳必选项（见 §3）；经验贴自动抓取。

**文档与代码分离：** `delta/` 仅为需求/设计约束工作空间；**后端仓与前端仓分仓**，由后续 coding 阶段按本 ADR 与 `2.design` 生成，不在 `delta/` 内放置应用源码。

## 2. 默认运行形态：本地 Web

```text
浏览器 (React)
    │ HTTP localhost
Java Spring Boot
    ├── PostgreSQL（本机）
    └── 调用 Python 子进程（JobSpy）
```

- 用户打开 `http://localhost:...` 使用应用。
- 后端与数据库都在本机；无公网账号体系。
- 符合 SRS「本地优先 / 第一用户本人」。

## 3. 可选增强：桌面壳（非 M1 必做）

「桌面壳」= 用一个小窗口包住同一个 Web 前端，看起来像独立 App，而不是在浏览器标签里开。

常见做法：

| 方案 | 做法 | 说明 |
|---|---|---|
| **Electron** | Chromium + Node 包一层；加载 `http://localhost` 或打包后的静态前端 | 职场最常见；体积大 |
| **Tauri** | 系统 WebView + Rust 壳 | 更轻；需一点 Rust 工具链 |
| **仅浏览器 PWA** | 可「安装到桌面」快捷方式 | 最简单，仍依赖本机后端进程 |

要点：

- **业务代码不用重写**：还是 React + Spring Boot；壳只负责开窗口、托盘、开机启动等。
- **后端仍要跑**：PostgreSQL + Java 进程；壳不会替代服务器。
- **建议**：M1 先做本地 Web；若以后想要「双击图标打开」，再加 Electron/Tauri 薄壳。

## 4. 为何这样选

- Java：与所有者技能与职场后端岗位匹配，便于作品集讲述领域规则与 API。
- PostgreSQL：比 SQLite 更接近职场；支持后续备份/导出与复杂查询。
- React：前端交付面广；所有者以验收与对接为主，不必先系统学完前端。
- Python：复用已验证的 JobSpy 路径，避免在 Java 内重写爬虫。

## 5. 后果与后续设计项

- 模块按 Spring 分包：`profile` / `roadmap` / `job` / `action` /（M2）`cv` /（M3）`behavioral`。
- ORM 候选：Spring Data JPA；具体实体映射在各域 `server/<domain>/` 设计中展开。
- 前端导航：`Home / Jobs / Roadmap / CV / Behavioral / Profile`（Interview 更名为 Behavioral）。
- PDF/DOCX 解析库、AI provider、备份钟点/保留份数仍为后续 ADR/设计条目。

## 6. 否决的备选（记录）

- 全 TypeScript 全栈：学习曲线对当前技能不最优。
- SQLite：个人够用，但作品集与职场 PostgreSQL 经验更弱。
- 纯 FastAPI：可行，但弱化 Java 作品集叙事。
