-- LeetCode Hot 100 catalog + per-problem review notes.

CREATE TABLE leetcode_problem (
    problem_id UUID PRIMARY KEY,
    problem_number INT NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    tags TEXT NOT NULL,
    url VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE leetcode_review (
    review_id UUID PRIMARY KEY,
    problem_id UUID NOT NULL UNIQUE REFERENCES leetcode_problem(problem_id) ON DELETE CASCADE,
    confusion TEXT NOT NULL,
    approach TEXT,
    key_code TEXT,
    mastery VARCHAR(32) NOT NULL,
    next_review_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_leetcode_problem_sort ON leetcode_problem(sort_order, problem_number);
CREATE INDEX idx_leetcode_review_mastery ON leetcode_review(mastery);
CREATE INDEX idx_leetcode_review_next ON leetcode_review(next_review_at);
