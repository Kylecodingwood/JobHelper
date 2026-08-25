package com.jobhelper.leetcode.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leetcode_problem")
public class LeetCodeProblemEntity {
    @Id
    @Column(name = "problem_id")
    private UUID problemId;

    @Column(name = "problem_number", nullable = false, unique = true)
    private int problemNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tags;

    @Column(nullable = false)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "statement_html", columnDefinition = "TEXT")
    private String statementHtml;

    /** Raw example testcases from LeetCode (newline-separated groups). */
    @Column(columnDefinition = "TEXT")
    private String examples;

    @Column(name = "content_fetched_at")
    private java.time.Instant contentFetchedAt;

    public UUID getProblemId() { return problemId; }
    public void setProblemId(UUID problemId) { this.problemId = problemId; }
    public int getProblemNumber() { return problemNumber; }
    public void setProblemNumber(int problemNumber) { this.problemNumber = problemNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getStatementHtml() { return statementHtml; }
    public void setStatementHtml(String statementHtml) { this.statementHtml = statementHtml; }
    public String getExamples() { return examples; }
    public void setExamples(String examples) { this.examples = examples; }
    public java.time.Instant getContentFetchedAt() { return contentFetchedAt; }
    public void setContentFetchedAt(java.time.Instant contentFetchedAt) { this.contentFetchedAt = contentFetchedAt; }
}
