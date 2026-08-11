package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_source")
public class JobSourceEntity {
    @Id
    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "adapter_type", nullable = false)
    private String adapterType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private String role;

    @Column(name = "search_terms_override")
    private String searchTermsOverride;

    @Column(name = "risk_note")
    private String riskNote;

    @Column(name = "config_version", nullable = false)
    private int configVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getSourceId() { return sourceId; }
    public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getAdapterType() { return adapterType; }
    public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getSearchTermsOverride() { return searchTermsOverride; }
    public void setSearchTermsOverride(String searchTermsOverride) { this.searchTermsOverride = searchTermsOverride; }
    public String getRiskNote() { return riskNote; }
    public void setRiskNote(String riskNote) { this.riskNote = riskNote; }
    public int getConfigVersion() { return configVersion; }
    public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
