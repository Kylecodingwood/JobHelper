package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_answer")
public class BhvAnswerEntity {
    @Id
    @Column(name = "answer_id")
    private UUID answerId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getAnswerId() { return answerId; }
    public void setAnswerId(UUID answerId) { this.answerId = answerId; }
    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(UUID currentVersionId) { this.currentVersionId = currentVersionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
