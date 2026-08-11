# Frontend 页面总览

- 状态：M1 页面设计已完成；**UI 布局以原型为验收基线（已确认）**
- 约束：`delta/` **仅为文档工作空间**，前后端代码将分仓生成，不在此目录落代码
- 脚手架（已确认）：**Vite + React SPA**
- 技术栈 ADR：[`../adr/tech-stack.md`](../adr/tech-stack.md)
- **UI 基线（实现必须遵守）**：[`ui-baseline.md`](ui-baseline.md) ← 对照 [`../prototype/`](../prototype/)
- **API 契约**：以后端为准；各页 `api.md` 已对齐（2026-08-09）；见 [`../../3.coding/api-contract.md`](../../3.coding/api-contract.md)

## 主导航

```text
Home / Jobs / Roadmap / Profile
（CV M2 / Behavioral M3：导航占位「即将推出」）
```

布局与视觉以 `prototype/` 为准（全局 sticky Nav + 品牌 Job Helper + 纸感背景），详见 ui-baseline。
## 页面关系

```text
无 Profile → /profile（onboarding 模式）
有 Profile → Home
Home Action → Jobs 详情侧栏 / Roadmap 任务焦点
Profile 重算确认 → Roadmap 预览 / Job Gate·Rank 刷新
Jobs 同步诊断 → jobs-sources
Roadmap 模板/导入 → 主页内小按钮打开抽屉/对话框（非独立路由）
```

## 页面目录与需求跟踪

| moduleGroup | page slug | 路由（拟定） | UC | 目录 |
|---|---|---|---|---|
| home | home | `/` | UC-ACT-001～005 | [`home/home/`](home/home/) |
| jobs | jobs | `/jobs` | UC-JOB-002～007（列表+详情侧栏，尽量多展示岗位） | [`jobs/jobs/`](jobs/jobs/) |
| jobs | jobs-sources | `/jobs/sources` | UC-JOB-001、008、009 | [`jobs/jobs-sources/`](jobs/jobs-sources/) |
| roadmap | roadmap | `/roadmap` | UC-RDM-001～007（单页路线图；模板等用按钮打开） | [`roadmap/roadmap/`](roadmap/roadmap/) |
| profile | profile | `/profile` | UC-PRO-001～007 | [`profile/profile/`](profile/profile/) |

**不做（v1）：** 经验贴 / Reddit 洞察域。

## 调度默认（已确认）

- Job 每日同步：**06:00** 本地时区
- 备份：每周日 **03:00**，保留 **4** 份，路径约定由实现仓配置（文档记为 `./data/backups`）
