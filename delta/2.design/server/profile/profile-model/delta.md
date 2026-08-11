# profile-model（Pivot）

## `profile`

| 列 | 类型 |
|---|---|
| profile_id | UUID PK |
| profile_version | INT |
| lifecycle | VARCHAR |
| nationality | VARCHAR(8) |
| identity_status | VARCHAR(64) | 原 Work Auth 类型并入 |
| identity_valid_until | DATE |
| target_country | VARCHAR(8) |
| job_seeking_goal | TEXT |
| created_at / updated_at | TIMESTAMPTZ |

## 子表

- `education_period`（保留）  
- `work_experience`（新）：company, title, start_date, end_date, summary  
- `profile_skill`（新）：skill_name  
- `language_proficiency`（保留，多条）  

## 删除表/概念

- `target_role`  
- `work_authorization`（字段并入 profile.identity_*）  
- `recompute_request`（Roadmap 合并场景）— Job Gate 刷新若需要可后续另议，M1 Pivot 不经 Profile recompute API
