# Job 数据源验证记录

- 状态：**Phase 0 完成**（2026-07-29）
- 脚本位置：[`../../../scripts/datasource/`](../../../scripts/datasource/)
- 对应 FR：FR-JOB-001、FR-JOB-002（修订后）

## 1. 策略变更

| 原方案（SRS v0.1） | 现方案（SRS v0.3） |
|--------------------|-------------------|
| EURES（验证通过后） | **弃用** — 噪声高、非爱尔兰 SWE 主市场 |
| 公司 Greenhouse/Lever 独立探针 | **合并进 FreeHire** — FreeHire 聚合 75+ ATS，URL 多指向 Greenhouse |
| 不自动搜索 LinkedIn/Indeed | **JobSpy 全站点** — 用户显式接受 ToS/封禁风险 |

## 2. FreeHire API

**端点：** `GET https://freehire.me/api/v1/jobs/search`

**爱尔兰过滤：** `countries=ie`（ISO-3166 alpha-2），配合 `q`、`category`、`seniority`、`cities` facet。

**验证查询（8 组）：** software engineer、graduate developer、junior backend、backend、Dublin software、fullstack、devops、intern software。

**2026-07-29 结果：**

- 去重后 **1,345** 条
- 查询错误 **0**
- 样例：Twilio *Software Engineer*（Remote - Ireland）→ Greenhouse URL
- 稳定 ID：`public_slug`（如 `software-engineer-twilio-zwmfljnj`）
- 网络注意：部分环境需 VPN 才能连接 `freehire.me:443`

**正式采用结论：** ✅ 作为 M1 **主自动来源**

## 3. JobSpy

**包：** `python-jobspy`（Python venv，`scripts/datasource/.venv`）

**站点（全部尝试）：** linkedin, indeed, zip_recruiter, glassdoor, google, bayt, naukri, bdjobs

**参数：** `location=Ireland`, `country_indeed=Ireland`, 3 组 search_term, `results_wanted=100`

**2026-07-29 结果：**

| 站点 |  fetched | 状态 |
|------|----------|------|
| linkedin | 115 unique | ✅ |
| indeed | 107 unique | ✅ |
| zip_recruiter | 0 | 403 forbidden |
| glassdoor | 0 | 403 |
| google | 0 | cursor 空 |
| bayt / naukri | 0 | 403 / recaptcha |
| bdjobs | — | library bug |

**正式采用结论：** ✅ JobSpy 作为 M1 **补充来源**。全部受支持站点默认尝试；LinkedIn + Indeed 当前可产出数据，其余站点失败仅进入 SourceRun 诊断且可独立停用。

## 4. 去重与合并

- FreeHire ↔ JobSpy：**按规范 apply URL 硬去重**；2026-07-29 样本精确 URL overlap 为 0，不能据此推断无语义重复（两源可能使用不同跟踪参数或落地 URL）。
- FreeHire 内部：`public_slug` 或 `url`
- JobSpy 内部：`job_url`

## 5. 待进入设计阶段确认

- [ ] Source Adapter 统一 RawPosting 字段映射（FreeHire vs JobSpy）
- [x] 默认每日同步一次并支持手动重跑；具体站点限速参数留待设计
- [ ] FreeHire 分页上限（单查询最多 1000 条/次验证配置）
- [x] 用户手动 URL 与自动源按规范 URL 合并；用户提供的 JD/备注不得被同步静默覆盖
