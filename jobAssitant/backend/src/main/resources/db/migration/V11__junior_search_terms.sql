-- Bias global search terms toward junior / graduate discovery (FreeHire q= + JobSpy).
UPDATE job_search_settings
SET search_terms = 'junior software engineer,graduate software developer,graduate software engineer,junior backend developer,software engineer',
    updated_at = NOW()
WHERE id = 1;
