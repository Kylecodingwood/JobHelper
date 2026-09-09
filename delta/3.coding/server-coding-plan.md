# 后端编码计划

> **状态 2026-09-08**：M1 域已实现（Flyway V17）；收尾见 [`../SRS/progress-log.md`](../SRS/progress-log.md) §2.2。

## 1. 工程骨架

- Spring Boot 3.x、Java 17+、PostgreSQL、Flyway  
- 包：`profile` / `roadmap` / `job` / `action` / `cv` / `behavioral` / `leetcode` / `shared`  
- 实现仓：`jobAssitant/backend`

## 2. 域实现顺序（含已落地）

| 序 | 切片 | 状态 |
|---|---|---|
| 1–5 | Profile / Job / Action Home BFF | ✅ 基线 |
| 6 | Roadmap 独立 Todo → **多夹 + Company + Document** | ✅ V13–V17 |
| 7 | CV / Behavioral | ✅ |
| 8 | Home `today-priority-v1` | ✅ `HomeTodayPriorityService` |
| 9 | LeetCode Hot 100 + content cache + review | ✅ V15–V16 |

## 3. 包内惯例

```text
…/{domain}/
  api/
  application/
  infrastructure/
```

## 4. 优先回归

- Roadmap folder kind 校验；删最后一夹拒绝  
- Document PATCH 与 TipTap 前端单向同步约定  
- Home 三槽 refresh 幂等（固定 slot UUID）  
- LeetCode 首次打开拉正文并落库  

## 5. 非目标（仍开放）

多租户、桌面壳、真实 AI provider、Company-forward 推荐链。
