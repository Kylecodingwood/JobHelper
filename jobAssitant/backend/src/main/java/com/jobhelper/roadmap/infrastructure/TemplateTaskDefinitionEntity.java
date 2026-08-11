package com.jobhelper.roadmap.infrastructure;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "template_task_definition")
public class TemplateTaskDefinitionEntity {
    @Id
    @Column(name = "template_task_definition_id")
    private UUID templateTaskDefinitionId;

    @Column(name = "template_version_id", nullable = false)
    private UUID templateVersionId;

    @Column(name = "template_task_key", nullable = false)
    private String templateTaskKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String phase;

    @Column(name = "anchor_type")
    private String anchorType;

    @Column(name = "relative_offset_days", nullable = false)
    private int relativeOffsetDays;

    @Column(name = "completion_criteria")
    private String completionCriteria;

    @Column(nullable = false)
    private String priority;

    /** JSON array of template task keys, e.g. ["profile.complete"]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "depends_on_keys", columnDefinition = "jsonb")
    private String dependsOnKeys;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public UUID getTemplateTaskDefinitionId() { return templateTaskDefinitionId; }
    public void setTemplateTaskDefinitionId(UUID templateTaskDefinitionId) { this.templateTaskDefinitionId = templateTaskDefinitionId; }
    public UUID getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(UUID templateVersionId) { this.templateVersionId = templateVersionId; }
    public String getTemplateTaskKey() { return templateTaskKey; }
    public void setTemplateTaskKey(String templateTaskKey) { this.templateTaskKey = templateTaskKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getAnchorType() { return anchorType; }
    public void setAnchorType(String anchorType) { this.anchorType = anchorType; }
    public int getRelativeOffsetDays() { return relativeOffsetDays; }
    public void setRelativeOffsetDays(int relativeOffsetDays) { this.relativeOffsetDays = relativeOffsetDays; }
    public String getCompletionCriteria() { return completionCriteria; }
    public void setCompletionCriteria(String completionCriteria) { this.completionCriteria = completionCriteria; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getDependsOnKeys() { return dependsOnKeys; }
    public void setDependsOnKeys(String dependsOnKeys) { this.dependsOnKeys = dependsOnKeys; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
