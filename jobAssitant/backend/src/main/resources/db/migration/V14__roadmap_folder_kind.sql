-- Folder kinds: todolist | companytracker. Rename seed folder; company tracker rows.

ALTER TABLE roadmap_folder
    ADD COLUMN kind VARCHAR(32) NOT NULL DEFAULT 'todolist';

UPDATE roadmap_folder
SET name = 'todolist', kind = 'todolist'
WHERE folder_id = 'aaaaaaaa-bbbb-cccc-dddd-000000000001';

UPDATE roadmap_folder
SET kind = 'todolist'
WHERE kind IS NULL OR kind = '';

CREATE TABLE roadmap_company (
    company_id UUID PRIMARY KEY,
    folder_id UUID NOT NULL REFERENCES roadmap_folder(folder_id) ON DELETE CASCADE,
    company_name VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'watching',
    contact VARCHAR(512),
    note TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_roadmap_company_folder ON roadmap_company(folder_id, sort_order, updated_at DESC);
CREATE INDEX idx_roadmap_folder_kind ON roadmap_folder(kind, sort_order);
