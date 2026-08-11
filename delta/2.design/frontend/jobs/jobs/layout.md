# Layout — Jobs Inbox

- 路由：`/jobs`
- 状态：设计正文已完成；**布局已由原型验收**（[`../../../prototype/jobs.html`](../../../prototype/jobs.html)）
- 全局基线：[`../../ui-baseline.md`](../../ui-baseline.md)

## 布局目标

- **信息密度优先**：同屏尽可能多展示 Job 行；详情侧栏可滚动，不挤压列表可视行数。
- **主从同屏**：列表约 **60%** 宽，详情侧栏约 **40%**；≥1280px 采用固定比例；<1280px 列表全宽 + 侧栏 overlay（设计默认，见 `style.md`）。
- **筛选置顶**：筛选与快捷操作占顶栏下方单行（可折行），列表区剩余高度全部用于虚拟滚动。

## 区域划分

| 区域 ID | 名称 | 占比/尺寸 | 职责 |
|---|---|---|---|
| `JobsPageShell` | 页面壳 | 100% × 100vh − 全局 Nav | 路由容器、错误边界 |
| `JobsTopBar` | 顶栏 | 全宽 × 48px | 页标题、保存 URL、跳转 `/jobs/sources`、Inbox 计数 |
| `JobsFilterBar` | 筛选条 | 全宽 × 40–56px | 状态/tier/Gate/来源/有效性/重复/隐藏开关 |
| `JobsMainSplit` | 主分栏 | 全宽 × 剩余高度 | 水平 split |
| `JobsListPane` | 列表区 | ~60% 宽 | 虚拟列表 + 列头（设计默认：无列头，卡片行） |
| `JobsDetailPane` | 详情侧栏 | ~40% 宽 | 选中 Job 详情；未选中显示占位 |
| `JobsDetailPlaceholder` | 空态 | 侧栏内居中 | 「选择左侧职位查看详情」 |

## 列表区内部（高密度）

- 行高 **设计默认 52px**（compact）；含：tier 色点、标题+公司、地点、状态 chip、有效性点、相对时间、重复/Gate 摘要 icon。
- 无分页控件：无限滚动 + `cursor` 分页（见 `api.md`）。
- 选中行：左侧 2px accent + 浅底；键盘 ↑↓ 切换选中（设计默认）。

## 详情侧栏内部（自上而下）

1. **Header**：标题、公司、地点、外链申请、关闭（窄屏 overlay 模式）
2. **StatusBar**：`jobStatus` 操作、`validityStatus`、`rankTier` badge
3. **GateSection**：四维结果 + 展开证据
4. **RankSection**：五因素 POSITIVE/NEUTRAL/NEGATIVE + 证据
5. **SourcesSection**：RawPosting 来源列表
6. **DuplicatePanel**（条件）：可能重复对比与操作
7. **DecisionHistory**：时间线
8. **GateOverrideForm**（条件）：覆盖原因输入

侧栏各 section 默认折叠除 Gate/Rank 外次要块（设计默认：Sources、DecisionHistory 折叠），以保留首屏证据可见性。

## 响应式

| 断点 | 行为 |
|---|---|
| ≥1280px | 60/40 固定 split |
| 1024–1279px | 55/45 split |
| <1024px | 列表全宽；选中 Job 时侧栏自右 overlay，带遮罩 |

## PlantUML

见 [`layout.puml`](layout.puml)。
