# Requirement 追踪矩阵

- 基线：[SRS v0.4](../SRS/SRS.md)
- 规则：每个 UC 的完整主/异常流程、业务规则与验收场景位于对应 `usecase-desc.md`；本表提供全局索引。
- 设计映射：页面/API/Service/Test 在 `2.design` 与 `3.coding` 阶段追加，不在需求阶段虚构接口。
- 跨域事件：[cross-domain-events.md](cross-domain-events.md)；全局 AI 同意：[ai-consent.md](ai-consent.md)

## Profile

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-PRO-001 首次配置求职档案 | FR-PRO-001～004、006、007 | [Profile workflow](profile/workflow-desc.md) | Profile、EducationPeriod、WorkAuthorization、TargetRole、LanguageProficiency、SkillEvidence |
| UC-PRO-002 更新求职档案 | FR-PRO-001～007 | 同上 | Profile、ExperienceEvidence、RecomputeRequest |
| UC-PRO-003 确认并执行影响重算 | FR-PRO-005 | 同上 | RecomputeRequest |
| UC-PRO-004 查看档案用途与敏感性 | FR-PRO-006 | 同上 | Profile |
| UC-PRO-005 每周本地自动备份 | FR-PRO-008；SRS §11 | 同上 | BackupSnapshot |
| UC-PRO-006 手动完整导出 | FR-PRO-008；SRS §11 | 同上 | ExportPackage |
| UC-PRO-007 从备份或导出恢复 | FR-PRO-008；SRS §11 | 同上 | BackupSnapshot、ExportPackage |

## Roadmap

> **现行（2026-08）**：多文件夹 Todo / Company / Document。旧 UC-RDM-001～007（模板/生成）废止实现，见 [`roadmap/entity.md`](roadmap/entity.md)、[`roadmap/usecase-desc.md`](roadmap/usecase-desc.md) 文首。

| UC | 主要实体 | 设计/契约 |
|---|---|---|
| UC-RDM-F01 管理文件夹 | RoadmapFolder | `2.design/server/roadmap/*`；`api-contract` |
| UC-RDM-F02 TodoList | RoadmapTodo | 同上 |
| UC-RDM-F03 CompanyTracker | RoadmapCompany | 同上 |
| UC-RDM-F04 Document | RoadmapDocument | 同上 + TipTap FE |

## LeetCode

| UC | 主要实体 | 设计/契约 |
|---|---|---|
| UC-LC-001～004 | LeetCodeProblem、LeetCodeReview | [`leetcode/`](leetcode/)；`2.design/**/leetcode/*` |

## Roadmap（历史索引 · 已废止）

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-RDM-001 管理 Roadmap Template | FR-RDM-001、002、007、009、010 | [Roadmap workflow](roadmap/workflow-desc.md) | RoadmapTemplate、RoadmapTemplateVersion、TemplateTaskDefinition |
| UC-RDM-002 生成基线 Roadmap | FR-RDM-001～003、006、011 | 同上 | Roadmap、GenerationRecord、RoadmapTask |
| UC-RDM-003 管理 Roadmap Task | FR-RDM-003～005 | 同上 | RoadmapTask、TaskDependency |
| UC-RDM-004 覆盖未满足依赖 | FR-RDM-004 | 同上 | DependencyOverride |
| UC-RDM-005 追加领域事件任务 | FR-RDM-003、005、012 | 同上 | DomainEventReceipt、RoadmapTask |
| UC-RDM-006 查看任务可执行性并触发 Action 事件 | FR-RDM-006（事件桥接至 Action 域） | 同上 | RoadmapTask |
| UC-RDM-007 应用系统模板更新 | FR-RDM-007 | 同上 | TemplateApplication |

种子模板见 [`roadmap/system-template-v1.md`](roadmap/system-template-v1.md)；Markdown 语法见 [`roadmap/markdown-template-spec.md`](roadmap/markdown-template-spec.md)。（历史）

## Job

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-JOB-001 同步职位源 | FR-JOB-001、002、004、004a、005、016、017、019、022、023 | [Job workflow](job/workflow-desc.md) | Source、SourceRun、RawPosting、TargetRole |
| UC-JOB-002 快速保存公开职位 URL | FR-JOB-003、005、007、018、019 | 同上 | RawPosting、CanonicalJob |
| UC-JOB-003 标准化、去重、Gate 与 Rank | FR-JOB-006～011、009a～009c、020；FR-PRO-002、005、007 | 同上 | CanonicalJob、RuleEvidence、PossibleDuplicate、LanguageProficiency |
| UC-JOB-004 查看和筛选 Job Inbox | FR-JOB-010～012、014、015、018～021 | 同上 | CanonicalJob、JobDecision、RuleEvidence |
| UC-JOB-005 作出或覆盖职位决定 | FR-JOB-012、013、019～021；FR-ACT-005 | 同上 | JobDecision、RuleEvidence |
| UC-JOB-006 处理可能重复职位 | FR-JOB-007、008 | 同上 | PossibleDuplicate、CanonicalJob |
| UC-JOB-007 刷新岗位有效性并归档 | FR-JOB-014～016 | 同上 | CanonicalJob、RawPosting、JobDecision |
| UC-JOB-008 查看来源运行诊断 | FR-JOB-016、018、022 | 同上 | SourceRun、Source |
| UC-JOB-009 手动重跑来源 | FR-JOB-001、016、017、023 | 同上 | SourceRun |

数据源实测依据见 [`job/datasource-validation.md`](job/datasource-validation.md)。

## CV

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-CV-001 上传并提取 CV | FR-CV-001～003、012 | [CV workflow](cv/workflow-desc.md) | CVDocument、CVVersion |
| UC-CV-002 确认或修正提取文本 | FR-CV-002、003、008、009、012 | 同上 | CVVersion |
| UC-CV-003 发起通用 CV 健康评审 | FR-CV-005、008～011 | 同上 | CVReview、CVSuggestion |
| UC-CV-004 发起岗位定制 CV 评审 | FR-CV-004、005、008～011 | 同上 | CVReview、CVSuggestion |
| UC-CV-005 决定评审建议 | FR-CV-005、006 | 同上 | CVSuggestion |
| UC-CV-006 生成并复制新 CV 版本 | FR-CV-002、007～010、013 | 同上 | CVVersion、AppliedSuggestion |
| UC-CV-007 管理 AI 同意并执行调用 | FR-CV-008、009；SRS §10 | 同上 | AIConsent（全局投影）、AIInvocation |
| UC-CV-008 手动删除 CV 敏感对象 | SRS §8.2、§11.1 | 同上 | SensitiveDeletionAudit 及 CV 聚合 |

本地规则见 [`cv/local-review-rules.md`](cv/local-review-rules.md)。

## Behavioral

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-BHV-001 浏览策展题并管理自定义题 | FR-BHV-001、010 | [Behavioral workflow](behavioral/workflow-desc.md) | BehavioralQuestion；种子 [curated-question-bank.md](behavioral/curated-question-bank.md) |
| UC-BHV-002 建立并复用 STAR Evidence | FR-BHV-002、003 | 同上 | STAREvidence、STAREvidenceRevision |
| UC-BHV-003 编写并保存答案文本版本 | FR-BHV-003、004、009 | 同上 | BehavioralAnswer、AnswerVersion、AnswerEvidenceLink |
| UC-BHV-004 获取结构与具体性反馈 | FR-BHV-005～007 | 同上 | AnswerFeedback、AnswerFeedbackItem |
| UC-BHV-005 决定反馈并创建修订版本 | FR-BHV-004、006、008 | 同上 | AppliedFeedback、AnswerVersion |
| UC-BHV-006 管理 AI 同意与调用 | FR-BHV-005～008；SRS §10 | 同上 | AIConsent（全局投影）、AIInvocation |
| UC-BHV-007 手动删除敏感对象 | SRS §8.2、§11.1 | 同上 | SensitiveDeletionAudit 及 Behavioral 聚合 |

本地规则见 [`behavioral/local-feedback-rules.md`](behavioral/local-feedback-rules.md)。

## Action

| UC | FR | Workflow | 主要实体 |
|---|---|---|---|
| UC-ACT-001 查看今日优先 Action 和新职位 | FR-ACT-001～003、006、007；FR-JOB-012、019、021 | [Action workflow](action/workflow-desc.md) | Action、ActionPriorityEvidence |
| UC-ACT-002 从 Action 进入源对象 | FR-ACT-002、004 | 同上 | Action、TargetRef |
| UC-ACT-003 根据源对象变化同步 Action | FR-ACT-001、002、005、006 | 同上 | ActionGenerationEvidence、DomainEventReceipt |
| UC-ACT-004 置顶、完成、忽略或恢复 Action | FR-ACT-002、005、006 | 同上 | ActionDecision |
| UC-ACT-005 解释 Action 优先级 | FR-ACT-002、003、006、007 | 同上 | ActionPriorityEvidence |

## 图与验收入口

每个域均包含：

- `usecase-diagram.puml`：参与者和 UC 边界；
- `workflow.puml`：主流程、关键分支和失败路径；
- `entity.puml`：聚合、实体和核心关系；
- `usecase-desc.md`：每个 UC 的 Given/When/Then 验收场景。
