package com.jobhelper.profile.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_authorization")
public class WorkAuthorizationEntity {
    @Id
    @Column(name = "work_authorization_id")
    private UUID workAuthorizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "permission_type", nullable = false)
    private String permissionType;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "weekly_hours_limit")
    private BigDecimal weeklyHoursLimit;

    @Column(name = "is_future", nullable = false)
    private boolean future;

    @Column(name = "country_code")
    private String countryCode;

    public UUID getWorkAuthorizationId() { return workAuthorizationId; }
    public void setWorkAuthorizationId(UUID id) { this.workAuthorizationId = id; }
    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public BigDecimal getWeeklyHoursLimit() { return weeklyHoursLimit; }
    public void setWeeklyHoursLimit(BigDecimal weeklyHoursLimit) { this.weeklyHoursLimit = weeklyHoursLimit; }
    public boolean isFuture() { return future; }
    public void setFuture(boolean future) { this.future = future; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
}
