CREATE TABLE profile (
    profile_id UUID PRIMARY KEY,
    profile_version INT NOT NULL,
    lifecycle VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE education_period (
    education_period_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    institution_name VARCHAR(255) NOT NULL,
    programme_name VARCHAR(255),
    start_date DATE,
    expected_graduation_date DATE,
    country_code VARCHAR(8),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE work_authorization (
    work_authorization_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    permission_type VARCHAR(64) NOT NULL,
    valid_from DATE,
    valid_until DATE,
    weekly_hours_limit DECIMAL(5,2),
    is_future BOOLEAN NOT NULL DEFAULT FALSE,
    country_code VARCHAR(8)
);

CREATE TABLE target_role (
    target_role_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    role_name VARCHAR(255) NOT NULL,
    seniority VARCHAR(64),
    location VARCHAR(255),
    priority_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE language_proficiency (
    language_proficiency_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    language_code VARCHAR(16) NOT NULL,
    proficiency VARCHAR(32) NOT NULL,
    evidence_ref VARCHAR(512),
    notes VARCHAR(1024)
);

CREATE TABLE action_item (
    action_id UUID PRIMARY KEY,
    source_domain VARCHAR(32) NOT NULL,
    action_kind VARCHAR(64) NOT NULL,
    title VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deadline TIMESTAMP WITH TIME ZONE,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    priority_band VARCHAR(32) NOT NULL,
    priority_sort_key VARCHAR(128) NOT NULL,
    target_type VARCHAR(64),
    target_id UUID,
    route_hint VARCHAR(512),
    focus_key VARCHAR(256),
    primary_reason VARCHAR(1024),
    version INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE canonical_job (
    job_id UUID PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    company VARCHAR(255),
    location VARCHAR(255),
    job_status VARCHAR(32) NOT NULL,
    validity_status VARCHAR(32) NOT NULL,
    gate_status VARCHAR(32) NOT NULL,
    rank_tier VARCHAR(32) NOT NULL,
    hidden_by_default BOOLEAN NOT NULL DEFAULT FALSE,
    expected_start_date DATE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    preferred_source_code VARCHAR(64),
    has_pending_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL,
    description VARCHAR(10000),
    canonical_apply_url VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_action_active_status ON action_item(active, status);
CREATE INDEX idx_job_status_hidden ON canonical_job(job_status, hidden_by_default);
