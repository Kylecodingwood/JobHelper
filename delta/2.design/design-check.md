# Design 阶段总检

当前状态：**M1 `2.design` 完成；前后端 API 已以后端为准对齐；`3.coding` 映射与规约已写入。应用源码在独立仓实现。**

## 已确认决策

| 项 | 决策 |
|---|---|
| delta 角色 | 仅文档约束，不是代码仓；前后端分仓后续生成 |
| 前端 | Vite + React SPA |
| **UI 布局** | **以 [`prototype/`](prototype/) 为最终布局与视觉基线**（[`frontend/ui-baseline.md`](frontend/ui-baseline.md)） |
| **API 契约** | **以后端 `*-api` 为准**；前端 `api.md` 已对齐（2026-08-09）；优化见 [`../3.coding/api-contract.md`](../3.coding/api-contract.md) |
| Jobs | `jobs` 列表+详情侧栏（高密度）+ `jobs-sources` |
| Roadmap | 单页时间线；模板/导入经小按钮 |
| 经验贴 | v1 不做 |
| 同步 / 备份 | 每日 06:00；周日 03:00 备份，保留 4 |

## 规范符合性

- [x] 前端 5 个 M1 页目录，各含 8 类文件 + PlantUML
- [x] 后端四域 `*-model` / `*-crud` / `*-api` / `*-workflow-*` 正文已写
- [x] 各域 `delta-check.md` / `component-*.puml` 已有
- [x] `pages.md` UC 矩阵与目录一致
- [x] 前后端 API 路径对齐（前端按后端改写 + 调用优化）
- [x] `3.coding` README / specification / coding-plan / api-contract

## 产物索引

| 层 | 入口 |
|---|---|
| 总览 | [`server/delta.md`](server/delta.md)、[`frontend/pages.md`](frontend/pages.md) |
| **UI 原型 / 基线** | [`prototype/`](prototype/)、[`frontend/ui-baseline.md`](frontend/ui-baseline.md) |
| **Coding 映射** | [`../3.coding/README.md`](../3.coding/README.md) |
| 前端 | [`frontend/home/home/`](frontend/home/home/)、[`jobs/jobs/`](frontend/jobs/jobs/)、[`jobs/jobs-sources/`](frontend/jobs/jobs-sources/)、[`roadmap/roadmap/`](frontend/roadmap/roadmap/)、[`profile/profile/`](frontend/profile/profile/) |
| 后端 | [`server/profile/`](server/profile/)、[`roadmap/`](server/roadmap/)、[`job/`](server/job/)、[`action/`](server/action/) |
| 技术栈 | [`adr/tech-stack.md`](adr/tech-stack.md) |

## 不做（已记录）

- 在 `delta/` 内生成应用源码
- CV / Behavioral 完整设计（M2/M3）
- 桌面壳、经验贴自动抓取
