package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_answer_feedback")
public class BhvAnswerFeedbackEntity {
    @Id
    @Column(name = "feedback_id")
    private UUID feedbackId;

    @Column(name = "answer_version_id", nullable = false)
    private UUID answerVersionId;

    @Column(name = "rule_version", nullable = false)
    private String ruleVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getFeedbackId() { return feedbackId; }
    public void setFeedbackId(UUID feedbackId) { this.feedbackId = feedbackId; }
    public UUID getAnswerVersionId() { return answerVersionId; }
    public void setAnswerVersionId(UUID answerVersionId) { this.answerVersionId = answerVersionId; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
