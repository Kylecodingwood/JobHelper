package com.jobhelper.profile.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profile_backup_metadata")
public class ProfileBackupMetadataEntity {
    @Id
    @Column(name = "backup_id")
    private UUID backupId;

    @Column(name = "backup_type", nullable = false)
    private String backupType;

    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;

    @Column
    private String checksum;

    @Column(name = "domains_included", length = 1024)
    private String domainsIncluded;

    @Column(name = "retention_rank")
    private Integer retentionRank;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getBackupId() { return backupId; }
    public void setBackupId(UUID backupId) { this.backupId = backupId; }
    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public String getDomainsIncluded() { return domainsIncluded; }
    public void setDomainsIncluded(String domainsIncluded) { this.domainsIncluded = domainsIncluded; }
    public Integer getRetentionRank() { return retentionRank; }
    public void setRetentionRank(Integer retentionRank) { this.retentionRank = retentionRank; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
