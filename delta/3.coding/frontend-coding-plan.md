# 前端编码计划

> **状态 2026-09-08**：8 页已实现；收尾见 [`../SRS/progress-log.md`](../SRS/progress-log.md) §2.2。

## 1. 工程骨架

- Vite + React + TypeScript + React Router  
- 样式：`shared.css` 令牌（Figtree / Fraunces）  
- API：对齐 [`api-contract.md`](api-contract.md)

## 2. 页面实现顺序（含已落地增补）

| 序 | 页 | 要点 |
|---|---|---|
| 1 | 全局壳 + Nav | sticky Nav；Profile 头像；含 LeetCode/CV/Behavioral |
| 2 | Profile | PUT；onboarding |
| 3 | Home | `GET /home`；今日优先三槽展示 |
| 4 | Jobs / Sources | 分栏 Inbox；全局 searchTerms |
| 5 | Roadmap | 多夹 + Todo/Company/Document；TipTap；自动保存 |
| 6 | LeetCode | Hot 100 + review（EN） |
| 7 | CV / Behavioral | 已落地页 |

## 3. Roadmap / Document 注意

- 自动保存队列 + keepalive flush  
- DocumentEditor：**禁止**对每次 autosave 的 `initialHtml` 做 `setContent`  
- ErrorBoundary 包裹编辑器，避免整页白屏  

## 4. 验收

对照 `ui-baseline.md` + `api-contract.md`。
