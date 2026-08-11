package com.jobhelper.behavioral.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bhv_question")
public class BhvQuestionEntity {
    @Id
    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "competency_topic", nullable = false)
    private String competencyTopic;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "curation_version")
    private String curationVersion;

    @Column(name = "visibility", nullable = false)
    private String visibility;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCompetencyTopic() { return competencyTopic; }
    public void setCompetencyTopic(String competencyTopic) { this.competencyTopic = competencyTopic; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getCurationVersion() { return curationVersion; }
    public void setCurationVersion(String curationVersion) { this.curationVersion = curationVersion; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
