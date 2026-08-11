> **OBSOLETE (Pivot 2026-08-09)** — 系统模板 / GRS / Profile→Roadmap 重算已废止。见 `delta/3.coding/api-contract.md`。

# Roadmap 用户模板 Markdown 语法规范

- 状态：已冻结（v1）
- 适用范围：`RoadmapTemplate` 且 `templateType = USER` 的上传/粘贴内容；`templateType = SYSTEM` 的首版种子 [`system-template-v1.md`](system-template-v1.md) 亦遵循本语法
- 解析产物：不可变 `RoadmapTemplateVersion` 内的 `TemplateTaskDefinition` 列表

## 文档结构

1. **一级标题（`#`）= 阶段（phase）**  
   每个 `#` 区块定义一个 Roadmap 阶段；阶段名写入 `TemplateTaskDefinition.phase`。文档至少包含一个阶段。

2. **任务行 = GitHub 风格复选框**  
   每个任务以 `- [ ]` 或 `- [x]` 开头（解析时忽略勾选状态；`[x]` 不表示已完成 Roadmap 任务）。  
   任务标题为复选框后的首行可见文本，映射为 `title`。

3. **任务元数据 = 缩进子列表**  
   任务标题下以两个空格缩进的 `- key: value` 行声明结构化字段。键名大小写不敏感；值按下文规则解析。

## 必填字段

每个任务必须包含以下元数据键（缺一即行级校验失败）：

| 键 | 映射 | 规则 |
|---|---|---|
| `templateTaskKey` | `TemplateTaskDefinition.templateTaskKey` | 同一模板版本内唯一；`[a-z0-9._-]+`；跨系统模板版本稳定 |
| `anchorType` | `anchorType` | `COURSE_START` \| `GRADUATE_RECRUITMENT_SEASON` \| `GRADUATION` \| `STAMP_EXPIRY` \| `NONE` |
| `relativeOffset` | `relativeOffset` | ISO-8601 时长，如 `-P90D`、`-P6M`；当 `anchorType = NONE` 时必须为 `-P0D` 或省略（解析为 `-P0D`） |
| `dependsOn` | 逻辑依赖 | 逗号分隔的 `templateTaskKey` 列表；可空（表示无前置） |
| `completionCriteria` | `completionCriteria` | 非空文本；完成该 Roadmap 任务所需的客观标准 |
| `priority` | `defaultPriority` | `CRITICAL` \| `HIGH` \| `MEDIUM` \| `LOW` |

可选字段：

| 键 | 映射 | 规则 |
|---|---|---|
| `description` | `description` | 补充说明；可空 |

## 示例

```markdown
# 材料准备

- [ ] 建立基础 Graduate CV
  - templateTaskKey: cv.base-draft
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P120D
  - dependsOn:
  - completionCriteria: 单页 ATS 友好 CV 草稿已保存至 CV 模块，含教育、技能与至少一个项目
  - priority: HIGH
  - description: 参考 GradIreland CV Guide；不复制外部全文

- [ ] 预约 Career Service CV Review
  - templateTaskKey: cv.career-review
  - anchorType: GRADUATE_RECRUITMENT_SEASON
  - relativeOffset: -P90D
  - dependsOn: cv.base-draft
  - completionCriteria: MyCareer 或等价渠道已提交 Review 预约且收到确认
  - priority: HIGH
```

## 校验与错误报告

解析器必须按**行号**报告错误，并阻止用户确认保存（`UC-RDM-001` E1）：

| 错误码 | 条件 | 示例消息 |
|---|---|---|
| `RDM-MD-001` | 缺少 `#` 阶段 | `L1: 文档须至少包含一个一级标题阶段` |
| `RDM-MD-002` | 任务行非复选框 | `L12: 期望 "- [ ]" 任务行` |
| `RDM-MD-003` | 缺少必填键 | `L14: 任务 "cv.base-draft" 缺少 completionCriteria` |
| `RDM-MD-004` | `templateTaskKey` 重复 | `L28: templateTaskKey "cv.base-draft" 重复（首次于 L14）` |
| `RDM-MD-005` | `dependsOn` 引用不存在 | `L30: dependsOn 引用未知键 "cv.missing"` |
| `RDM-MD-006` | 依赖成环 | `L45: 依赖环 cv.a -> cv.b -> cv.a` |
| `RDM-MD-007` | 非法 `anchorType` / `priority` / 偏移 | `L16: anchorType "FOO" 非法` |
| `RDM-MD-008` | `anchorType = NONE` 但偏移非零 | `L20: NONE 锚点须使用 -P0D` |

预览（`UC-RDM-001`）须展示：解析后的阶段、任务、锚点、依赖 DAG 及全部警告/错误。

## 与生成流程的关系

- **首次生成**（`UC-RDM-002`）：合并选定 `RoadmapTemplate` 版本 → 解析 Profile 锚点 → **预览**候选任务与日期 → 用户确认 → 原子创建 Roadmap。
- **Profile 变更重算**：仅对 **系统模板来源且未完成** 的任务 merge-recompute 日期与可安全合并的模板字段；保留用户创建任务、用户编辑、完成、归档、pin、依赖覆盖及用户模板覆盖项。

## 术语

- 使用 **`RoadmapTemplate`** + **`TemplateType`**（`SYSTEM` / `USER`），不使用 `SystemTemplate` / `UserTemplate` 作为类名。
- 用户上传/粘贴产生的是 `templateType = USER` 的 `RoadmapTemplate` 新版本；内置种子为 `templateType = SYSTEM`。
