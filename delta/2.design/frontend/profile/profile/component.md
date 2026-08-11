# Components — Profile

- 路由：`/profile`
- 状态：设计正文已完成

## 组件树

```text
ProfilePageShell
├── [onboarding] ProfileOnboardingWizard
│   ├── ProfileStepIndicator
│   ├── ProfileStepEducation / WorkAuth / Target / LanguageSkills
│   └── ProfileStepReview
└── [edit] ProfileTopBar + ProfileMainSplit
    ├── ProfileFormColumn
    │   ├── ProfileSectionEducation
    │   ├── ProfileSectionWorkAuth
    │   ├── ProfileSectionTargetRole
    │   ├── ProfileSectionLanguageProficiency
    │   ├── ProfileSectionSkills
    │   └── ProfileSectionExperience
    └── ProfileSideColumn
        ├── ProfileRecomputePanel
        └── ProfileDataManagementPanel

ProfileFieldUsagePopover (shared)
ProfileImpactConfirmDialog
ProfileExportDialog
ProfileRestoreDialog
ProfileRoadmapPreviewEmbed (in RecomputePanel)
```

## 组件规格

### `ProfilePageShell`

| 属性 | 说明 |
|---|---|
| 模式 | `onboarding` \| `edit` 由 Profile 是否存在 + query 决定 |
| 加载 | `GET /api/v1/profile` → 404 强制 onboarding |

### `ProfileOnboardingWizard`

| ID | `profile-onboarding-wizard` |
|---|---|
| 步骤 | 1 教育 2 许可 3 目标 4 语言/技能/经历 + Review |
| 校验 | 步间不可下一步若必填失败 |
| 提交 | `POST /api/v1/profile` |

### `ProfileSectionEducation`

| ID | `profile-section-education` |
|---|---|
| 控件 | institution, programme, start/end, country, isPrimary radio |
| 用途 | Popover：Roadmap 锚点 COURSE_START/GRADUATION |

### `ProfileSectionWorkAuth`

| ID | `profile-section-work-auth` |
|---|---|
| 控件 | 时间线列表：permissionType, validFrom/Until, weeklyHours, isFuture |
| 类型 | STAMP_2, STAMP_1G, STAMP_1, STAMP_4, EU_EEA_CITIZEN, OTHER |
| 用途 | Gate + Roadmap STAMP_EXPIRY |

### `ProfileSectionTargetRole`

| ID | `profile-section-target-role` |
|---|---|
| 控件 | roleName, seniority, location, priorityOrder, active toggle |
| 提示 | active roleName → Job Source 默认搜索词 |

### `ProfileSectionLanguageProficiency`

| ID | `profile-section-language-proficiency` |
|---|---|
| 实体 | **LanguageProficiency** |
| 控件 | languageCode, proficiency (NATIVE…UNKNOWN), evidenceRef?, notes? |
| 约束 | languageCode 唯一；至少 0 条（Gate 缺语言可标 UNKNOWN） |
| 用途 | Popover：Job Gate mandatoryLanguages |

### `ProfileSectionSkills` / `ProfileSectionExperience`

| ID | `profile-section-skills` / `profile-section-experience` |
|---|---|
| 实体 | SkillEvidence / ExperienceEvidence |
| 关联 | Skill 可链接 Experience |

### `ProfileFieldUsagePopover`

| ID | `profile-field-usage-popover` |
|---|---|
| 内容 | 用于 ROADMAP / JOB_GATE / JOB_RANK；敏感标识；非 AI consent 免责声明 |

### `ProfileRecomputePanel`

| ID | `profile-recompute-panel` |
|---|---|
| 列表 | PENDING/DEFERRED RecomputeRequest |
| 操作 | scope 多选、预览、确认、推迟 |
| 嵌入 | `ProfileRoadmapPreviewEmbed` 当 scope 含 ROADMAP |

### `ProfileDataManagementPanel`

| ID | `profile-data-management-panel` |
|---|---|
| 展示 | 调度 **每周日 03:00**、保留 **4**、`./data/backups` |
| 操作 | 打开 Export / Restore 对话框；备份历史列表只读 |

### `ProfileImpactConfirmDialog`

| ID | `profile-impact-confirm-dialog` |
|---|---|
| 内容 | field diff + affected scopes 摘要 |

### `ProfileExportDialog` / `ProfileRestoreDialog`

| ID | `profile-export-dialog` / `profile-restore-dialog` |
|---|---|
| Export | 路径选择、可选加密口令、敏感提示 |
| Restore | 文件选择、preview 域列表、覆盖二次确认 |

## PlantUML

见 [`component.puml`](component.puml)。
