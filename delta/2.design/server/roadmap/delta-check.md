# roadmap 域核对

- 对齐需求：[`../../1.req/roadmap/`](../../1.req/roadmap/)
- 前端页面：[`../../frontend/roadmap/roadmap/page.md`](../../frontend/roadmap/roadmap/page.md)（路由 `/roadmap`）
- 跨域事件：[`../../1.req/cross-domain-events.md`](../../1.req/cross-domain-events.md)
- 核对日期：2026-07-31

## 1. 结构完整性

| 检查项 | 状态 | 说明 |
|---|---|---|
| roadmap-model | ✅ | 模板 + Roadmap 聚合；无 Action；Receipt + Outbox |
| roadmap-crud | ✅ | 模板/生成/merge/任务/事件/Actionability 服务 |
| roadmap-api | ✅ | REST `/api/v1/roadmap*` 全 UC 覆盖 |
| roadmap-workflow-generate | ✅ | 首次/merge/模板更新/事件/Action 发布 |
| component-roadmap.puml | ✅ | api→crud→model + workflow + action 消费 |

## 2. 需求对齐

| 需求要点 | 设计落点 | 状态 |
|---|---|---|
| RoadmapTemplate SYSTEM/USER | model §2、API templates | ✅ |
| Markdown 语法 | crud MarkdownTemplateParser、API preview | ✅ |
| 首次生成 preview → confirm | workflow GEN-001、API generate/* | ✅ |
| Profile merge 仅 SYSTEM 未完成 | workflow GEN-002、API recompute/* | ✅ |
| GRS = 最近不晚于毕业的 9/1 | model §4、workflow §5 | ✅ |
| STAMP_EXPIRY 规则 | model §4、AnchorResolver | ✅ |
| 无 Action 实体 | model 边界、workflow GEN-005 | ✅ |
| ROADMAP_* 事件 | model §5、Outbox | ✅ |
| CV/JOB/BEHAVIORAL 追加 | crud EventAppend、Receipt | ✅ |
| Behavioral（非 Interview） | BEHAVIORAL_EVENT origin | ✅ |
| DependencyOverride | model §3.4、API complete | ✅ |
| 系统模板保护性更新 | workflow GEN-003、API template-updates | ✅ |
| UC-RDM-001～007 | crud + API 映射表 | ✅ |

## 3. 跨域一致性

| 检查项 | 状态 | 说明 |
|---|---|---|
| Profile Recompute ROADMAP 链 | ✅ | Profile confirm → RoadmapRecomputePort |
| PROFILE_* 消费 | ✅ | 登记重算建议；执行仍 preview confirm |
| Action 消费 ROADMAP_* | ✅ | cross-domain-events §4.2 |
| system-template-v1 种子 | ✅ | seedSystemTemplateIfAbsent |
| markdown-template-spec | ✅ | 解析错误码 RDM-MD-* |

## 4. 前后端 API 闭环（待前端 api.md 填充时核对）

| 前端 `/roadmap` 能力 | 后端 API | 状态 |
|---|---|---|
| 单页路线图展示 | GET `/api/v1/roadmap` | ✅ |
| 模板导入（小按钮） | POST templates/user/preview、user | ✅ |
| 首次/重算预览确认 | generate/*、recompute/* | ✅ |
| 任务编辑/完成/覆盖依赖 | PATCH tasks、POST complete | ✅ |
| 可执行性摘要 | GET `/roadmap/actionability` | ✅ |
| 系统模板更新 | template-updates/* | ✅ |
| 跳转 Home Actions | actionability.homeActionsHint | ✅ |

## 5. PlantUML

| 文件 | 状态 |
|---|---|
| domain-roadmap.puml | ✅ |
| service-roadmap.puml | ✅ |
| api-roadmap.puml | ✅ |
| workflow-roadmap-generate.puml | ✅ |
| component-roadmap.puml | ✅ |

## 6. 待实现阶段（非设计缺口）

- 领域事件 Inbox 投递器（Job/CV/Behavioral → RoadmapEventAppendService）
- `GET /api/v1/actions` Action 域 API（Home 页）
- 模板 SYSTEM 版本发布运维脚本
