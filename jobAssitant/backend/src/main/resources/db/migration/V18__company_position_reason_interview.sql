ALTER TABLE roadmap_company
    ADD COLUMN IF NOT EXISTS position     TEXT,
    ADD COLUMN IF NOT EXISTS reason       TEXT,
    ADD COLUMN IF NOT EXISTS interview_at TIMESTAMPTZ;
