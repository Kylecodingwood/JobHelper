package com.jobhelper.job.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "canonical_job")
public class CanonicalJobEntity {
    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(nullable = false)
    private String title;

    private String company;
    private String location;

    @Column(name = "job_status", nullable = false)
    private String jobStatus;

    @Column(name = "validity_status", nullable = false)
    private String validityStatus;

    @Column(name = "gate_status", nullable = false)
    private String gateStatus;

    @Column(name = "rank_tier", nullable = false)
    private String rankTier;

    @Column(name = "hidden_by_default", nullable = false)
    private boolean hiddenByDefault;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "preferred_source_code")
    private String preferredSourceCode;

    @Column(name = "has_pending_duplicate", nullable = false)
    private boolean hasPendingDuplicate;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 10000)
    private String description;

    @Column(name = "canonical_apply_url")
    private String canonicalApplyUrl;

    @Column(name = "source_stable_id")
    private String sourceStableId;

    private String seniority;

    @Column(name = "gate_dimensions_json", length = 8000)
    private String gateDimensionsJson;

    @Column(name = "rank_factors_json", length = 8000)
    private String rankFactorsJson;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getJobStatus() { return jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String validityStatus) { this.validityStatus = validityStatus; }
    public String getGateStatus() { return gateStatus; }
    public void setGateStatus(String gateStatus) { this.gateStatus = gateStatus; }
    public String getRankTier() { return rankTier; }
    public void setRankTier(String rankTier) { this.rankTier = rankTier; }
    public boolean isHiddenByDefault() { return hiddenByDefault; }
    public void setHiddenByDefault(boolean hiddenByDefault) { this.hiddenByDefault = hiddenByDefault; }
    public LocalDate getExpectedStartDate() { return expectedStartDate; }
    public void setExpectedStartDate(LocalDate expectedStartDate) { this.expectedStartDate = expectedStartDate; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getPreferredSourceCode() { return preferredSourceCode; }
    public void setPreferredSourceCode(String preferredSourceCode) { this.preferredSourceCode = preferredSourceCode; }
    public boolean isHasPendingDuplicate() { return hasPendingDuplicate; }
    public void setHasPendingDuplicate(boolean hasPendingDuplicate) { this.hasPendingDuplicate = hasPendingDuplicate; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCanonicalApplyUrl() { return canonicalApplyUrl; }
    public void setCanonicalApplyUrl(String canonicalApplyUrl) { this.canonicalApplyUrl = canonicalApplyUrl; }
    public String getSourceStableId() { return sourceStableId; }
    public void setSourceStableId(String sourceStableId) { this.sourceStableId = sourceStableId; }
    public String getSeniority() { return seniority; }
    public void setSeniority(String seniority) { this.seniority = seniority; }
    public String getGateDimensionsJson() { return gateDimensionsJson; }
    public void setGateDimensionsJson(String gateDimensionsJson) { this.gateDimensionsJson = gateDimensionsJson; }
    public String getRankFactorsJson() { return rankFactorsJson; }
    public void setRankFactorsJson(String rankFactorsJson) { this.rankFactorsJson = rankFactorsJson; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
