-- M1 complete: outbox, recompute, backups, generation, decisions, diagnostics

CREATE TABLE domain_event_outbox (
    event_id UUID PRIMARY KEY,
    source_domain VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE domain_event_receipt (
    receipt_id UUID PRIMARY KEY,
    consumer_name VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (consumer_name, event_id)
);

CREATE TABLE recompute_request (
    recompute_request_id UUID PRIMARY KEY,
    profile_version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    scopes TEXT NOT NULL,
    change_summary TEXT,
    impact_summary TEXT,
    preview_token VARCHAR(128),
    preview_expires_at TIMESTAMP WITH TIME ZONE,
    preview_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE profile_backup_metadata (
    backup_id UUID PRIMARY KEY,
    backup_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    checksum VARCHAR(128),
    domains_included TEXT,
    retention_rank INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE generation_record (
    generation_record_id UUID PRIMARY KEY,
    roadmap_id UUID NOT NULL REFERENCES roadmap(roadmap_id) ON DELETE CASCADE,
    mode VARCHAR(32) NOT NULL,
    input_hash VARCHAR(128) NOT NULL,
    profile_version INT NOT NULL,
    preview_token VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE job_decision (
    decision_id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES canonical_job(job_id) ON DELETE CASCADE,
    decision_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE job_source_diagnostic (
    diagnostic_id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES job_source_run(run_id) ON DELETE CASCADE,
    category VARCHAR(64) NOT NULL,
    site VARCHAR(64),
    message TEXT,
    item_key VARCHAR(256),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE action_decision (
    decision_id UUID PRIMARY KEY,
    action_id UUID NOT NULL REFERENCES action_item(action_id) ON DELETE CASCADE,
    operation VARCHAR(32) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE roadmap_task
    ADD COLUMN IF NOT EXISTS blocking_task_keys TEXT;

CREATE INDEX idx_outbox_unpublished ON domain_event_outbox(published_at) WHERE published_at IS NULL;
CREATE INDEX idx_recompute_status ON recompute_request(status);
