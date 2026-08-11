-- Global search terms shared by all job sources (FreeHire, JobSpy, …)

CREATE TABLE job_search_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    search_terms TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO job_search_settings (id, search_terms, updated_at)
SELECT 1,
       COALESCE(
           (SELECT search_terms_override
            FROM job_source
            WHERE code = 'freehire'
              AND search_terms_override IS NOT NULL
              AND TRIM(search_terms_override) <> ''),
           (SELECT search_terms_override
            FROM job_source
            WHERE search_terms_override IS NOT NULL
              AND TRIM(search_terms_override) <> ''
            ORDER BY code
            LIMIT 1),
           ''
       ),
       NOW();

COMMENT ON TABLE job_search_settings IS 'Singleton: user-owned search terms applied to every syncable source';
COMMENT ON COLUMN job_source.search_terms_override IS 'Deprecated: use job_search_settings; retained for migration/history';
