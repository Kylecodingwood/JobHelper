# 后端编码计划（M1）

## 1. 工程骨架

- Spring Boot 3.x、Java 17+、PostgreSQL、Flyway  
- 模块或单仓多包：`profile` / `roadmap` / `job` / `action` / `shared`  
- Python JobSpy：独立脚本进程，由 `job.adapter.jobspy` 调用（已有 `scripts/datasource/jobspy_fetch.py` 可复用思路）

## 2. 域实现顺序（纵向切片）

| 序 | 切片 | 设计入口 | 完成标准 |
|---|---|---|---|
| 1 | Profile CRUD + validate + field-usage | `server/profile/*` | PUT/GET/404 onboarding；乐观锁 409 |
| 2 | Profile recompute + Roadmap generate/merge Port | profile-workflow-recompute、roadmap-api | preview→confirm；SYSTEM_INCOMPLETE_ONLY |
| 3 | Job sources + sync + FreeHire/JobSpy | job-workflow-sync、job-api §3 | SourceRun + diagnostics；06:00 调度可后接 |
| 4 | Job normalize + Gate/Rank + Inbox API | job-workflow-gate-rank、job-api §1 | 列表默认隐藏 FAILED；override/status |
| 5 | Action 投影 + Home BFF | action-* 、`GET /home` | 事件消费 + 优先级排序 |
| 6 | Profile backup 调度 | profile-workflow-backup | 周日 03:00、保留 4、restore preview |

## 3. 包内惯例

```text
…/{domain}/
  api/          # Controller + Request/Response DTO
  application/  # *Service、workflow 编排
  domain/       # 聚合、不变式（尽量无 Spring）
  infrastructure/  # JPA、Outbox、Scheduler、HTTP/Process 适配器
```

对照：`component-{domain}.puml`、`{domain}-model|crud|api|workflow-*`。

## 4. 优先测试

- Gate WORK_AUTH 缺 `expectedStartDate` → NEEDS_CONFIRMATION  
- Rank 投票 HIGH/MEDIUM/LOW  
- Action priority：deadline > blocker > pin > suggestion  
- Roadmap GRS 锚点；merge 不碰用户任务  
- Job status 不被同步覆盖  

## 5. 非目标（M1）

CV、Behavioral、多租户、桌面壳、Company-forward、经验贴抓取。
