> **OBSOLETE (Pivot 2026-08-09)** — 系统模板 / GRS / Profile→Roadmap 重算已废止。见 `delta/3.coding/api-contract.md`。

# 定位与资格

- [ ] 确认目标岗位与 Ireland 工作地点偏好
  - templateTaskKey: qualify.target-role
  - anchorType: COURSE_START
  - relativeOffset: P30D
  - dependsOn:
  - completionCriteria: Profile 中 TargetRole 与目标地点已保存且经用户确认
  - priority: CRITICAL
  - description: 对齐 UCD Careers 早期职业规划建议

- [ ] 梳理 Ireland 工作许可时间线（Stamp 2 → Stamp 1G）
  - templateTaskKey: qualify.work-auth-timeline
  - anchorType: GRADUATION
  - relativeOffset: -P180D
  - dependsOn: qualify.target-role
  - completionCriteria: Profile WorkAuthorization 含 Stamp 2 validUntil 及预期 Stamp 1G 申请窗口说明
  - priority: CRITICAL
  - description: 来源 UCD Stamp 1G Graduate Permission；仅事实摘要

- [ ] 标记 Graduate 招聘季目标公司与截止日期
  - templateTaskKey: qualify.recruitment-targets
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P150D
  - dependsOn: qualify.target-role
  - completionCriteria: 至少 5 家目标公司已录入 Job 模块或 Roadmap 备注，含已知 graduate programme 截止日期
  - priority: HIGH
  - description: 参考 GradIreland Engineering Job Guide 招聘季节奏

- [ ] 注册 MyCareer / Career Service 账户
  - templateTaskKey: qualify.mycareer-register
  - anchorType: COURSE_START
  - relativeOffset: P14D
  - dependsOn:
  - completionCriteria: UCD MyCareer（或等价 Career Service 门户）账户可登录且 profile 基本信息完整
  - priority: MEDIUM
  - description: 来源 UCD Careers Network

# 材料准备

- [ ] 建立基础 Graduate CV 草稿
  - templateTaskKey: cv.base-draft
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P120D
  - dependsOn: qualify.target-role
  - completionCriteria: CV 模块存在 v1 草稿：单页、含教育/技能/至少一个项目或实习
  - priority: HIGH
  - description: 参考 GradIreland CV Guide 与 Software Space Graduate CV 结构要点

- [ ] 完善 LinkedIn 与 GitHub/Portfolio 链接
  - templateTaskKey: cv.online-presence
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P105D
  - dependsOn: cv.base-draft
  - completionCriteria: Profile 或 CV 中 LinkedIn URL 与 GitHub/Portfolio 至少一项可访问且与 CV 一致
  - priority: MEDIUM

- [ ] 预约 Career Service CV Review
  - templateTaskKey: cv.career-review
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P90D
  - dependsOn: cv.base-draft
  - completionCriteria: MyCareer 已提交 CV Review 预约并收到确认（或已完成一次 review 会话）
  - priority: HIGH
  - description: UCD Careers Network

- [ ] 准备 2 位学术或实习 references
  - templateTaskKey: cv.references
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P75D
  - dependsOn: cv.base-draft
  - completionCriteria: 两位 referee 姓名、关系与联系方式已私密保存；已告知可能被联系
  - priority: MEDIUM

# 发现与申请

- [ ] 配置 Job 来源同步与每日 Inbox 习惯
  - templateTaskKey: apply.job-sync
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P60D
  - dependsOn: qualify.recruitment-targets
  - completionCriteria: 至少一个 Job Source 已启用且连续 3 个工作日处理 New Job
  - priority: HIGH

- [ ] 完成首批岗位 Gate 与 Shortlist
  - templateTaskKey: apply.first-shortlist
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P45D
  - dependsOn: apply.job-sync, cv.base-draft
  - completionCriteria: Job 模块中至少 3 个岗位状态为 Shortlisted 且 Gate 证据已查看
  - priority: HIGH

- [ ] 按岗位定制 CV 并完成一次定向 Review
  - templateTaskKey: apply.tailored-cv-review
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P30D
  - dependsOn: apply.first-shortlist, cv.career-review
  - completionCriteria: 至少一个 Shortlisted 岗位存在定制 CV 版本且完成 CV Review（通用或定向）
  - priority: HIGH

- [ ] 提交第一份 Graduate 申请
  - templateTaskKey: apply.first-application
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P21D
  - dependsOn: apply.tailored-cv-review
  - completionCriteria: 至少一个 Shortlisted 岗位标记为 Applied 并记录申请日期
  - priority: CRITICAL

# 面试准备

- [ ] 启动 DSA 复习计划（按个人缺口）
  - templateTaskKey: interview.dsa-plan
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P90D
  - dependsOn: qualify.target-role
  - completionCriteria: Roadmap 或学习笔记中列出 4 周 DSA 主题清单并完成首周 3 个主题
  - priority: HIGH
  - description: 提炼自 coding-interview-university；保留 CC-BY-SA-4.0 署名要求

- [ ] 建立 3 条 STAR Behavioral Evidence
  - templateTaskKey: interview.star-evidence
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P60D
  - dependsOn: cv.base-draft
  - completionCriteria: Behavioral 模块存在至少 3 条 STAR Evidence，每条含 Situation/Task/Action/Result
  - priority: HIGH
  - description: 参考 tech-interview-handbook behavioral 章节要点

- [ ] 完成一次 Mock Technical + HR 准备清单
  - templateTaskKey: interview.mock-checklist
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P14D
  - dependsOn: interview.dsa-plan, interview.star-evidence
  - completionCriteria: 自检清单（coding、项目深挖、behavioral、公司研究）已全部勾选或标注豁免理由
  - priority: HIGH
  - description: GradIreland Engineering 面试准备要点

- [ ] 轻量 System Design 阅读（Backend 向）
  - templateTaskKey: interview.system-design-lite
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P30D
  - dependsOn: interview.dsa-plan
  - completionCriteria: 完成 2 个经典题目（如 URL 短链、速率限制）的一页要点笔记
  - priority: MEDIUM
  - description: 仅链接 system-design-primer；未复制全文

# 复盘与许可转换

- [ ] 申请结果复盘并调整准备重点
  - templateTaskKey: close.application-retrospective
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: P30D
  - dependsOn: apply.first-application
  - completionCriteria: 记录至少 3 次申请/面试反馈与下一步调整（技能缺口或材料修改）
  - priority: MEDIUM

- [ ] 准备 Stamp 1G 申请材料包
  - templateTaskKey: close.stamp1g-application
  - anchorType: GRADUATION
  - relativeOffset: P7D
  - dependsOn: qualify.work-auth-timeline
  - completionCriteria: Transcript、IRP、保险证明清单齐备且截止日期已录入 Profile
  - priority: CRITICAL
  - description: UCD Stamp 1G 与 Irish Immigration Third Level Graduate Programme 事实摘要

- [ ] 设置 Stamp 1G / Employment Permit 到期提醒
  - templateTaskKey: close.permit-expiry-reminder
  - anchorType: STAMP_EXPIRY
  - relativeOffset: -P90D
  - dependsOn: close.stamp1g-application
  - completionCriteria: STAMP_EXPIRY 锚点相关任务在 Roadmap 中可见且提醒日期不晚于 validUntil 前 90 天
  - priority: CRITICAL
  - description: 官方 immigration 续期与转换检查点

- [ ] 选定一个 Portfolio 补充项目（如需）
  - templateTaskKey: close.portfolio-project
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P45D
  - dependsOn: cv.online-presence
  - completionCriteria: 从候选项目列表选定 1 题并创建“进行中”项目记录或 GitHub repo
  - priority: LOW
  - description: 参考 karan/Projects 题目方向；不伪造经历
