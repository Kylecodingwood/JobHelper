# Server 设计总览

- 状态：M1 四域 + **CV / Behavioral / LeetCode** 已落地设计文档
- 可运行实现：`jobAssitant/backend`
- 技术栈 ADR：[`../adr/tech-stack.md`](../adr/tech-stack.md)

## 1. 域清单

| Domain | 里程碑 | model | api | 说明 |
|---|---|---|---|---|
| [profile](profile/) | M1 | ✅ | ✅ | 画像基座 |
| [roadmap](roadmap/) | M1→增补 | ✅ 多夹 | ✅ Todo/Company/Document | Pivot：废止模板生成 |
| [job](job/) | M1 | ✅ | ✅ | FreeHire + JobSpy |
| [action](action/) | M1→增补 | ✅ | ✅ | Home `today-priority-v1` |
| [cv](cv/) | 已落地 | ✅ | ✅ | 文件管理 |
| [behavioral](behavioral/) | 已落地 | ✅ | ✅ | 本地域；AI Port 未接 |
| [leetcode](leetcode/) | 2026-08 | ✅ | ✅ | Hot 100 + review + 题面缓存 |

## 2. 跨域说明

Home 今日优先会 **读** Job / Roadmap(Todo·Company) / CV 投影三槽，不反向改对方业务状态（除 Action 行 upsert）。

## 3. 运行默认

| 项 | 值 |
|---|---|
| Job 同步 | 每日 **06:00** 本地时区 |
| 备份 | 每周日 **03:00**，保留 **4** 份 |
| 鉴权 | 本地单用户 |
