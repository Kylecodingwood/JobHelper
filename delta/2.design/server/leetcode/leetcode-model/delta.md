# leetcode-model

Flyway：`V15__leetcode_hot100.sql`、`V16__leetcode_problem_content.sql`。种子：`resources/leetcode/hot100.json`。

## `leetcode_problem`

| 列 | 说明 |
|---|---|
| problem_id | UUID PK |
| problem_number | INT UNIQUE |
| title, slug, difficulty, tags, url | |
| sort_order | |
| statement_html, examples | 可空；首次打开拉取 |
| content_fetched_at | 可空 |

## `leetcode_review`

| 列 | 说明 |
|---|---|
| review_id | UUID PK |
| problem_id | UNIQUE FK CASCADE |
| confusion | NOT NULL |
| approach, key_code | 可空 |
| mastery | confident/partial/weak |
| next_review_at | 可空 |
| created_at / updated_at | |
