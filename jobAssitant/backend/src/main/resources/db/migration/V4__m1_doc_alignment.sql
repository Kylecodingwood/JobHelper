-- M1 doc alignment: job_source_ref (job-model §3.6) + job_possible_duplicate (job-model §3.9)

CREATE TABLE job_source_ref (
    job_source_ref_id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES canonical_job(job_id) ON DELETE CASCADE,
    source_code VARCHAR(64) NOT NULL,
    source_stable_id VARCHAR(512),
    apply_url VARCHAR(1024),
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_job_source_ref_job_id ON job_source_ref(job_id);

-- One (source, stable id) pair links to at most one active job's source ref.
CREATE UNIQUE INDEX uq_job_source_ref_source ON job_source_ref(source_code, source_stable_id)
    WHERE source_stable_id IS NOT NULL;

CREATE TABLE job_possible_duplicate (
    duplicate_id UUID PRIMARY KEY,
    left_job_id UUID NOT NULL REFERENCES canonical_job(job_id) ON DELETE CASCADE,
    right_job_id UUID NOT NULL REFERENCES canonical_job(job_id) ON DELETE CASCADE,
    signals JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    decided_by_decision_id UUID REFERENCES job_decision(decision_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Unordered pair: app always writes LEAST(...)/GREATEST(...) into left/right so this
    -- also blocks storing the same pair twice in swapped order.
    CONSTRAINT chk_job_possible_duplicate_order CHECK (left_job_id < right_job_id)
);

CREATE UNIQUE INDEX uq_job_possible_duplicate_pair ON job_possible_duplicate(left_job_id, right_job_id);
CREATE INDEX idx_job_possible_duplicate_status ON job_possible_duplicate(status);
