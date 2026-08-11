-- Pivot 2026-08-09: persona profile, notion todos, source-owned search terms

ALTER TABLE profile
    ADD COLUMN IF NOT EXISTS nationality VARCHAR(8),
    ADD COLUMN IF NOT EXISTS identity_status VARCHAR(64),
    ADD COLUMN IF NOT EXISTS identity_valid_until DATE,
    ADD COLUMN IF NOT EXISTS target_country VARCHAR(8),
    ADD COLUMN IF NOT EXISTS job_seeking_goal TEXT;

-- Migrate first work_authorization into identity columns when empty
UPDATE profile p
SET identity_status = wa.permission_type,
    identity_valid_until = wa.valid_until
FROM (
    SELECT DISTINCT ON (profile_id) profile_id, permission_type, valid_until
    FROM work_authorization
    ORDER BY profile_id, work_authorization_id
) wa
WHERE p.profile_id = wa.profile_id
  AND (p.identity_status IS NULL OR p.identity_status = '');

CREATE TABLE IF NOT EXISTS work_experience (
    work_experience_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    company VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    start_date DATE,
    end_date DATE,
    summary TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS profile_skill (
    profile_skill_id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profile(profile_id) ON DELETE CASCADE,
    skill_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS roadmap_todo (
    todo_id UUID PRIMARY KEY,
    name VARCHAR(512) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    comment TEXT,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_roadmap_todo_sort ON roadmap_todo(done, sort_order, updated_at DESC);

-- Ensure job_source.search_terms_override is the sole user-owned terms column (API: searchTerms)
COMMENT ON COLUMN job_source.search_terms_override IS 'User-owned search terms (Pivot); not derived from Profile';
