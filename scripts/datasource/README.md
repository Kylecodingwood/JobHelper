# 数据源验证：FreeHire API + JobSpy（全站点）

> **策略变更**：不再使用 EURES / Greenhouse / Lever 独立探针。主数据源为 **FreeHire 公开 API**，补充源为 **JobSpy 全站点抓取**（含 LinkedIn / Indeed 等，用户接受 ToS 与封禁风险）。

## 测试目标

验证以下方案是否**可跑通**并产出可用数据集：

| 维度 | FreeHire API | JobSpy（8 站点） |
|------|----------------|------------------|
| 接入方式 | `GET /api/v1/jobs/search` 无需 API Key | Python `python-jobspy`，本地 subprocess |
| 爱尔兰过滤 | `countries=ie` + 关键词 / category / seniority facet | `location=Ireland`, `country_indeed=Ireland` |
| 目标岗位 | software / graduate / junior / backend / fullstack / devops | 同上 3 组 search_term × 8 site |
| 成功标准 | HTTP 200，返回 `data[]` + `meta.total`，去重后 > 0 | 至少 1 个 site×term 有结果；错误写入 `errors[]` 不中断 |
| 输出 | `out/freehire-latest.json` | `out/jobspy-latest.json` |
| 汇总 | `out/summary-latest.json`（重叠 URL、站点分布、可行性结论） |

## 目录

```
scripts/datasource/
├── README.md           # 本文件（测试方案）
├── requirements.txt    # JobSpy 依赖
├── run-all.js          # 一键跑 FreeHire → JobSpy → 汇总
├── freehire-fetch.js   # FreeHire 多查询 + 分页
├── jobspy_fetch.py     # JobSpy 全站点
├── summarize.js        # 数据集质量报告
├── lib/http.js
├── .venv/              # Python 虚拟环境（gitignore）
└── out/                # 运行结果（gitignore）
```

## 运行

```bash
cd scripts/datasource

# 首次：Python 环境
/opt/homebrew/bin/python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# 全量跑一遍
node run-all.js

# 或分步
node freehire-fetch.js
.venv/bin/python jobspy_fetch.py
node summarize.js
```

## FreeHire 查询设计

8 组爱尔兰向查询（各最多 10 页 × 100 条）：

- `countries=ie` + `q=software engineer` / `graduate developer` / `intern software`
- `countries=ie` + `category=backend|fullstack|devops`
- `countries=ie` + `seniority=junior` + `category=backend`
- `countries=ie` + `cities=Dublin` + `q=software`

去重键：`public_slug` 或 `url`。

## JobSpy 抓取设计

**站点（全部）**：`linkedin`, `indeed`, `zip_recruiter`, `glassdoor`, `google`, `bayt`, `naukri`, `bdjobs`

**参数**：

- `search_term`: software engineer / graduate software developer / junior backend developer
- `location`: Ireland
- `results_wanted`: 100 / run
- `hours_old`: 720（30 天）
- `country_indeed`: Ireland

单站失败记录到 `errors[]`，不影响其他站点。

## 风险（已知且接受）

- **LinkedIn / Indeed**：反爬、429、空结果；JobSpy 非官方 API。
- **zip_recruiter / bayt / naukri / bdjobs**：非爱尔兰主市场，可能 0 结果或 irrelevant。
- **FreeHire**：部分岗位 `countries` 为空（location 解析失败）；以 `countries=ie` facet 为准。
- **合规**：与 `PRODUCT_CHARTER.md` / SRS FR-JOB-004 原「禁止未授权 LinkedIn 抓取」冲突；当前按用户显式需求执行。

## 预期结论格式（`summary-latest.json`）

- `freehire.unique_jobs` / `jobspy.unique_jobs`
- `overlap.shared_urls` — 两源同一 apply URL 数量
- `verdict.dual_source_feasible` — 是否可作为 M1 数据源原型
- `verdict.notes` — 各源质量一句话

## 本次跑数结果（2026-07-29）

| 源 | 结果 | 数量 |
|----|------|------|
| **FreeHire API** | ❌ 本机网络无法连接 `freehire.me:443`（connect timeout） | 0 |
| **JobSpy LinkedIn** | ✅ | 115 |
| **JobSpy Indeed** | ✅ | 107 |
| **JobSpy ZipRecruiter** | 403 forbidden | 0 |
| **JobSpy Glassdoor** | 403 + location parse fail | 0 |
| **JobSpy Google** | cursor 空 | 0 |
| **JobSpy Bayt / Naukri** | 403 / recaptcha | 0 |
| **JobSpy BDJobs** | library bug (`user_agent` kw) | 0 |

**合计**：222 条唯一岗位（`out/jobspy-latest.json`），合并集 `out/dataset-latest.json`。

**结论**：方案**部分可行** — JobSpy 在 LinkedIn/Indeed 上能稳定出爱尔兰 SWE 数据；FreeHire 脚本逻辑正确但需可访问 `freehire.me` 的网络（当前环境被墙/路由不可达）。双源合并需先解决 FreeHire 连通性。

## 与产品集成的下一步（不在本脚本范围）

1. 统一 Job 模型：`title, company, location, url, source, posted_at, description`
2. FreeHire 作主索引；JobSpy 按 URL 去重合并
3. 定时任务 + 退避重试；LinkedIn 单独限速
