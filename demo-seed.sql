-- ============================================================
-- Job Helper — Demo Seed Data
-- Run: psql -h localhost -U jobhelper -d jobhelper -f demo-seed.sql
-- ============================================================
-- All data is fictional. No real people, companies, or credentials.

BEGIN;

-- Clean existing data (order matters for FK constraints)
TRUNCATE
  bhv_answer_feedback_item, bhv_answer_feedback, bhv_answer_version_evidence,
  bhv_answer_version, bhv_answer, bhv_star_evidence_revision, bhv_star_evidence,
  bhv_question,
  leetcode_review, leetcode_problem,
  roadmap_document, roadmap_company, roadmap_todo, roadmap_folder,
  email_application,
  cv_document,
  action_decision, action_item,
  job_source_diagnostic, job_possible_duplicate, job_decision,
  job_source_ref, canonical_job, job_source_run, job_source, job_search_settings,
  template_task_definition, roadmap_template_version, roadmap_template,
  task_dependency_override, task_dependency, roadmap_task, generation_record, roadmap,
  domain_event_receipt, domain_event_outbox,
  profile_backup_metadata, recompute_request, preview_session,
  work_experience, profile_skill, language_proficiency,
  target_role, work_authorization, education_period, profile
CASCADE;

-- ============================================================
-- 1. PROFILE
-- ============================================================
INSERT INTO profile (profile_id, profile_version, lifecycle, created_at, updated_at, nationality, identity_status, identity_valid_until, target_country, job_seeking_goal) VALUES (
  'a0000000-0000-0000-0000-000000000001', 1, 'ACTIVE', NOW(), NOW(),
  'IE', 'Stamp 1G', '2027-06-30', 'IE',
  'Seeking graduate SWE roles in Ireland / Europe'
);

INSERT INTO education_period (education_period_id, profile_id, institution_name, programme_name, start_date, expected_graduation_date, country_code, is_primary) VALUES
  ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   'University College Dublin', 'MSc Computer Science', '2025-09-01', '2027-05-31', 'IE', TRUE),
  ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
   'Zhejiang University', 'BSc Software Engineering', '2021-09-01', '2025-06-15', 'CN', FALSE);

INSERT INTO work_authorization (work_authorization_id, profile_id, permission_type, valid_from, valid_until, weekly_hours_limit, is_future, country_code) VALUES
  ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   'STAMP_1G', '2027-06-01', '2029-05-31', 20.00, FALSE, 'IE'),
  ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
   'STAMP_4', NULL, NULL, 40.00, TRUE, 'IE');

INSERT INTO target_role (target_role_id, profile_id, role_name, seniority, location, priority_order, active) VALUES
  ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   'Software Engineer', 'JUNIOR', 'Dublin, Ireland', 1, TRUE),
  ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
   'Backend Engineer', 'JUNIOR', 'Remote (Europe)', 2, TRUE),
  ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',
   'Data Engineer', 'ENTRY', 'Dublin, Ireland', 3, TRUE);

INSERT INTO language_proficiency (language_proficiency_id, profile_id, language_code, proficiency, evidence_ref, notes) VALUES
  ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   'zh', 'NATIVE', NULL, 'Mandarin — native speaker'),
  ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
   'en', 'C1', 'IELTS 7.5', 'Academic English');

INSERT INTO work_experience (work_experience_id, profile_id, company, title, start_date, end_date, summary, sort_order) VALUES
  ('f0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
   'ByteDance', 'Backend Engineer Intern', '2024-06-01', '2024-09-30',
   'Built recommendation pipeline serving 50M daily users. Go + Kafka + Redis.', 0),
  ('f0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
   'Huawei Cloud', 'Software Engineer Intern', '2023-07-01', '2023-12-31',
   'Developed CI/CD tooling and container orchestration dashboards. Java + Vue.', 1);

INSERT INTO profile_skill (profile_skill_id, profile_id, skill_name, sort_order) VALUES
  ('f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Java', 0),
  ('f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Python', 1),
  ('f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Spring Boot', 2),
  ('f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'React', 3),
  ('f1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'PostgreSQL', 4),
  ('f1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Docker', 5),
  ('f1000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'Go', 6),
  ('f1000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', 'TypeScript', 7);

-- ============================================================
-- 2. JOB SOURCES + JOBS
-- ============================================================
INSERT INTO job_search_settings (id, search_terms, updated_at) VALUES (1, 'software engineer graduate dublin, backend developer ireland, java developer junior', NOW());

INSERT INTO job_source (source_id, code, adapter_type, enabled, role, search_terms_override, risk_note, config_version, created_at, updated_at) VALUES
  ('10000000-0000-0000-0000-000000000001', 'freehire_ie', 'FREEHIRE', TRUE, 'GRAD', NULL, NULL, 1, NOW(), NOW()),
  ('10000000-0000-0000-0000-000000000002', 'linkedin_ie', 'JOBSPY', TRUE, 'GRAD', 'software engineer graduate dublin', NULL, 1, NOW(), NOW()),
  ('10000000-0000-0000-0000-000000000003', 'indeed_ie', 'JOBSPY', TRUE, 'GRAD', 'junior developer ireland', NULL, 1, NOW(), NOW());

INSERT INTO job_source_run (run_id, source_id, trigger_type, status, parameters_snapshot, request_count, received_count, valid_count, created_count, updated_count, failed_count, diagnostics, started_at, ended_at, created_at) VALUES
  ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
   'SCHEDULED', 'COMPLETED', NULL, 1, 35, 28, 20, 8, 0, NULL,
   NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 55 minutes', NOW() - INTERVAL '2 hours'),
  ('11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002',
   'MANUAL', 'COMPLETED', NULL, 1, 50, 42, 30, 12, 0, NULL,
   NOW() - INTERVAL '1 day', NOW() - INTERVAL '23 hours', NOW() - INTERVAL '1 day');

INSERT INTO canonical_job (job_id, title, company, location, job_status, validity_status, gate_status, rank_tier, hidden_by_default, version, description, canonical_apply_url, seniority, first_seen_at, created_at, updated_at) VALUES
  ('20000000-0000-0000-0000-000000000001', 'Graduate Software Engineer', 'Stripe', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Join our Payments team to build reliable, scalable financial infrastructure. Java, distributed systems, API design.',
   'https://example.com/stripe-grad', 'JUNIOR', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW()),
  ('20000000-0000-0000-0000-000000000002', 'Software Engineer — New Grad', 'Workday', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Full-stack development on our HCM platform. React + Java microservices.',
   'https://example.com/workday-grad', 'JUNIOR', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
  ('20000000-0000-0000-0000-000000000003', 'Junior Backend Developer', 'Intercom', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Build real-time messaging infrastructure. Ruby on Rails + Go.',
   'https://example.com/intercom-jr', 'JUNIOR', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
  ('20000000-0000-0000-0000-000000000004', 'Graduate Data Engineer', 'Accenture', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'MEDIUM', FALSE, 1,
   'ETL pipelines and data warehouse design. Python + SQL + Spark.',
   'https://example.com/accenture-data', 'ENTRY', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', NOW()),
  ('20000000-0000-0000-0000-000000000005', 'Software Engineer Intern (Summer 2027)', 'Google', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   '12-week summer internship. Work on Google Cloud or Ads infrastructure.',
   'https://example.com/google-intern', 'INTERN', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', NOW()),
  ('20000000-0000-0000-0000-000000000006', 'Junior Full-Stack Developer', 'Fidelity Investments', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'MEDIUM', FALSE, 1,
   'Angular + Spring Boot on our wealth management platform.',
   'https://example.com/fidelity-fs', 'JUNIOR', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NOW()),
  ('20000000-0000-0000-0000-000000000007', 'Platform Engineer Graduate', 'Mastercard', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'MEDIUM', FALSE, 1,
   'Kubernetes, Terraform, CI/CD. Site reliability and developer tooling.',
   'https://example.com/mc-platform', 'JUNIOR', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days', NOW()),
  ('20000000-0000-0000-0000-000000000008', 'Junior Java Developer', 'Citi', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'MEDIUM', FALSE, 1,
   'Core banking systems modernisation. Java 17 + Spring Boot + Oracle.',
   'https://example.com/citi-java', 'JUNIOR', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW()),
  ('20000000-0000-0000-0000-000000000009', 'QA Automation Engineer', 'Zendesk', 'Dublin', 'ACTIVE', 'VALID', 'FAIL', 'LOW', TRUE, 1,
   'Test automation for support ticketing platform. Selenium + Python.',
   'https://example.com/zendesk-qa', 'JUNIOR', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days', NOW()),
  ('20000000-0000-0000-0000-000000000010', 'DevOps Graduate', 'Version 1', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'MEDIUM', FALSE, 1,
   'Consulting role across AWS / Azure. Terraform + Ansible + Docker.',
   'https://example.com/v1-devops', 'ENTRY', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW()),
  ('20000000-0000-0000-0000-000000000011', 'Graduate Software Engineer', 'Waymo (via Google)', 'Remote EU', 'STALE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Autonomous driving perception stack. C++ and Python.',
   'https://example.com/waymo-grad', 'JUNIOR', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', NOW()),
  ('20000000-0000-0000-0000-000000000012', 'Backend Developer', 'Hubspot', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'CRM platform backend. Java + Kafka + MySQL.',
   'https://example.com/hubspot-be', 'JUNIOR', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW()),
  ('20000000-0000-0000-0000-000000000013', 'Software Engineer I', 'Salesforce', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Build scalable multi-tenant SaaS features. Java + Lightning Web Components.',
   'https://example.com/sf-se1', 'JUNIOR', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW()),
  ('20000000-0000-0000-0000-000000000014', 'IT Graduate — Business Systems', 'Kerry Group', 'Tralee', 'ACTIVE', 'VALID', 'FAIL', 'LOW', TRUE, 1,
   'ERP support and integration development. SAP basis.',
   'https://example.com/kerry-it', 'ENTRY', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days', NOW()),
  ('20000000-0000-0000-0000-000000000015', 'Machine Learning Engineer Intern', 'Meta', 'Dublin', 'ACTIVE', 'VALID', 'PASS', 'HIGH', FALSE, 1,
   'Research and production ML for ranking. PyTorch + distributed training.',
   'https://example.com/meta-ml', 'INTERN', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', NOW());

INSERT INTO job_source_ref (job_source_ref_id, job_id, source_code, source_stable_id, apply_url, is_preferred, first_seen_at, last_seen_at, created_at, updated_at) VALUES
  ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'freehire_ie', 'fh-stripe-001', 'https://example.com/stripe-grad', TRUE, NOW()-INTERVAL '3 days', NOW(), NOW()-INTERVAL '3 days', NOW()),
  ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'linkedin_ie', 'li-workday-001', 'https://example.com/workday-grad', TRUE, NOW()-INTERVAL '5 days', NOW(), NOW()-INTERVAL '5 days', NOW()),
  ('21000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'freehire_ie', 'fh-intercom-001', 'https://example.com/intercom-jr', TRUE, NOW()-INTERVAL '2 days', NOW(), NOW()-INTERVAL '2 days', NOW()),
  ('21000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000005', 'linkedin_ie', 'li-google-001', 'https://example.com/google-intern', TRUE, NOW()-INTERVAL '10 days', NOW(), NOW()-INTERVAL '10 days', NOW()),
  ('21000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000012', 'indeed_ie', 'ind-hubspot-001', 'https://example.com/hubspot-be', TRUE, NOW()-INTERVAL '2 days', NOW(), NOW()-INTERVAL '2 days', NOW());

-- ============================================================
-- 3. ACTION ITEMS
-- ============================================================
INSERT INTO action_item (action_id, source_domain, action_kind, title, status, active, deadline, pinned, priority_band, priority_sort_key, target_type, target_id, route_hint, version, created_at, updated_at) VALUES
  ('30000000-0000-0000-0000-000000000001', 'JOB', 'APPLY', 'Apply to Stripe — Graduate Software Engineer', 'OPEN', TRUE,
   NOW() + INTERVAL '2 days', TRUE, 'URGENT', '01-URGENT', 'JOB', '20000000-0000-0000-0000-000000000001', '/jobs', 1, NOW()-INTERVAL '1 day', NOW()),
  ('30000000-0000-0000-0000-000000000002', 'JOB', 'APPLY', 'Apply to Intercom — Junior Backend Developer', 'OPEN', TRUE,
   NOW() + INTERVAL '3 days', FALSE, 'HIGH', '02-HIGH', 'JOB', '20000000-0000-0000-0000-000000000003', '/jobs', 1, NOW()-INTERVAL '1 day', NOW()),
  ('30000000-0000-0000-0000-000000000003', 'JOB', 'REVIEW', 'Review Hubspot listing details', 'OPEN', TRUE,
   NOW() + INTERVAL '5 days', FALSE, 'MEDIUM', '03-MEDIUM', 'JOB', '20000000-0000-0000-0000-000000000012', '/jobs', 1, NOW(), NOW()),
  ('30000000-0000-0000-0000-000000000004', 'ROADMAP', 'TODO', 'Finish LeetCode binary search set', 'OPEN', TRUE,
   NOW() + INTERVAL '1 day', TRUE, 'URGENT', '01-URGENT', NULL, NULL, '/leetcode', 1, NOW()-INTERVAL '2 days', NOW()),
  ('30000000-0000-0000-0000-000000000005', 'ROADMAP', 'TODO', 'Prepare STAR answer for teamwork question', 'OPEN', TRUE,
   NOW() + INTERVAL '4 days', FALSE, 'HIGH', '02-HIGH', NULL, NULL, '/behavioral', 1, NOW()-INTERVAL '1 day', NOW()),
  ('30000000-0000-0000-0000-000000000006', 'JOB', 'APPLY', 'Apply to Workday — New Grad SWE', 'DONE', FALSE,
   NOW() - INTERVAL '2 days', FALSE, 'MEDIUM', '03-MEDIUM', 'JOB', '20000000-0000-0000-0000-000000000002', '/jobs', 1, NOW()-INTERVAL '5 days', NOW()-INTERVAL '2 days'),
  ('30000000-0000-0000-0000-000000000007', 'JOB', 'APPLY', 'Apply to Salesforce — SE I', 'OPEN', TRUE,
   NOW() + INTERVAL '6 days', FALSE, 'MEDIUM', '03-MEDIUM', 'JOB', '20000000-0000-0000-0000-000000000013', '/jobs', 1, NOW(), NOW()),
  ('30000000-0000-0000-0000-000000000008', 'ROADMAP', 'TODO', 'Update CV with ByteDance experience', 'DONE', FALSE,
   NOW() - INTERVAL '3 days', FALSE, 'HIGH', '02-HIGH', NULL, NULL, '/cv', 1, NOW()-INTERVAL '5 days', NOW()-INTERVAL '3 days');

-- ============================================================
-- 4. ROADMAP FOLDERS + TODOS + COMPANIES + DOCUMENTS
-- ============================================================
INSERT INTO roadmap_folder (folder_id, name, kind, sort_order, created_at, updated_at) VALUES
  ('40000000-0000-0000-0000-000000000001', 'Todolist', 'todolist', 0, NOW()-INTERVAL '30 days', NOW()),
  ('40000000-0000-0000-0000-000000000002', '11/3',     'todolist', 1, NOW()-INTERVAL '8 days', NOW()-INTERVAL '8 days'),
  ('40000000-0000-0000-0000-000000000003', '11/7',     'todolist', 2, NOW()-INTERVAL '4 days', NOW()-INTERVAL '4 days'),
  ('40000000-0000-0000-0000-000000000004', '11/10',    'todolist', 3, NOW()-INTERVAL '1 day', NOW()),
  ('40000000-0000-0000-0000-000000000005', '11/14',    'todolist', 4, NOW(), NOW()),
  ('40000000-0000-0000-0000-000000000010', 'Target Companies',     'companytracker', 10, NOW()-INTERVAL '20 days', NOW()),
  ('40000000-0000-0000-0000-000000000011', 'Interview Pipeline',   'companytracker', 11, NOW()-INTERVAL '10 days', NOW()),
  ('40000000-0000-0000-0000-000000000020', 'Interview Notes',      'document', 20, NOW()-INTERVAL '15 days', NOW()),
  ('40000000-0000-0000-0000-000000000021', 'Knowledge Base',       'document', 21, NOW()-INTERVAL '10 days', NOW());

-- Todos — general todolist
INSERT INTO roadmap_todo (todo_id, folder_id, name, done, sort_order, created_at, updated_at) VALUES
  ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Set up LinkedIn profile headline', TRUE,  0, NOW()-INTERVAL '25 days', NOW()-INTERVAL '20 days'),
  ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'Draft cover letter template',       TRUE,  1, NOW()-INTERVAL '25 days', NOW()-INTERVAL '18 days'),
  ('41000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 'Research visa sponsorship companies', FALSE, 2, NOW()-INTERVAL '20 days', NOW());

-- Todos — 11/3 (past, all done)
INSERT INTO roadmap_todo (todo_id, folder_id, name, done, sort_order, created_at, updated_at) VALUES
  ('41000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000002', 'Submit Workday application',         TRUE, 0, NOW()-INTERVAL '8 days', NOW()-INTERVAL '7 days'),
  ('41000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000002', 'Review Two Sum solution',            TRUE, 1, NOW()-INTERVAL '8 days', NOW()-INTERVAL '7 days');

-- Todos — 11/7
INSERT INTO roadmap_todo (todo_id, folder_id, name, done, sort_order, created_at, updated_at) VALUES
  ('41000000-0000-0000-0000-000000000020', '40000000-0000-0000-0000-000000000003', 'Stripe application — tailor CV',     TRUE,  0, NOW()-INTERVAL '4 days', NOW()-INTERVAL '3 days'),
  ('41000000-0000-0000-0000-000000000021', '40000000-0000-0000-0000-000000000003', 'LeetCode — 3 medium problems',      TRUE,  1, NOW()-INTERVAL '4 days', NOW()-INTERVAL '3 days'),
  ('41000000-0000-0000-0000-000000000022', '40000000-0000-0000-0000-000000000003', 'Mock interview with friend',         FALSE, 2, NOW()-INTERVAL '4 days', NOW());

-- Todos — 11/10 (current)
INSERT INTO roadmap_todo (todo_id, folder_id, name, done, sort_order, created_at, updated_at) VALUES
  ('41000000-0000-0000-0000-000000000030', '40000000-0000-0000-0000-000000000004', 'Apply to Intercom',                  FALSE, 0, NOW()-INTERVAL '1 day', NOW()),
  ('41000000-0000-0000-0000-000000000031', '40000000-0000-0000-0000-000000000004', 'Finish binary search problem set',   FALSE, 1, NOW()-INTERVAL '1 day', NOW()),
  ('41000000-0000-0000-0000-000000000032', '40000000-0000-0000-0000-000000000004', 'Review system design notes',         FALSE, 2, NOW()-INTERVAL '1 day', NOW()),
  ('41000000-0000-0000-0000-000000000033', '40000000-0000-0000-0000-000000000004', 'Update GitHub README screenshots',   TRUE,  3, NOW()-INTERVAL '1 day', NOW()-INTERVAL '2 hours');

-- Todos — 11/14 (future)
INSERT INTO roadmap_todo (todo_id, folder_id, name, done, sort_order, created_at, updated_at) VALUES
  ('41000000-0000-0000-0000-000000000040', '40000000-0000-0000-0000-000000000005', 'Google OA practice',                 FALSE, 0, NOW(), NOW()),
  ('41000000-0000-0000-0000-000000000041', '40000000-0000-0000-0000-000000000005', 'Prepare STAR answers for leadership', FALSE, 1, NOW(), NOW()),
  ('41000000-0000-0000-0000-000000000042', '40000000-0000-0000-0000-000000000005', 'Apply to Hubspot & Salesforce',      FALSE, 2, NOW(), NOW());

-- Companies — Target Companies
INSERT INTO roadmap_company (company_id, folder_id, company_name, status, contact, note, position, reason, interview_at, sort_order, created_at, updated_at) VALUES
  ('42000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000010',
   'Stripe', 'applied', 'recruiter@example-stripe.com', 'Strong match — payments infra', 'Graduate SWE', 'Top choice — distributed systems', NULL, 0, NOW()-INTERVAL '5 days', NOW()),
  ('42000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000010',
   'Workday', 'applied', 'campus@example-workday.com', 'Applied via careers page', 'New Grad SWE', 'Dublin HQ, good WLB', NULL, 1, NOW()-INTERVAL '8 days', NOW()),
  ('42000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000010',
   'Intercom', 'watching', NULL, 'Interesting product, need to apply', 'Junior Backend', 'Messaging infra experience', NULL, 2, NOW()-INTERVAL '3 days', NOW()),
  ('42000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000010',
   'Google', 'applied', NULL, 'Summer intern application submitted', 'SWE Intern Summer 2027', 'Dream company', NULL, 3, NOW()-INTERVAL '10 days', NOW()),
  ('42000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000010',
   'Hubspot', 'watching', NULL, 'Listed on FreeHire', 'Backend Developer', 'CRM + Kafka stack', NULL, 4, NOW()-INTERVAL '2 days', NOW()),
  ('42000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000010',
   'Salesforce', 'watching', NULL, NULL, 'SE I', 'Large scale multi-tenant SaaS', NULL, 5, NOW()-INTERVAL '5 days', NOW()),
  ('42000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000010',
   'Meta', 'applied', NULL, 'ML intern track', 'ML Engineer Intern', 'PyTorch experience fits', NULL, 6, NOW()-INTERVAL '4 days', NOW()),
  ('42000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000010',
   'Mastercard', 'applied', 'grad@example-mc.com', 'Submitted online', 'Platform Engineer Grad', 'K8s + IaC', NULL, 7, NOW()-INTERVAL '6 days', NOW());

-- Companies — Interview Pipeline
INSERT INTO roadmap_company (company_id, folder_id, company_name, status, contact, note, position, reason, interview_at, sort_order, created_at, updated_at) VALUES
  ('42000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000011',
   'Workday', 'interviewing', 'jane.smith@example-workday.com', 'Passed OA, phone screen scheduled', 'New Grad SWE', 'Strong culture fit', '11/15 2pm', 0, NOW()-INTERVAL '3 days', NOW()),
  ('42000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000011',
   'Accenture', 'interviewing', 'grad-recruit@example-accenture.com', 'Video interview done, waiting result', 'Graduate Data Engineer', 'Fallback option', '11/8 10am', 1, NOW()-INTERVAL '5 days', NOW()),
  ('42000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000011',
   'Fidelity', 'offer', 'hr@example-fidelity.com', 'Offer received! Deadline 11/20', 'Junior Full-Stack Dev', 'Stable finance sector', '11/5 3pm', 2, NOW()-INTERVAL '10 days', NOW());

-- Documents
INSERT INTO roadmap_document (document_id, folder_id, title, body_html, sort_order, created_at, updated_at) VALUES
  ('43000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000020',
   'Workday Phone Screen Notes',
   '<h2>Workday — Phone Screen Prep</h2><ul><li>Review REST API design patterns</li><li>Prepare "why Workday" answer</li><li>Research HCM platform architecture</li></ul><p>Key points: mention ByteDance distributed systems experience, relate to their microservices migration.</p>',
   0, NOW()-INTERVAL '3 days', NOW()),
  ('43000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000020',
   'Common Interview Questions',
   '<h2>Technical</h2><ul><li>Explain REST vs GraphQL tradeoffs</li><li>How does a HashMap work internally?</li><li>Describe ACID properties</li></ul><h2>Behavioral</h2><ul><li>Tell me about a time you had a conflict with a teammate</li><li>Describe a project you are proud of</li></ul>',
   1, NOW()-INTERVAL '10 days', NOW()),
  ('43000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000021',
   'System Design Cheat Sheet',
   '<h2>Load Balancer</h2><p>Round robin, least connections, consistent hashing.</p><h2>Caching</h2><p>Redis/Memcached. Cache-aside vs write-through. TTL and eviction.</p><h2>Database</h2><p>Sharding, replication, CAP theorem.</p>',
   0, NOW()-INTERVAL '8 days', NOW()),
  ('43000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000021',
   'Java Concurrency Notes',
   '<h2>Thread Safety</h2><p>synchronized, volatile, AtomicInteger. ReentrantLock vs synchronized.</p><h2>Executor Framework</h2><p>ThreadPoolExecutor, CompletableFuture, ForkJoinPool.</p><h2>Common Pitfalls</h2><p>Deadlock detection, race conditions, visibility issues.</p>',
   1, NOW()-INTERVAL '5 days', NOW());

-- ============================================================
-- 5. LEETCODE PROBLEMS + REVIEWS
-- ============================================================
INSERT INTO leetcode_problem (problem_id, problem_number, title, slug, difficulty, tags, url, sort_order, statement_html, content_fetched_at) VALUES
  ('50000000-0000-0000-0000-000000000001',  1,  'Two Sum',                    'two-sum',                   'Easy',   'Array, Hash Table',          'https://leetcode.com/problems/two-sum/',                    1,  '<p>Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.</p>', NOW()-INTERVAL '20 days'),
  ('50000000-0000-0000-0000-000000000002',  3,  'Longest Substring Without Repeating Characters', 'longest-substring-without-repeating-characters', 'Medium', 'String, Sliding Window', 'https://leetcode.com/problems/longest-substring-without-repeating-characters/', 2, NULL, NULL),
  ('50000000-0000-0000-0000-000000000003', 11,  'Container With Most Water',  'container-with-most-water', 'Medium', 'Array, Two Pointers',        'https://leetcode.com/problems/container-with-most-water/',  3,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000004', 15,  'Three Sum',                  'three-sum',                 'Medium', 'Array, Two Pointers, Sorting', 'https://leetcode.com/problems/3sum/',                      4,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000005', 20,  'Valid Parentheses',          'valid-parentheses',         'Easy',   'String, Stack',              'https://leetcode.com/problems/valid-parentheses/',          5,  '<p>Given a string s containing just the characters ''('', '')'', ''{'', ''}'', ''['' and '']'', determine if the input string is valid.</p>', NOW()-INTERVAL '18 days'),
  ('50000000-0000-0000-0000-000000000006', 21,  'Merge Two Sorted Lists',     'merge-two-sorted-lists',    'Easy',   'Linked List, Recursion',     'https://leetcode.com/problems/merge-two-sorted-lists/',     6,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000007', 33,  'Search in Rotated Sorted Array', 'search-in-rotated-sorted-array', 'Medium', 'Array, Binary Search', 'https://leetcode.com/problems/search-in-rotated-sorted-array/', 7, NULL, NULL),
  ('50000000-0000-0000-0000-000000000008', 49,  'Group Anagrams',             'group-anagrams',            'Medium', 'Array, Hash Table, String, Sorting', 'https://leetcode.com/problems/group-anagrams/',        8,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000009', 53,  'Maximum Subarray',           'maximum-subarray',          'Medium', 'Array, Dynamic Programming', 'https://leetcode.com/problems/maximum-subarray/',           9,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000010', 70,  'Climbing Stairs',            'climbing-stairs',           'Easy',   'Dynamic Programming',        'https://leetcode.com/problems/climbing-stairs/',           10,  '<p>You are climbing a staircase. It takes n steps to reach the top. Each time you can either climb 1 or 2 steps.</p>', NOW()-INTERVAL '15 days'),
  ('50000000-0000-0000-0000-000000000011', 121, 'Best Time to Buy and Sell Stock', 'best-time-to-buy-and-sell-stock', 'Easy', 'Array, Dynamic Programming', 'https://leetcode.com/problems/best-time-to-buy-and-sell-stock/', 11, NULL, NULL),
  ('50000000-0000-0000-0000-000000000012', 128, 'Longest Consecutive Sequence', 'longest-consecutive-sequence', 'Medium', 'Array, Hash Table, Union Find', 'https://leetcode.com/problems/longest-consecutive-sequence/', 12, NULL, NULL),
  ('50000000-0000-0000-0000-000000000013', 200, 'Number of Islands',          'number-of-islands',         'Medium', 'Array, BFS, DFS, Matrix',    'https://leetcode.com/problems/number-of-islands/',         13,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000014', 206, 'Reverse Linked List',        'reverse-linked-list',       'Easy',   'Linked List, Recursion',     'https://leetcode.com/problems/reverse-linked-list/',       14,  NULL, NULL),
  ('50000000-0000-0000-0000-000000000015', 238, 'Product of Array Except Self', 'product-of-array-except-self', 'Medium', 'Array, Prefix Sum',    'https://leetcode.com/problems/product-of-array-except-self/', 15, NULL, NULL);

INSERT INTO leetcode_review (review_id, problem_id, confusion, approach, key_code, mastery, next_review_at, created_at, updated_at) VALUES
  ('51000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
   'Initially tried brute force O(n^2)', 'Hash map: store complement, one-pass O(n)', 'Map<Integer,Integer> map = new HashMap<>(); for(int i=0;i<nums.length;i++){if(map.containsKey(target-nums[i])) return new int[]{map.get(target-nums[i]),i}; map.put(nums[i],i);}',
   'confident', NOW() + INTERVAL '14 days', NOW()-INTERVAL '18 days', NOW()-INTERVAL '2 days'),
  ('51000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000005',
   'Forgot to handle nested brackets', 'Stack-based: push open, pop and match close', NULL,
   'confident', NOW() + INTERVAL '14 days', NOW()-INTERVAL '16 days', NOW()-INTERVAL '3 days'),
  ('51000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000010',
   'Confused by overlapping subproblems', 'dp[i] = dp[i-1] + dp[i-2], base cases dp[0]=1, dp[1]=1', NULL,
   'confident', NOW() + INTERVAL '21 days', NOW()-INTERVAL '14 days', NOW()-INTERVAL '5 days'),
  ('51000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000004',
   'Handling duplicates in sorted array', 'Sort + two pointers, skip duplicates with while loop', NULL,
   'partial', NOW() + INTERVAL '3 days', NOW()-INTERVAL '5 days', NOW()-INTERVAL '1 day'),
  ('51000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000007',
   'Edge case when mid is in the left sorted portion', 'Modified binary search: determine which half is sorted, then decide direction', NULL,
   'weak', NOW() + INTERVAL '1 day', NOW()-INTERVAL '2 days', NOW()),
  ('51000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000009',
   'Kadane''s algorithm not intuitive at first', 'Track currentMax and globalMax. currentMax = max(nums[i], currentMax + nums[i])', NULL,
   'partial', NOW() + INTERVAL '5 days', NOW()-INTERVAL '7 days', NOW()-INTERVAL '2 days'),
  ('51000000-0000-0000-0000-000000000007', '50000000-0000-0000-0000-000000000013',
   'BFS vs DFS choice', 'DFS flood fill: mark visited, recurse 4 directions, count connected components', NULL,
   'partial', NOW() + INTERVAL '4 days', NOW()-INTERVAL '3 days', NOW()),
  ('51000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000014',
   'Iterative vs recursive approach', 'Iterative: prev=null, curr=head, next=curr.next, curr.next=prev, shift all', NULL,
   'confident', NOW() + INTERVAL '14 days', NOW()-INTERVAL '10 days', NOW()-INTERVAL '4 days');

-- ============================================================
-- 6. BEHAVIORAL — QUESTIONS + STAR EVIDENCE + ANSWERS
-- ============================================================
INSERT INTO bhv_question (question_id, text, competency_topic, source_type, visibility, created_at, updated_at) VALUES
  ('60000000-0000-0000-0000-000000000001', 'Tell me about a time you had to work with a difficult team member.', 'TEAMWORK', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000002', 'Describe a project where you had to learn a new technology quickly.', 'LEARNING_AGILITY', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000003', 'Give an example of when you took initiative beyond your assigned tasks.', 'INITIATIVE', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000004', 'Tell me about a time you failed and what you learned from it.', 'RESILIENCE', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000005', 'Describe how you prioritise tasks when you have multiple deadlines.', 'TIME_MANAGEMENT', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000006', 'Tell me about a time you had to communicate a complex technical concept to a non-technical audience.', 'COMMUNICATION', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000007', 'Describe a situation where you had to make a decision with incomplete information.', 'DECISION_MAKING', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000008', 'Tell me about your most impactful contribution to a team project.', 'TEAMWORK', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000009', 'Give an example of when you had to adapt to a major change at work or school.', 'ADAPTABILITY', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW()),
  ('60000000-0000-0000-0000-000000000010', 'Describe a time you went above and beyond to deliver results.', 'INITIATIVE', 'CURATED', 'ACTIVE', NOW()-INTERVAL '20 days', NOW());

INSERT INTO bhv_star_evidence (evidence_id, title, status, created_at, updated_at) VALUES
  ('61000000-0000-0000-0000-000000000001', 'ByteDance — Recommendation Pipeline Redesign', 'DRAFT', NOW()-INTERVAL '10 days', NOW()),
  ('61000000-0000-0000-0000-000000000002', 'University — Capstone Project Leadership', 'DRAFT', NOW()-INTERVAL '8 days', NOW()),
  ('61000000-0000-0000-0000-000000000003', 'Huawei — CI/CD Migration Initiative', 'DRAFT', NOW()-INTERVAL '6 days', NOW());

INSERT INTO bhv_star_evidence_revision (revision_id, evidence_id, parent_revision_id, situation, task, action, result, competency_tags, created_at) VALUES
  ('62000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000001', NULL,
   'During my ByteDance internship, the recommendation pipeline had latency spikes during peak hours affecting 50M daily users.',
   'I was tasked with diagnosing the bottleneck and proposing a solution within my 4-month internship.',
   'Profiled the pipeline, identified a synchronous database call as the bottleneck. Proposed and implemented an async caching layer using Redis. Collaborated with 2 senior engineers for code review.',
   'Reduced p99 latency by 40%. The solution was adopted into the production pipeline and is still in use.',
   'TEAMWORK, INITIATIVE, LEARNING_AGILITY', NOW()-INTERVAL '10 days'),
  ('62000000-0000-0000-0000-000000000002', '61000000-0000-0000-0000-000000000002', NULL,
   'In my final year at ZJU, our 5-person capstone team had conflicting ideas about the project direction for our distributed file system.',
   'As the team lead, I needed to align everyone on a single architecture while keeping morale high.',
   'Organised a structured design review session where each member presented their approach. Facilitated a pros/cons matrix and let the team vote. Combined the best ideas.',
   'Delivered the project 2 weeks early with a grade of 95/100. All team members cited the process as a positive experience in peer reviews.',
   'TEAMWORK, COMMUNICATION, DECISION_MAKING', NOW()-INTERVAL '8 days'),
  ('62000000-0000-0000-0000-000000000003', '61000000-0000-0000-0000-000000000003', NULL,
   'At Huawei Cloud, the team used manual deployment scripts that took 45 minutes per release and were error-prone.',
   'No one had been assigned to fix this. I noticed the pain point and proposed automating the deployment pipeline.',
   'Built a Jenkins + Docker pipeline on my own initiative. Wrote documentation and trained the team. Handled resistance from a senior developer who preferred the manual process by demonstrating time savings.',
   'Deployment time dropped from 45 minutes to 8 minutes. Error rate went from ~10% to near zero. The pipeline was adopted team-wide.',
   'INITIATIVE, RESILIENCE, COMMUNICATION', NOW()-INTERVAL '6 days');

-- Link current_revision_id
UPDATE bhv_star_evidence SET current_revision_id = '62000000-0000-0000-0000-000000000001' WHERE evidence_id = '61000000-0000-0000-0000-000000000001';
UPDATE bhv_star_evidence SET current_revision_id = '62000000-0000-0000-0000-000000000002' WHERE evidence_id = '61000000-0000-0000-0000-000000000002';
UPDATE bhv_star_evidence SET current_revision_id = '62000000-0000-0000-0000-000000000003' WHERE evidence_id = '61000000-0000-0000-0000-000000000003';

INSERT INTO bhv_answer (answer_id, question_id, status, created_at, updated_at) VALUES
  ('63000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', 'IN_PROGRESS', NOW()-INTERVAL '5 days', NOW()),
  ('63000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', 'IN_PROGRESS', NOW()-INTERVAL '4 days', NOW());

INSERT INTO bhv_answer_version (version_id, answer_id, parent_version_id, version_number, question_text_snapshot, body_text, created_at) VALUES
  ('64000000-0000-0000-0000-000000000001', '63000000-0000-0000-0000-000000000001', NULL, 1,
   'Tell me about a time you had to work with a difficult team member.',
   'During my capstone project at ZJU, one team member consistently missed deadlines and pushed back on code reviews. I scheduled a 1-on-1 to understand their perspective — they were struggling with the distributed systems concepts. I paired with them for a week, which helped them catch up. We delivered on time and they became one of the strongest contributors.',
   NOW()-INTERVAL '5 days'),
  ('64000000-0000-0000-0000-000000000002', '63000000-0000-0000-0000-000000000002', NULL, 1,
   'Give an example of when you took initiative beyond your assigned tasks.',
   'At Huawei Cloud, I noticed our deployment process was manual and error-prone, taking 45 minutes per release. Even though it was not my responsibility, I built a Jenkins + Docker CI/CD pipeline that reduced deployment time to 8 minutes. I had to convince a skeptical senior engineer by demonstrating the time savings with real metrics.',
   NOW()-INTERVAL '4 days');

UPDATE bhv_answer SET current_version_id = '64000000-0000-0000-0000-000000000001' WHERE answer_id = '63000000-0000-0000-0000-000000000001';
UPDATE bhv_answer SET current_version_id = '64000000-0000-0000-0000-000000000002' WHERE answer_id = '63000000-0000-0000-0000-000000000002';

INSERT INTO bhv_answer_version_evidence VALUES
  ('64000000-0000-0000-0000-000000000001', '62000000-0000-0000-0000-000000000002'),
  ('64000000-0000-0000-0000-000000000002', '62000000-0000-0000-0000-000000000003');

-- ============================================================
-- 7. CV DOCUMENTS (metadata only — no real files on disk)
-- ============================================================
INSERT INTO cv_document (document_id, display_name, original_filename, original_path, pdf_path, content_type_original, size_bytes_original, size_bytes_pdf, pdf_ready, created_at, updated_at) VALUES
  ('70000000-0000-0000-0000-000000000001', 'CV — General SWE (v3)', 'CV_SWE_General_v3.pdf', 'data/cv/cv-swe-v3.pdf', 'data/cv/cv-swe-v3.pdf', 'application/pdf', 245760, 245760, TRUE, NOW()-INTERVAL '10 days', NOW()-INTERVAL '2 days'),
  ('70000000-0000-0000-0000-000000000002', 'CV — Backend Focused (v2)', 'CV_Backend_v2.pdf', 'data/cv/cv-backend-v2.pdf', 'data/cv/cv-backend-v2.pdf', 'application/pdf', 231424, 231424, TRUE, NOW()-INTERVAL '8 days', NOW()-INTERVAL '3 days'),
  ('70000000-0000-0000-0000-000000000003', 'CV — Data Engineering', 'CV_DataEng.docx', 'data/cv/cv-dataeng.docx', NULL, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 198656, 0, FALSE, NOW()-INTERVAL '5 days', NOW()-INTERVAL '5 days');

-- ============================================================
-- 8. EMAIL APPLICATIONS
-- ============================================================
INSERT INTO email_application (app_id, company_name, role_name, category, is_swe, status, notes, created_at, updated_at) VALUES
  ('80000000-0000-0000-0000-000000000001', 'Stripe',      'Graduate SWE',                  'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '5 days', NOW()),
  ('80000000-0000-0000-0000-000000000002', 'Workday',     'New Grad SWE',                  'grad_swe',      TRUE,  'oa',         'OA completed, awaiting result',   NOW()-INTERVAL '8 days', NOW()),
  ('80000000-0000-0000-0000-000000000003', 'Intercom',    'Junior Backend Developer',      'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '2 days', NOW()),
  ('80000000-0000-0000-0000-000000000004', 'Hubspot',     'Backend Developer',             'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '2 days', NOW()),
  ('80000000-0000-0000-0000-000000000005', 'Salesforce',  'SE I',                          'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '5 days', NOW()),
  ('80000000-0000-0000-0000-000000000006', 'Mastercard',  'Platform Engineer Graduate',    'grad_tech',     FALSE, 'applied',    NULL,                             NOW()-INTERVAL '6 days', NOW()),
  ('80000000-0000-0000-0000-000000000007', 'Fidelity',    'Junior Full-Stack Developer',   'grad_tech',     FALSE, 'offer',      'Offer received 11/8, deadline 11/20', NOW()-INTERVAL '10 days', NOW()),
  ('80000000-0000-0000-0000-000000000008', 'Version 1',   'DevOps Graduate',               'grad_tech',     FALSE, 'applied',    NULL,                             NOW()-INTERVAL '3 days', NOW()),
  ('80000000-0000-0000-0000-000000000009', 'Accenture',   'Graduate Data Engineer',        'grad_other',    FALSE, 'video',      'Video interview done 11/8',       NOW()-INTERVAL '7 days', NOW()),
  ('80000000-0000-0000-0000-000000000010', 'Kerry Group', 'IT Graduate — Business Systems','grad_other',    FALSE, 'rejected',   NULL,                             NOW()-INTERVAL '12 days', NOW()),
  ('80000000-0000-0000-0000-000000000011', 'Citi',        'Junior Java Developer',         'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '1 day', NOW()),
  ('80000000-0000-0000-0000-000000000012', 'Google',      'SWE Intern Summer 2027',        'intern_summer', TRUE,  'oa',         'OA link received',                NOW()-INTERVAL '10 days', NOW()),
  ('80000000-0000-0000-0000-000000000013', 'Meta',        'ML Engineer Intern',            'intern_summer', TRUE,  'applied',    NULL,                             NOW()-INTERVAL '4 days', NOW()),
  ('80000000-0000-0000-0000-000000000014', 'Amazon',      'SDE Intern (6 months)',          'intern_long',   TRUE,  'hackerrank', 'HackerRank sent',                NOW()-INTERVAL '9 days', NOW()),
  ('80000000-0000-0000-0000-000000000015', 'Microsoft',   'Software Engineer Intern',      'intern_summer', TRUE,  'rejected',   NULL,                             NOW()-INTERVAL '15 days', NOW()),
  ('80000000-0000-0000-0000-000000000016', 'JPMorgan',    'SEP Summer Intern 2027',        'intern_summer', TRUE,  'hackerrank', 'HackerRank scheduled',            NOW()-INTERVAL '7 days', NOW()),
  ('80000000-0000-0000-0000-000000000017', 'Optiver',     'SWE Intern',                    'intern_unknown',TRUE,  'rejected',   NULL,                             NOW()-INTERVAL '14 days', NOW()),
  ('80000000-0000-0000-0000-000000000018', 'Jane Street', 'SWE Intern',                    'intern_unknown',TRUE,  'rejected',   NULL,                             NOW()-INTERVAL '13 days', NOW()),
  ('80000000-0000-0000-0000-000000000019', 'Zendesk',     'QA Automation Engineer',        'other',         FALSE, 'rejected',   'Role didn''t match expectations', NOW()-INTERVAL '8 days', NOW()),
  ('80000000-0000-0000-0000-000000000020', 'Pinterest',   'SWE Intern Dublin',             'intern_summer', TRUE,  'applied',    NULL,                             NOW()-INTERVAL '3 days', NOW()),
  ('80000000-0000-0000-0000-000000000021', 'Cloudflare',  'Systems Engineer Graduate',     'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '4 days', NOW()),
  ('80000000-0000-0000-0000-000000000022', 'Databricks',  'Software Engineer New Grad',    'grad_swe',      TRUE,  'oa',         'OA completed',                    NOW()-INTERVAL '6 days', NOW()),
  ('80000000-0000-0000-0000-000000000023', 'Revolut',     'Graduate Backend Engineer',     'grad_swe',      TRUE,  'applied',    NULL,                             NOW()-INTERVAL '3 days', NOW()),
  ('80000000-0000-0000-0000-000000000024', 'TikTok',      'Backend Engineer Intern',       'intern_unknown',TRUE,  'oa',         'Online assessment sent',           NOW()-INTERVAL '5 days', NOW()),
  ('80000000-0000-0000-0000-000000000025', 'Deloitte',    'Technology Consulting Graduate','grad_other',    FALSE, 'applied',    NULL,                             NOW()-INTERVAL '7 days', NOW());

COMMIT;

-- Verify counts
SELECT 'profile'           AS tbl, count(*) FROM profile
UNION ALL SELECT 'education_period',   count(*) FROM education_period
UNION ALL SELECT 'canonical_job',      count(*) FROM canonical_job
UNION ALL SELECT 'action_item',        count(*) FROM action_item
UNION ALL SELECT 'roadmap_folder',     count(*) FROM roadmap_folder
UNION ALL SELECT 'roadmap_todo',       count(*) FROM roadmap_todo
UNION ALL SELECT 'roadmap_company',    count(*) FROM roadmap_company
UNION ALL SELECT 'roadmap_document',   count(*) FROM roadmap_document
UNION ALL SELECT 'leetcode_problem',   count(*) FROM leetcode_problem
UNION ALL SELECT 'leetcode_review',    count(*) FROM leetcode_review
UNION ALL SELECT 'bhv_question',       count(*) FROM bhv_question
UNION ALL SELECT 'bhv_star_evidence',  count(*) FROM bhv_star_evidence
UNION ALL SELECT 'bhv_answer',         count(*) FROM bhv_answer
UNION ALL SELECT 'cv_document',        count(*) FROM cv_document
UNION ALL SELECT 'email_application',  count(*) FROM email_application
ORDER BY tbl;
