# UI 原型（静态 HTML）— 已确认为实现基线

基于 `2.design` 的可点击线框。**2026-07-31 用户确认：本目录布局与视觉 = 最终前端实现目标。**

正式约束见 [`../frontend/ui-baseline.md`](../frontend/ui-baseline.md)。  
本目录**不是**应用源码；`3.coding` / 前端仓用 Vite + React **按此布局重建**。

## 打开方式

```bash
cd delta/2.design/prototype && python3 -m http.server 8765
```

浏览器：http://localhost:8765/

或直接打开 `index.html`（字体需联网加载 Google Fonts）。

## 页面 ↔ 路由

| 文件 | 路由 | 实现时必须保留的结构 |
|---|---|---|
| `index.html` | `/` | 居中 960 主列；Action 栈 + New Job；为何优先抽屉 |
| `jobs.html` | `/jobs` | 筛选条 + 左列表右详情；52px 行；Gate/Rank 证据 |
| `jobs-sources.html` | `/jobs/sources` | Source 条 + 运行列表/详情分栏 |
| `roadmap.html` | `/roadmap` | **历史**：时间线布局；**现行**见 [`../frontend/roadmap/roadmap/page.md`](../frontend/roadmap/roadmap/page.md)（左夹右表/Document） |
| `profile.html` | `/profile` | 表单 + 右侧重算/备份 |
| `profile-onboarding.html` | `/profile?mode=onboarding` | 品牌标题 + 四步向导 |

共享视觉：`shared.css`（实现仓提取为 design tokens / CSS variables）。
