# API — LeetCode

- 路由：`/leetcode`
- 契约：[`../../../server/leetcode/leetcode-api/delta.md`](../../../server/leetcode/leetcode-api/delta.md)

| 场景 | 调用 |
|---|---|
| 列表 | `GET /leetcode/problems?…` |
| 打开题 | `GET /leetcode/problems/{id}`（可能触发正文缓存） |
| 刷新题面 | `POST …/content/refresh` |
| 保存笔记 | `PUT …/review` |
| 清除笔记 | `DELETE …/review` |
