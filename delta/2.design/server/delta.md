# Server 设计总览

- 状态：**M1 四域设计正文已填满**（profile / roadmap / job / action）
- **`delta/` 不是代码仓库**：仅约束设计；实现将在独立的后端仓 / 前端仓生成
- 技术栈 ADR：[`../adr/tech-stack.md`](../adr/tech-stack.md)（Java Spring Boot、PostgreSQL、Python JobSpy）

## 1. 域清单（与 `1.req` 对齐）

| Domain | 里程碑 | model | crud | api | workflow |
|---|---|---|---|---|---|
| [profile](profile/) | M1 | ✅ 已填 | ✅ 已填 | ✅ 已填 | ✅ recompute、backup |
| [roadmap](roadmap/) | M1 | ✅ 已填 | ✅ 已填 | ✅ 已填 | ✅ generate |
| [job](job/) | M1 | ✅ 已填 | ✅ 已填 | ✅ 已填 | ✅ sync、gate-rank |
| [action](action/) | M1 | ✅ 已填 | ✅ 已填 | ✅ 已填 | ✅ event-consume |
| cv | M2 | — | — | — | 占位，不建目录 |
| behavioral | M3 | — | — | — | 占位，不建目录 |

## 2. 跨域「infra / 事件」说明（非独立业务域）

各业务域发生「需要别人知道」的变化时，**不直接改对方的表**，而是发一条**领域事件**；Action（及可选 Roadmap 追加）去消费。

```text
例：用户把 Job 标为 Shortlisted
  → Job 域写自己的 JobDecision
  → 同时发事件 JOB_DECISION_CHANGED（只含 id/状态摘要，无敏感正文）
  → Action 域收到后：关闭「去处理新职位」类待办，或更新优先级
```

实现上通常放在后端仓的 **infra 包**（与 profile/job 并列的技术支撑），例如：

- 发事件：事务提交后投递（Outbox）
- 收事件：幂等 Receipt，避免重复创建 Action

需求契约见 [`../../1.req/cross-domain-events.md`](../../1.req/cross-domain-events.md)。设计落点：[`action/action-workflow-event-consume/`](action/action-workflow-event-consume/)。

## 3. 已确认运行默认

| 项 | 值 |
|---|---|
| Job 同步 | 每日 **06:00** 本地时区 |
| 备份 | 每周日 **03:00**，保留 **4** 份 |
| JobSpy Adapter | 归属 **job** 域（`job-workflow-sync`），不单开域 |
| 鉴权 | M1 本地单用户，无账号 |

## 4. 明确不做（v1）

- 经验贴 / Reddit 洞察自动抓取
- 桌面壳（Electron 等）
- CV / Behavioral 完整 API
