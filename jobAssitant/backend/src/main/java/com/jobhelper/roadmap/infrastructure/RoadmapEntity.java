package com.jobhelper.roadmap.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "roadmap")
public class RoadmapEntity {
    @Id
    @Column(name = "roadmap_id")
    private UUID roadmapId;

    @Column(nullable = false)
    private String status;

    @Column(name = "profile_version", nullable = false)
    private Integer profileVersion;

    @Column(name = "rule_version", nullable = false)
    private String ruleVersion;

    @Column(name = "system_template_version_id")
    private UUID systemTemplateVersionId;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<RoadmapTaskEntity> tasks = new ArrayList<>();

    public UUID getRoadmapId() { return roadmapId; }
    public void setRoadmapId(UUID roadmapId) { this.roadmapId = roadmapId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Integer profileVersion) { this.profileVersion = profileVersion; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public UUID getSystemTemplateVersionId() { return systemTemplateVersionId; }
    public void setSystemTemplateVersionId(UUID systemTemplateVersionId) { this.systemTemplateVersionId = systemTemplateVersionId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<RoadmapTaskEntity> getTasks() { return tasks; }
}
