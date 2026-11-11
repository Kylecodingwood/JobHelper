CREATE TABLE email_application (
    app_id      UUID PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    role_name   TEXT NOT NULL,
    category    VARCHAR(50) NOT NULL,
    is_swe      BOOLEAN NOT NULL DEFAULT TRUE,
    status      VARCHAR(30) NOT NULL DEFAULT 'applied',
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_email_app UNIQUE (company_name, role_name)
);
