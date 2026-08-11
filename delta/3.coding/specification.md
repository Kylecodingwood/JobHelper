# Coding Specification（Pivot 2026-08-09）

| 项 | 内容 |
|---|---|
| 状态 | **已细化**（Pivot：Roadmap Notion / Profile 画像 / Source 自管词） |
| 技术栈 | Java Spring Boot、PostgreSQL、React (Vite)、Python JobSpy |
| 依据 | `3.coding/api-contract.md`、各域新 api/model |

## 1. API 总则

- 前缀 `/api/v1`；M1 本地单用户无 token  
- 乐观锁：Profile `expectedProfileVersion` → 409  
- 错误体：`{ code, message, details, requestId }`  
- `SEARCH_TERMS_REQUIRED` → 400（首次同步前未填搜索词）

## 2. 域边界（Pivot）

| 域 | 边界 |
|---|---|
| Profile | 画像 CRUD + backup；**不**驱动 Roadmap；**不**派生 Source 搜索词 |
| Roadmap | 独立 todos；无模板/依赖/GRS/重算 |
| Job | Source 自管 `searchTerms`；Gate 可读 Profile 身份/语言 |
| Action/Home | 事件投影；Home 默认 3 条 |

## 3. 测试底线

- Profile PUT 无 targetRoles；多语言/skills 持久化  
- Roadmap CRUD + toggle  
- Sync 空 searchTerms → 400  
- Home actionLimit 默认 3  

## 4. 禁止

- Profile 保存触发 Roadmap merge  
- TargetRole → JobSpy/FreeHire 搜索词  
- 在 `delta/` 提交应用源码冒充实现仓  
