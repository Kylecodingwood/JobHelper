package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap_template_version")
public class RoadmapTemplateVersionEntity {
    @Id
    @Column(name = "template_version_id")
    private UUID templateVersionId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String status;

    @Column(name = "markdown_content", columnDefinition = "TEXT")
    private String markdownContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phases_json", columnDefinition = "jsonb")
    private String phasesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(UUID templateVersionId) { this.templateVersionId = templateVersionId; }
    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
    public String getPhasesJson() { return phasesJson; }
    public void setPhasesJson(String phasesJson) { this.phasesJson = phasesJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
