-- V4: Roadmap templates (SYSTEM + USER), template versioning, durable preview sessions,
-- dependency override persistence, and roadmap -> system template version tracking.
-- (task_dependency table already exists from V2__m1_pipeline.sql.)

CREATE TABLE roadmap_template (
    template_id UUID PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(256) NOT NULL,
    type VARCHAR(32) NOT NULL, -- SYSTEM | USER
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE roadmap_template_version (
    template_version_id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES roadmap_template(template_id) ON DELETE CASCADE,
    version INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- DRAFT | ACTIVE | RETIRED
    markdown_content TEXT,
    phases_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (template_id, version)
);

CREATE TABLE template_task_definition (
    template_task_definition_id UUID PRIMARY KEY,
    template_version_id UUID NOT NULL REFERENCES roadmap_template_version(template_version_id) ON DELETE CASCADE,
    template_task_key VARCHAR(128) NOT NULL,
    title VARCHAR(512) NOT NULL,
    phase VARCHAR(128) NOT NULL,
    anchor_type VARCHAR(64),
    relative_offset_days INT NOT NULL DEFAULT 0,
    completion_criteria VARCHAR(1024),
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    depends_on_keys JSONB,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_template_task_definition_version ON template_task_definition(template_version_id);

CREATE TABLE preview_session (
    preview_token VARCHAR(128) PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_preview_session_expires ON preview_session(expires_at);

CREATE TABLE task_dependency_override (
    task_id UUID PRIMARY KEY REFERENCES roadmap_task(task_id) ON DELETE CASCADE,
    reason VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE roadmap
    ADD COLUMN IF NOT EXISTS system_template_version_id UUID;

-- Seed SYSTEM template (matches the M1 default task set, now DB-driven).
INSERT INTO roadmap_template (template_id, code, name, type, created_at, updated_at) VALUES
  ('22222222-2222-2222-2222-222222222201', 'SYSTEM_IE_GRADUATE', 'IE Graduate default', 'SYSTEM', NOW(), NOW());

INSERT INTO roadmap_template_version (template_version_id, template_id, version, status, markdown_content, phases_json, created_at) VALUES
  ('22222222-2222-2222-2222-222222222202', '22222222-2222-2222-2222-222222222201', 1, 'ACTIVE', NULL,
   '["定位与资格","材料准备","投递执行","面试准备","收尾"]'::jsonb, NOW());

INSERT INTO template_task_definition
  (template_task_definition_id, template_version_id, template_task_key, title, phase, anchor_type, relative_offset_days, completion_criteria, priority, depends_on_keys, sort_order)
VALUES
  ('22222222-2222-2222-2222-222222222211', '22222222-2222-2222-2222-222222222202', 'profile.complete', '完善求职 Profile', '定位与资格', 'COURSE_START', 0, 'Education / Work Auth / Target Role 齐全', 'HIGH', '[]'::jsonb, 0),
  ('22222222-2222-2222-2222-222222222212', '22222222-2222-2222-2222-222222222202', 'auth.review', '核对工作许可有效期', '定位与资格', 'STAMP_EXPIRY', -90, '记录 Stamp 到期与续签计划', 'HIGH', '["profile.complete"]'::jsonb, 1),
  ('22222222-2222-2222-2222-222222222213', '22222222-2222-2222-2222-222222222202', 'cv.base-draft', '建立基础 Graduate CV 草稿', '材料准备', 'GRADUATE_RECRUITMENT_SEASON', -30, '一页 CV 初稿可投递', 'HIGH', '["profile.complete"]'::jsonb, 2),
  ('22222222-2222-2222-2222-222222222214', '22222222-2222-2222-2222-222222222202', 'jobs.sync', '启动职位同步并建立 Shortlist', '投递执行', 'GRADUATE_RECRUITMENT_SEASON', 0, '至少同步一次 FreeHire/JobSpy', 'MEDIUM', '["cv.base-draft"]'::jsonb, 3),
  ('22222222-2222-2222-2222-222222222215', '22222222-2222-2222-2222-222222222202', 'interview.prep', '准备常见行为面试题', '面试准备', 'GRADUATION', -60, '完成 5 道 STAR 草稿', 'MEDIUM', '["cv.base-draft"]'::jsonb, 4),
  ('22222222-2222-2222-2222-222222222216', '22222222-2222-2222-2222-222222222202', 'grad.check', '毕业前材料与签证清单', '收尾', 'GRADUATION', -14, '材料与许可检查完成', 'HIGH', '["auth.review","jobs.sync","interview.prep"]'::jsonb, 5);
