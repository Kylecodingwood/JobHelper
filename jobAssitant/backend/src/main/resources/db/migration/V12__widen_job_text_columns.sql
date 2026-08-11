-- FreeHire titles/companies/locations can exceed original varchar limits.
ALTER TABLE canonical_job ALTER COLUMN title TYPE VARCHAR(1024);
ALTER TABLE canonical_job ALTER COLUMN company TYPE VARCHAR(512);
ALTER TABLE canonical_job ALTER COLUMN location TYPE VARCHAR(512);
ALTER TABLE job_source_run ALTER COLUMN diagnostics TYPE TEXT;
ALTER TABLE job_source_diagnostic ALTER COLUMN message TYPE TEXT;
