-- M1 pipeline: sources, runs, roadmap, job evaluation columns

CREATE TABLE job_source (
    source_id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    adapter_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(32) NOT NULL,
    search_terms_override TEXT,
    risk_note VARCHAR(1024),
    config_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE job_source_run (
    run_id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES job_source(source_id),
    trigger_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    parameters_snapshot TEXT,
    request_count INT NOT NULL DEFAULT 0,
    received_count INT NOT NULL DEFAULT 0,
    valid_count INT NOT NULL DEFAULT 0,
    created_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    diagnostics TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE canonical_job
    ADD COLUMN IF NOT EXISTS source_stable_id VARCHAR(512),
    ADD COLUMN IF NOT EXISTS seniority VARCHAR(64),
    ADD COLUMN IF NOT EXISTS gate_dimensions_json TEXT,
    ADD COLUMN IF NOT EXISTS rank_factors_json TEXT,
    ADD COLUMN IF NOT EXISTS first_seen_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_job_source_stable
    ON canonical_job (preferred_source_code, source_stable_id)
    WHERE source_stable_id IS NOT NULL;

CREATE TABLE roadmap (
    roadmap_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    profile_version INT NOT NULL,
    rule_version VARCHAR(256) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE roadmap_task (
    task_id UUID PRIMARY KEY,
    roadmap_id UUID NOT NULL REFERENCES roadmap(roadmap_id) ON DELETE CASCADE,
    logical_task_key VARCHAR(128) NOT NULL,
    title VARCHAR(512) NOT NULL,
    phase VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    actionability VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    origin VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    user_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    user_edited BOOLEAN NOT NULL DEFAULT FALSE,
    completion_criteria VARCHAR(1024),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE task_dependency (
    predecessor_task_id UUID NOT NULL REFERENCES roadmap_task(task_id) ON DELETE CASCADE,
    successor_task_id UUID NOT NULL REFERENCES roadmap_task(task_id) ON DELETE CASCADE,
    PRIMARY KEY (predecessor_task_id, successor_task_id)
);

INSERT INTO job_source (source_id, code, adapter_type, enabled, role, search_terms_override, risk_note, config_version, created_at, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111101', 'freehire', 'FREEHIRE_API', TRUE, 'PRIMARY', NULL, 'FreeHire public API', 1, NOW(), NOW()),
  ('11111111-1111-1111-1111-111111111102', 'jobspy', 'JOBSPY', TRUE, 'SUPPLEMENTAL', NULL, 'JobSpy Python subprocess; ToS/rate-limit risk', 1, NOW(), NOW()),
  ('11111111-1111-1111-1111-111111111103', 'manual_url', 'MANUAL', TRUE, 'MANUAL', NULL, 'User pasted URL', 1, NOW(), NOW());
