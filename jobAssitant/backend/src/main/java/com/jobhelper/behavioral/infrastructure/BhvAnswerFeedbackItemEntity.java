package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_answer_feedback_item")
public class BhvAnswerFeedbackItemEntity {
    @Id
    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "feedback_id", nullable = false)
    private UUID feedbackId;

    @Column(name = "dimension", nullable = false)
    private String dimension;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(name = "target_quote")
    private String targetQuote;

    @Column(name = "issue")
    private String issue;

    @Column(name = "rationale")
    private String rationale;

    @Column(name = "suggestion")
    private String suggestion;

    @Column(name = "origin", nullable = false)
    private String origin;

    @Column(name = "decision")
    private String decision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public UUID getFeedbackId() { return feedbackId; }
    public void setFeedbackId(UUID feedbackId) { this.feedbackId = feedbackId; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTargetQuote() { return targetQuote; }
    public void setTargetQuote(String targetQuote) { this.targetQuote = targetQuote; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
