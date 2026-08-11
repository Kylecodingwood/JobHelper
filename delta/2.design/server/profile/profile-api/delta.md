# profile-api（Pivot）

用户画像基座（后续 AI prompt）。无 TargetRole；无 Roadmap 重算。

## 端点

| 方法 | 路径 |
|---|---|
| GET | `/api/v1/profile` |
| PUT | `/api/v1/profile` |
| POST | `/api/v1/profile/validate` |
| GET | `/api/v1/profile/field-usage` |
| GET/POST | `/api/v1/profile/backups`… |

## ProfileDto / PUT

```json
{
  "profileId": "uuid",
  "profileVersion": 3,
  "lifecycle": "READY",
  "nationality": "CN",
  "identityStatus": "Stamp 1G",
  "identityValidUntil": "2027-03-01",
  "targetCountry": "IE",
  "jobSeekingGoal": "Graduate software roles in Ireland",
  "educationPeriods": [],
  "workExperiences": [
    { "company": "Acme", "title": "Intern", "startDate": "2024-06-01", "endDate": "2024-09-01", "summary": "…" }
  ],
  "skills": ["Java", "Spring", "SQL"],
  "languageProficiencies": [
    { "languageCode": "en", "proficiency": "B2" },
    { "languageCode": "zh", "proficiency": "NATIVE" }
  ]
}
```

PUT 带 `expectedProfileVersion`；冲突 409 `PROFILE_VERSION_CONFLICT`。

## 废止

- `targetRoles`、`workAuthorizations`  
- `/profile/recompute-requests/*`
