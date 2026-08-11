# profile entity（Pivot）

用户画像：`nationality`, `identityStatus`（含原工作许可类型）, `identityValidUntil`, `targetCountry`, `jobSeekingGoal`, `educationPeriods[]`, `workExperiences[]`, `skills[]`, `languageProficiencies[]`。

废止：`TargetRole`、独立 `WorkAuthorization` 聚合、Profile→Roadmap RecomputeRequest。
