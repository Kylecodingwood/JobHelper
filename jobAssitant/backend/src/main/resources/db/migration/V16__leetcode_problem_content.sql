-- Cache LeetCode problem statement + examples in DB.

ALTER TABLE leetcode_problem
    ADD COLUMN statement_html TEXT,
    ADD COLUMN examples TEXT,
    ADD COLUMN content_fetched_at TIMESTAMP WITH TIME ZONE;
