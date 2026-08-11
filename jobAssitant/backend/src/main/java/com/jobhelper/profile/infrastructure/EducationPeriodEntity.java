package com.jobhelper.profile.infrastructure;

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
@Table(name = "education_period")
public class EducationPeriodEntity {
    @Id
    @Column(name = "education_period_id")
    private UUID educationPeriodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "institution_name", nullable = false)
    private String institutionName;

    @Column(name = "programme_name")
    private String programmeName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_graduation_date")
    private LocalDate expectedGraduationDate;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    public UUID getEducationPeriodId() { return educationPeriodId; }
    public void setEducationPeriodId(UUID educationPeriodId) { this.educationPeriodId = educationPeriodId; }
    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getExpectedGraduationDate() { return expectedGraduationDate; }
    public void setExpectedGraduationDate(LocalDate expectedGraduationDate) { this.expectedGraduationDate = expectedGraduationDate; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}
