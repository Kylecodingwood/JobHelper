-- Prefer software-focused search terms over a single "graduate …" query
UPDATE job_search_settings
SET search_terms = 'software engineer,junior software engineer,graduate software developer,backend developer',
    updated_at = NOW()
WHERE id = 1
  AND (
    search_terms IS NULL
    OR TRIM(search_terms) = ''
    OR TRIM(search_terms) = 'graduate software engineer'
  );
