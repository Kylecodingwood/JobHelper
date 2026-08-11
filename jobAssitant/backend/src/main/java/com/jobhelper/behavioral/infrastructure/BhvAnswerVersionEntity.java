package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_answer_version")
public class BhvAnswerVersionEntity {
    @Id
    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "answer_id", nullable = false)
    private UUID answerId;

    @Column(name = "parent_version_id")
    private UUID parentVersionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "question_text_snapshot", nullable = false)
    private String questionTextSnapshot;

    @Column(name = "body_text", nullable = false)
    private String bodyText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getVersionId() { return versionId; }
    public void setVersionId(UUID versionId) { this.versionId = versionId; }
    public UUID getAnswerId() { return answerId; }
    public void setAnswerId(UUID answerId) { this.answerId = answerId; }
    public UUID getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(UUID parentVersionId) { this.parentVersionId = parentVersionId; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getQuestionTextSnapshot() { return questionTextSnapshot; }
    public void setQuestionTextSnapshot(String questionTextSnapshot) { this.questionTextSnapshot = questionTextSnapshot; }
    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
