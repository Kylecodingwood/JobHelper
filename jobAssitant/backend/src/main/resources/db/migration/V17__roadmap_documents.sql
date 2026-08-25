-- Roadmap document folders: long-form notes (AI tips, etc.).

CREATE TABLE roadmap_document (
    document_id UUID PRIMARY KEY,
    folder_id UUID NOT NULL REFERENCES roadmap_folder(folder_id) ON DELETE CASCADE,
    title VARCHAR(512) NOT NULL,
    body_html TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_roadmap_document_folder ON roadmap_document(folder_id, sort_order, updated_at DESC);
