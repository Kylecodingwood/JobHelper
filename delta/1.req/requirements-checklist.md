# Requirement 阶段检查清单

## 1. 文档结构

- [x] `delta/SRS/SRS.md` 包含版本、目的、范围、角色与系统定位
- [x] `delta/1.req/requirements.md` 提供业务域和细化入口
- [x] 六个 Module Group 与 SRS 术语一致
- [x] 每个 Module Group 均已完成 use case、workflow 和 entity 文档

## 2. 需求完整性

- [x] 产品级功能需求具有稳定 FR 编号
- [x] 已描述页面级行为，详细 UI 设计明确留到 `2.design`
- [x] 已描述数据源、AI、隐私和安全边界
- [x] 所有 UC 具有编号、参与者、触发条件、前置条件、主流程、异常流程和后置条件
- [x] 所有关键状态与枚举已在实体文档中定义
- [x] 跨域事件契约、全局 AI 同意、备份/导出/恢复 UC 已补齐
- [x] Roadmap 种子模板、Markdown 语法、Behavioral 策展题库 v1 已落地

## 3. 一致性

- [x] Module Group 命名在 SRS 与 `1.req` 中一致
- [x] M1/M2/M3 范围在文档中一致
- [x] 商业平台策略已更新：JobSpy 可搜索已启用站点；不得自动登录或提交申请
- [x] 六域均有用例图、流程图和实体图，并已完成结构一致性检查
- [x] Action 唯一拥有 Home 待办；Roadmap 不再定义独立 Action 聚合
- [x] Interview 命名已统一为 Behavioral（事件与 TaskOrigin）

## 4. 可追溯性

- [x] 产品目标可追溯至领域和里程碑
- [x] UC 可追溯至 FR
- [x] UC 可追溯至 workflow 和 entity
- [ ] M1 UC 映射至页面、API 和自动测试（`2.design` / `3.coding` 阶段完成）

## 5. 可测试性

- [x] 里程碑具有验收摘要
- [x] Job 幂等、用户决定优先和 AI 同意等关键规则可测试
- [x] Rank 投票聚合与缺入职日 Gate 规则可测试
- [x] 各 UC 已列出正常、边界和失败场景
- [x] 性能、备份和数据保留指标已量化；可访问性具体标准排入前端设计

## 6. 配置与集成

- [x] 外部系统及访问边界已列出
- [x] FreeHire API 验证已完成（M1 主源）
- [x] JobSpy LinkedIn/Indeed 验证已完成（M1 补充源）
- [x] JobSpy 默认每日全站点尝试、支持手动重跑和逐站点停用
- [x] AI provider 选择明确延期；provider-neutral 边界、默认关闭、授权与本地规则已确认

## 7. 阶段结论

当前状态：**SRS v0.4 与六域 1.req 已达到 Requirement 阶段门禁，可进入 `2.design`。**

允许先行的工作：

- 已完成 FreeHire + JobSpy 验证；
- 下一阶段补充 UC ↔ 页面/API/Service/自动测试映射；
- 技术栈、AI provider、解析库和备份钟点/保留份数在设计 ADR 中确认。
