-- CV file management + Behavioral domain

CREATE TABLE cv_document (
    document_id UUID PRIMARY KEY,
    display_name VARCHAR(512) NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    original_path VARCHAR(1024) NOT NULL,
    pdf_path VARCHAR(1024) NOT NULL,
    content_type_original VARCHAR(128) NOT NULL,
    size_bytes_original BIGINT NOT NULL,
    size_bytes_pdf BIGINT NOT NULL,
    pdf_ready BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_question (
    question_id UUID PRIMARY KEY,
    text TEXT NOT NULL,
    competency_topic VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    curation_version VARCHAR(32),
    visibility VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_star_evidence (
    evidence_id UUID PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_revision_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_star_evidence_revision (
    revision_id UUID PRIMARY KEY,
    evidence_id UUID NOT NULL REFERENCES bhv_star_evidence(evidence_id) ON DELETE CASCADE,
    parent_revision_id UUID,
    situation TEXT,
    task TEXT,
    action TEXT,
    result TEXT,
    competency_tags TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_answer (
    answer_id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES bhv_question(question_id),
    current_version_id UUID,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_answer_version (
    version_id UUID PRIMARY KEY,
    answer_id UUID NOT NULL REFERENCES bhv_answer(answer_id) ON DELETE CASCADE,
    parent_version_id UUID,
    version_number INT NOT NULL,
    question_text_snapshot TEXT NOT NULL,
    body_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_answer_version_evidence (
    version_id UUID NOT NULL REFERENCES bhv_answer_version(version_id) ON DELETE CASCADE,
    evidence_revision_id UUID NOT NULL REFERENCES bhv_star_evidence_revision(revision_id),
    PRIMARY KEY (version_id, evidence_revision_id)
);

CREATE TABLE bhv_answer_feedback (
    feedback_id UUID PRIMARY KEY,
    answer_version_id UUID NOT NULL REFERENCES bhv_answer_version(version_id) ON DELETE CASCADE,
    rule_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE bhv_answer_feedback_item (
    item_id UUID PRIMARY KEY,
    feedback_id UUID NOT NULL REFERENCES bhv_answer_feedback(feedback_id) ON DELETE CASCADE,
    dimension VARCHAR(64) NOT NULL,
    rule_id VARCHAR(64) NOT NULL,
    target_quote TEXT,
    issue TEXT,
    rationale TEXT,
    suggestion TEXT,
    origin VARCHAR(64) NOT NULL,
    decision VARCHAR(32),
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_bhv_question_topic ON bhv_question(competency_topic, visibility);
CREATE INDEX idx_bhv_answer_question ON bhv_answer(question_id);

-- Curated question bank v1 (24)
INSERT INTO bhv_question (question_id, text, competency_topic, source_type, curation_version, visibility, created_at, updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa001', 'Tell me about a time you worked on a cross-functional team where members had different priorities. How did you align everyone toward delivery?', 'teamwork', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa002', 'Describe a situation where you had to rely on a teammate who was struggling to meet their commitments. What did you do?', 'teamwork', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa003', 'Give an example of when you received constructive feedback from a peer during a project. How did you respond?', 'teamwork', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa004', 'Tell me about a time you had to onboard or mentor a new team member while still meeting your own deadlines.', 'teamwork', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa005', 'Describe a time you took the lead on a technical initiative without being formally assigned as the lead.', 'leadership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa006', 'Tell me about a decision you made that affected your team''s roadmap or priorities. How did you communicate it?', 'leadership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa007', 'Give an example of when you delegated work to others. How did you choose who did what and follow up?', 'leadership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa008', 'Tell me about a time you had to motivate others during a difficult sprint or release cycle.', 'leadership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa009', 'Describe a disagreement you had with a colleague about a technical approach. How was it resolved?', 'conflict', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa010', 'Tell me about a time you pushed back on a stakeholder request that you believed was unrealistic or risky.', 'conflict', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa011', 'Give an example of when two teammates were in conflict and you helped de-escalate the situation.', 'conflict', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa012', 'Tell me about a time you received feedback you strongly disagreed with. What did you do next?', 'conflict', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa013', 'Describe a project or feature that did not go as planned. What happened and what did you learn?', 'failure', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa014', 'Tell me about a mistake you made in production or near-production. How did you detect and fix it?', 'failure', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa015', 'Give an example of when you missed a deadline or commitment. What caused it and how did you handle it?', 'failure', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa016', 'Tell me about a time an experiment or proof-of-concept failed. Why did you pursue it and what changed afterward?', 'failure', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa017', 'Describe a time you had to explain a complex technical topic to a non-technical audience.', 'communication', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa018', 'Tell me about a situation where unclear requirements caused rework. How did you improve clarity?', 'communication', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa019', 'Give an example of written communication (design doc, RFC, status update) that influenced a team decision.', 'communication', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa020', 'Tell me about a time you had to deliver difficult news about delays or scope cuts to stakeholders.', 'communication', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa021', 'Describe a time you noticed a problem outside your immediate scope and took initiative to fix it.', 'ownership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa022', 'Tell me about the most ambiguous task you owned end-to-end. How did you define success?', 'ownership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa023', 'Give an example of when you improved code quality, reliability, or developer experience without being asked.', 'ownership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaa024', 'Tell me about a time you balanced short-term delivery pressure with long-term maintainability.', 'ownership', 'CURATED', 'v1', 'ACTIVE', NOW(), NOW());
