# leetcode entity（2026-08）

独立面试刷题域（与 Roadmap 无关）。UI 全英。目录以 LeetCode **Hot 100** 为主，种子数据 `classpath:leetcode/hot100.json`。

## 1. LeetCodeProblem

| 属性 | 说明 |
|---|---|
| `problemId` | UUID |
| `problemNumber` | 题号（唯一） |
| `title`, `slug`, `url` | |
| `difficulty` | Easy / Medium / Hard 等 |
| `tags` | 序列化标签 |
| `sortOrder` | 目录顺序 |
| `statementHtml?` | 题面 HTML；首次打开时若空则从 LeetCode GraphQL 拉取并缓存 |
| `examples?` | 样例文本/JSON |
| `contentFetchedAt?` | 正文缓存时间 |

## 2. LeetCodeReview（每题至多一条）

| 属性 | 说明 |
|---|---|
| `reviewId`, `problemId` | problem 唯一 |
| `confusion` | **必填**（疑惑点） |
| `approach?`, `keyCode?` | |
| `mastery` | `confident` \| `partial` \| `weak` |
| `nextReviewAt?` | |

## 3. 规则

1. 正文缓存可 `POST .../content/refresh` 强制刷新。
2. 删除 review 不影响 catalog 题目。
3. 列表支持 `q` / `difficulty` / `mastery` / `review=all|reviewed|unreviewed` 过滤。
