# Delta Requirement 入口


| 项目   | 内容                                             |
| ---- | ---------------------------------------------- |
| 状态   | Requirement 门禁通过（v0.4 冻结决策已同步）              |
| 基线   | [SRS v0.4](../SRS/SRS.md)（2026-07-30）          |
| 范围   | M1–M3：Profile、Roadmap、Job、CV、Behavioral、Action |
| 第一用户 | 本地单用户                                          |


## 1. 单一事实来源

产品级目标、范围和 FR 以 `[../SRS/SRS.md](../SRS/SRS.md)` 为权威来源。本目录不复制完整 SRS，而是将 FR 拆解为可追溯、可测试的用例、流程和实体。

## 2. 里程碑


| 里程碑 | 范围                     | 主要业务域                      |
| --- | ---------------------- | -------------------------- |
| M1  | 个性化准备路线图与职位发现/决策       | Profile、Roadmap、Job、Action |
| M2  | 通用及按岗位进行 CV Review     | CV、Job、Profile、Action      |
| M3  | Behavioral Question 准备 | Behavioral、Profile、Action  |


## 3. Module Group


| Module Group | 中文职责                       | 详细需求入口                       | 当前状态 |
| ------------ | -------------------------- | ---------------------------- | ---- |
| `profile`    | 教育、许可、语言、目标、技能、备份/导出/恢复   | `[profile/](profile/)`       | 已细化  |
| `roadmap`    | 来源模板、Markdown 导入、任务依赖与相对时间 | `[roadmap/](roadmap/)`       | 已细化  |
| `job`        | 来源同步、清洗、Gate、Rank、决定与诊断    | `[job/](job/)`               | 已细化  |
| `cv`         | 文件解析、版本、通用/定向 Review 与建议决策 | `[cv/](cv/)`                 | 已细化  |
| `behavioral` | 题库、STAR Evidence、答案版本和反馈   | `[behavioral/](behavioral/)` | 已细化  |
| `action`     | 跨域行动与 Home 优先级聚合（唯一拥有）    | `[action/](action/)`         | 已细化  |


每个域使用同一组产物：

- `usecase-desc.md` / `usecase-diagram.puml`
- `workflow-desc.md` / `workflow.puml`
- `entity.md` / `entity.puml`

全局 UC ↔ FR ↔ Workflow ↔ Entity 索引见 `[traceability.md](traceability.md)`。  
跨域事件契约见 `[cross-domain-events.md](cross-domain-events.md)`。  
全局 AI 同意见 `[ai-consent.md](ai-consent.md)`。

## 4. 跨域主链路

```text
Profile
  ├─> Roadmap rules ─> Roadmap Task ─事件─> Action
  ├─> Job clean ─> Gate ─> Rank Tier ─> Job Decision ─事件─> Action
  ├─> CV Review <──── Target Job ─────────────────────事件─> Action
  └─> STAR Evidence ─> Behavioral Answer ─────────────事件─> Action
```

## 5. 已冻结的跨域规则

1. **决策优先级**：用户决定高于后台同步、确定性规则和 AI 建议。
2. **职位处理顺序**：抓取 → 标准化/去重 → Gate → Rank Tier → 用户决定。
3. **Gate**：失败岗位保留并默认隐藏；未知进入待确认；缺入职日时工作授权为 `NEEDS_CONFIRMATION`；用户可带原因覆盖。
4. **Rank**：五项因素正/中/负投票——≥3 正且无负=`High`，≥2 负=`Low`，其余=`Medium`；不使用黑箱百分制。
5. **Shortlist**：系统只推荐，必须由用户手动确认。
6. **Roadmap**：系统种子模板 `system-template-v1` + 结构化 Markdown 导入 + Profile/CV/Job/Behavioral 事件追加；招聘季=毕业日前最近 9 月 1 日；生成/重算先预览确认；重算只更新系统未完成任务。
7. **Action**：由 Action 域唯一拥有 Home 待办；优先级 deadline → blocker → user pin → system suggestion；状态无 `IN_PROGRESS`。
8. **AI**：本地规则始终可用；provider / Cursor Skill/SDK 可选且默认关闭；[全局 AI 同意注册表](ai-consent.md)按类别授权并可撤销。
9. **删除**：无自动清理；用户确认影响后级联硬删除敏感正文；STAR Evidence 删除保留答案文本并移除引用。
10. **备份**：每周自动本地备份 + 手动完整 ZIP 导出 + 恢复前影响预览（UC-PRO-005～007）。
11. **搜索词**：默认来自启用中的 `TargetRole`；用户可在 Source 设置中覆盖。
12. **语言**：独立 `LanguageProficiency` 实体供 Gate 使用。

## 6. 非功能验收基线

- 本地累计 10,000 个 CanonicalJob 内，列表、筛选和排序不超过 2 秒。
- 不依赖外部调用的普通页面操作不超过 1 秒。
- 每日职位同步正常情况下不超过 30 分钟；单站失败不阻塞其他站点。
- JobSpy 全站点默认尝试，失败仅进入 SourceRun 诊断。
- 第一阶段本地单用户，无账号、RBAC、公开部署和系统推送。
- 第一版 CV 不做 OCR、不生成最终 PDF/DOCX；Behavioral 不做语音。

## 7. Phase 0 / Requirement 种子已完成

- [x] FreeHire API 爱尔兰 SWE 查询验证（1,345 条去重样本）。
- [x] JobSpy LinkedIn/Indeed 验证（222 条去重样本）。
- [x] 数据源边界与失败表现已确认，见 `[job/datasource-validation.md](job/datasource-validation.md)`。
- [x] Roadmap 来源目录与种子模板，见 `[roadmap/template-sources.md](roadmap/template-sources.md)`、`[roadmap/system-template-v1.md](roadmap/system-template-v1.md)`。
- [x] Markdown 模板语法，见 `[roadmap/markdown-template-spec.md](roadmap/markdown-template-spec.md)`。
- [x] Behavioral 策展题库 v1（24 题），见 `[behavioral/curated-question-bank.md](behavioral/curated-question-bank.md)`。
- [x] 跨域事件契约与全局 AI 同意，见上文链接。

## 8. 设计阶段 TBD

- **技术栈已确认**（见 [`../2.design/adr/tech-stack.md`](../2.design/adr/tech-stack.md)）：Java Spring Boot、PostgreSQL、React、Python（JobSpy）。
- PDF/DOCX 解析库、任务调度钟点细节。
- AI provider、模型和预算；需求仅约束 provider-neutral 接口，候选方向见 `[ai-provider-directions.md](ai-provider-directions.md)`。
- Source Adapter、API、表结构和前端组件的技术契约。
- 每周备份的具体执行钟点与保留份数（功能范围已冻结）。
- 桌面壳（Electron/Tauri）为可选增强，非 M1 范围。
