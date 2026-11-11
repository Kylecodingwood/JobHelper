ALTER TABLE roadmap_company
    ALTER COLUMN interview_at TYPE TEXT USING interview_at::TEXT;
