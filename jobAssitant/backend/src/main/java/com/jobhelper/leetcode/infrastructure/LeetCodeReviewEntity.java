package com.jobhelper.leetcode.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leetcode_review")
public class LeetCodeReviewEntity {
    @Id
    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "problem_id", nullable = false, unique = true)
    private UUID problemId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String confusion;

    @Column(columnDefinition = "TEXT")
    private String approach;

    @Column(name = "key_code", columnDefinition = "TEXT")
    private String keyCode;

    /** confident | partial | weak */
    @Column(nullable = false)
    private String mastery;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }
    public UUID getProblemId() { return problemId; }
    public void setProblemId(UUID problemId) { this.problemId = problemId; }
    public String getConfusion() { return confusion; }
    public void setConfusion(String confusion) { this.confusion = confusion; }
    public String getApproach() { return approach; }
    public void setApproach(String approach) { this.approach = approach; }
    public String getKeyCode() { return keyCode; }
    public void setKeyCode(String keyCode) { this.keyCode = keyCode; }
    public String getMastery() { return mastery; }
    public void setMastery(String mastery) { this.mastery = mastery; }
    public Instant getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Instant nextReviewAt) { this.nextReviewAt = nextReviewAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
