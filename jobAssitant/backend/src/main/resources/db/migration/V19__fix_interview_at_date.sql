ALTER TABLE roadmap_company
    ALTER COLUMN interview_at TYPE DATE USING interview_at::DATE;
