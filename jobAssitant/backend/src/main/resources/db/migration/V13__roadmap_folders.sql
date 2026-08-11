-- Roadmap multi-folder: folders contain todos (no nesting).

CREATE TABLE roadmap_folder (
    folder_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Stable default folder for migrating existing flat todos.
INSERT INTO roadmap_folder (folder_id, name, sort_order, created_at, updated_at)
VALUES ('aaaaaaaa-bbbb-cccc-dddd-000000000001', '默认', 0, NOW(), NOW());

ALTER TABLE roadmap_todo
    ADD COLUMN folder_id UUID REFERENCES roadmap_folder(folder_id) ON DELETE CASCADE;

UPDATE roadmap_todo
SET folder_id = 'aaaaaaaa-bbbb-cccc-dddd-000000000001'
WHERE folder_id IS NULL;

ALTER TABLE roadmap_todo
    ALTER COLUMN folder_id SET NOT NULL;

CREATE INDEX idx_roadmap_todo_folder ON roadmap_todo(folder_id, done, sort_order, updated_at DESC);
CREATE INDEX idx_roadmap_folder_sort ON roadmap_folder(sort_order, updated_at DESC);
