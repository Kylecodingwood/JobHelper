> **OBSOLETE (Pivot 2026-08-09)** — 系统模板 / GRS / Profile→Roadmap 重算已废止。见 `delta/3.coding/api-contract.md`。

# Roadmap 模板来源目录

- 状态：已确认的需求输入，不代表外部内容已全文导入
- 核对日期：2026-07-30
- 原则：系统保存自行整理的任务、来源链接和许可信息；除许可证明确允许外，不复制外部全文

## 首版系统模板内容

| 项 | 值 |
|---|---|
| 文件 | [`system-template-v1.md`](system-template-v1.md) |
| 载体 | `RoadmapTemplate` + `TemplateType = SYSTEM` 的首个 `RoadmapTemplateVersion` |
| 语法 | [`markdown-template-spec.md`](markdown-template-spec.md) |
| 任务数 | 20 项，覆盖下方 5 个阶段 |
| 说明 | 任务为基于下表来源的原创归纳；相对锚点与依赖见种子文件 |

发布系统模板时须同时保存：各来源 URL、license/使用依据、来源 `updatedAt`（见 `BR-RDM-001`）。

## 爱尔兰本地权威来源

| 来源 | 用途 | 系统提炼的任务类型 | 使用方式 |
|---|---|---|---|
| [UCD Careers Network](https://www.ucd.ie/careers/) | UCD Career Ready、MyCareer、活动与雇主接触 | 注册 MyCareer、预约 CV Review、参加招聘活动、完成 Career Ready 阶段任务 | 仅保存链接和自行整理的任务 |
| [UCD Stamp 1G Graduate Permission](https://www.ucd.ie/global/currentstudents/stamp1ggraduatepermission/) | 毕业后 Ireland 工作许可时间线 | 准备 transcript、IRP、保险；毕业后申请 Stamp 1G；跟踪续期条件 | 仅保存链接和事实摘要，定期复核 |
| [Irish Immigration — Third Level Graduate Programme](https://www.irishimmigration.ie/my-situation-has-changed-since-i-arrived-in-ireland/third-level-graduate-programme/) | Stamp 1G 官方依据 | 工作许可检查点、到期提醒、就业许可转换准备 | 官方事实来源，不复制全文 |
| [GradIreland CV Guide](https://gradireland.com/careers-advice/cvs-applications-and-tests/graduates-guide-cvs-what-your-cv-should-include) | Ireland Graduate CV 与申请材料 | 建立基础 CV、按岗位定制、Career Service Review、准备 Cover Letter | 仅保存链接和自行整理的任务 |
| [GradIreland Engineering Job Guide](https://gradireland.com/careers-advice/engineering/how-get-job-engineering) | Graduate 招聘季与面试准备 | 提前记录截止日期、参加秋季招聘活动、准备 technical/HR interview | 仅保存链接和自行整理的任务 |
| [Software Space Graduate CV](https://softwarespace.ie/graduate-cv-template/) | Ireland Graduate Software CV | 单页 ATS 友好 CV、突出技能/项目/实习、准备 references | 仅作交叉参考 |

## GitHub 高质量技术准备来源

| Repository | 许可 | 适用范围 | 导入策略 |
|---|---|---|---|
| [jwasham/coding-interview-university](https://github.com/jwasham/coding-interview-university) | CC-BY-SA-4.0 | DSA 与技术面试长期学习计划 | 只提炼适合个人时间线的阶段任务；派生内容保留署名和同许可证要求 |
| [yangshun/tech-interview-handbook](https://github.com/yangshun/tech-interview-handbook) | MIT | Coding、Behavioral、Resume 与面试流程 | 可在保留许可声明后提炼检查项 |
| [ossu/computer-science](https://github.com/ossu/computer-science) | MIT | 发现基础知识缺口后的补充学习 | 不作为完整求职路线，只按技能缺口引用课程 |
| [karan/Projects](https://github.com/karan/Projects) | MIT | 需要补充项目证据时提供题目方向 | 不自动生成虚假项目经历，只创建“选择并完成项目”的任务 |
| [roadmap.sh / developer-roadmap](https://github.com/nilbuild/developer-roadmap) | GitHub API 返回 `NOASSERTION` | Backend/DevOps 技能地图 | 只保存链接；未核实具体内容许可前不复制 |
| [donnemartin/system-design-primer](https://github.com/donnemartin/system-design-primer) | GitHub API 返回 `NOASSERTION` | 轻量 System Design 准备 | 只保存链接；未核实具体内容许可前不复制 |

## 初始系统模板阶段

1. **定位与资格**：确认目标岗位、Ireland 工作许可时间线、招聘季和目标公司。
2. **材料准备**：完成基础 CV、LinkedIn、GitHub/Portfolio、references。
3. **发现与申请**：每日职位同步、处理 New Job、按岗位 Review CV、手动 Shortlist 和申请。
4. **面试准备**：DSA/OA、项目深挖、轻量 System Design、STAR Evidence。
5. **复盘与许可转换**：按申请结果调整准备重点，跟踪 Stamp 1G 与后续 Employment Permit 时间点。

具体任务、相对日期与完成标准由 [`system-template-v1.md`](system-template-v1.md) 承载；本文件记录来源边界与首版发布指向。
