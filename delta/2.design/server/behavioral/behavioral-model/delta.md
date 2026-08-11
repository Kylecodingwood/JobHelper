# behavioral-model

表（摘要）：

- `bhv_question`：question_id, text, competency_topic, source_type, curation_version, visibility, created_at, updated_at  
- `bhv_star_evidence`：evidence_id, title, status, current_revision_id, created_at, updated_at  
- `bhv_star_evidence_revision`：revision_id, evidence_id, parent_revision_id, situation, task, action, result, competency_tags (text), created_at  
- `bhv_answer`：answer_id, question_id, current_version_id, status, created_at, updated_at  
- `bhv_answer_version`：version_id, answer_id, parent_version_id, version_number, question_text_snapshot, body_text, created_at  
- `bhv_answer_version_evidence`：version_id, evidence_revision_id  
- `bhv_answer_feedback`：feedback_id, answer_version_id, rule_version, created_at  
- `bhv_answer_feedback_item`：item_id, feedback_id, dimension, rule_id, target_quote, issue, rationale, suggestion, origin, decision, decided_at  

策展种子：`1.req/behavioral/curated-question-bank.md` → Flyway/启动导入 `curationVersion=v1`。
