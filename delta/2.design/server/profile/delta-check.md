# profile 域核对

- 对齐需求：[`../../1.req/profile/`](../../1.req/profile/)
- 前端页面：[`../../frontend/profile/profile/page.md`](../../frontend/profile/profile/page.md)（路由 `/profile`）
- 核对日期：2026-07-31

## 1. 结构完整性

| 检查项 | 状态 | 说明 |
|---|---|---|
| profile-model | ✅ | 含 LanguageProficiency、RecomputeRequest、BackupMetadata、Outbox |
| profile-crud | ✅ | Query/Command/Impact/Recompute/Backup 服务 |
| profile-api | ✅ | REST `/api/v1/profile*` 与重算、备份闭环 |
| profile-workflow-recompute | ✅ | 预览确认；ROADMAP merge 仅系统未完成 |
| profile-workflow-backup | ✅ | 周日 03:00、保留 4、恢复 preview |
| component-profile.puml | ✅ | api→crud→model + workflow + 外域 Port |

## 2. 需求对齐

| 需求要点 | 设计落点 | 状态 |
|---|---|---|
| 唯一 Profile + profileVersion | model §2.1、API 409 | ✅ |
| LanguageProficiency → Job Gate | model §2.5、Impact JOB_GATE | ✅ |
| 重算预览确认，不覆盖用户决定 | workflow-recompute §3、API preview/confirm | ✅ |
| ROADMAP merge 语义 | workflow-recompute §3 ROADMAP | ✅ |
| 推迟 → DEFERRED | API defer、状态机 | ✅ |
| PROFILE_* 事件 | model §4、Outbox | ✅ |
| 不直接创建 Action | model 边界、无 Action 实体 | ✅ |
| 备份 weekly + 保留 4 | workflow-backup §1 | ✅ |
| 导出/恢复 preview | workflow-backup §5、API restore-preview | ✅ |
| UC-PRO-001～007 | crud 服务表、API 表 | ✅ |

## 3. 跨域一致性

| 检查项 | 状态 | 说明 |
|---|---|---|
| cross-domain-events §4.5 | ✅ | PROFILE_TIMELINE/GOALS/SKILLS_CHANGED |
| Roadmap 首次生成 preview | ✅ | recompute 链至 roadmap generate |
| GRADUATE_RECRUITMENT_SEASON | ✅ | 由 Roadmap 锚点规则消费 Profile 日期 |
| Behavioral（非 Interview）事件名 | ✅ | Profile 不发布 Interview 事件 |

## 4. 前后端 API 闭环（待前端 api.md 填充时核对）

| 前端 `/profile` 能力 | 后端 API | 状态 |
|---|---|---|
| 档案编辑保存 | PUT `/api/v1/profile` | ✅ 已定义 |
| 影响与重算预览 | POST `.../recompute-requests/{id}/preview` | ✅ |
| 重算确认/推迟 | POST confirm / defer | ✅ |
| 字段用途说明 | GET `/profile/field-usage` | ✅ |
| 备份列表 | GET `/profile/backups` | ✅ |
| 导出 | POST `/profile/backups/export` | ✅ |
| 恢复预览/确认 | GET restore-preview、POST restore | ✅ |

## 5. PlantUML

| 文件 | 状态 |
|---|---|
| domain-profile.puml | ✅ |
| service-profile.puml | ✅ |
| api-profile.puml | ✅ |
| workflow-profile-recompute.puml | ✅ |
| workflow-profile-backup.puml | ✅ |
| component-profile.puml | ✅ |

## 6. 待实现阶段（非设计缺口）

- 错误码全局注册表 → `3.coding/specification.md`
- 备份包加密口令 → 可选 ADR
- Outbox 投递器实现 → infra 包
