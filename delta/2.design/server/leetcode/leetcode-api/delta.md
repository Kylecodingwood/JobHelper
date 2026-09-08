# leetcode-api

权威契约：[`../../../../3.coding/api-contract.md`](../../../../3.coding/api-contract.md) LeetCode 节。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/leetcode/problems` | 过滤列表 |
| GET | `/api/v1/leetcode/problems/{problemId}` | 详情；缺正文则 GraphQL 拉取并落库 |
| POST | `/api/v1/leetcode/problems/{problemId}/content/refresh` | 强制刷新正文 |
| PUT | `/api/v1/leetcode/problems/{problemId}/review` | upsert review |
| DELETE | `/api/v1/leetcode/problems/{problemId}/review` | 204 |

包路径：`com.jobhelper.leetcode.{api,application,infrastructure}`。
