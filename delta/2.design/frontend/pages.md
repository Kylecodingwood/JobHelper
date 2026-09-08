# Frontend 页面总览

- 状态：M1 基线 + **2026-08 增补**（Roadmap 多夹 / Document / Home 今日优先 v1 / LeetCode）
- 约束：`delta/` **主要为文档工作空间**；可运行代码在 `jobAssitant/`
- 脚手架：**Vite + React SPA**
- 技术栈 ADR：[`../adr/tech-stack.md`](../adr/tech-stack.md)
- **UI 基线**：[`ui-baseline.md`](ui-baseline.md)（Nav 已增补，见下）
- **API 契约**：[`../../3.coding/api-contract.md`](../../3.coding/api-contract.md)

## 主导航（现行）

```text
Home / Jobs / Sources / Roadmap / LeetCode / CV / Behavioral
Profile → 右上角圆形头像入口「P」（不再占主导航文字链）
```

## 页面目录

| moduleGroup | page slug | 路由 | 目录 |
|---|---|---|---|
| home | home | `/` | [`home/home/`](home/home/) |
| jobs | jobs | `/jobs` | [`jobs/jobs/`](jobs/jobs/) |
| jobs | jobs-sources | `/jobs/sources` | [`jobs/jobs-sources/`](jobs/jobs-sources/) |
| roadmap | roadmap | `/roadmap` | [`roadmap/roadmap/`](roadmap/roadmap/) |
| leetcode | leetcode | `/leetcode` | [`leetcode/leetcode/`](leetcode/leetcode/) |
| cv | cv | `/cv` | [`cv/cv/`](cv/cv/) |
| behavioral | behavioral | `/behavioral` | [`behavioral/behavioral/`](behavioral/behavioral/) |
| profile | profile | `/profile` | [`profile/profile/`](profile/profile/) |

## 页面关系（现行）

```text
无 Profile → /profile（onboarding）
有 Profile → Home
Home 今日优先 → Jobs / Roadmap（folder） / CV
Roadmap：左夹（todolist|companytracker|document）→ 右表或文档编辑器
LeetCode：独立 Hot 100 + review
```
