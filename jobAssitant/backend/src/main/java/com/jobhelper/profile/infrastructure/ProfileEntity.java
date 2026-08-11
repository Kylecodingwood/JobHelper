package com.jobhelper.profile.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "profile")
public class ProfileEntity {
    @Id
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "profile_version", nullable = false)
    private Integer profileVersion;

    @Column(nullable = false)
    private String lifecycle;

    private String nationality;

    @Column(name = "identity_status")
    private String identityStatus;

    @Column(name = "identity_valid_until")
    private LocalDate identityValidUntil;

    @Column(name = "target_country")
    private String targetCountry;

    @Column(name = "job_seeking_goal", columnDefinition = "TEXT")
    private String jobSeekingGoal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EducationPeriodEntity> educationPeriods = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkExperienceEntity> workExperiences = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileSkillEntity> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LanguageProficiencyEntity> languageProficiencies = new ArrayList<>();

    /** Legacy collections kept for Hibernate mapping of old tables until dropped. */
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkAuthorizationEntity> workAuthorizations = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TargetRoleEntity> targetRoles = new ArrayList<>();

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }
    public Integer getProfileVersion() { return profileVersion; }
    public void setProfileVersion(Integer profileVersion) { this.profileVersion = profileVersion; }
    public String getLifecycle() { return lifecycle; }
    public void setLifecycle(String lifecycle) { this.lifecycle = lifecycle; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getIdentityStatus() { return identityStatus; }
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }
    public LocalDate getIdentityValidUntil() { return identityValidUntil; }
    public void setIdentityValidUntil(LocalDate identityValidUntil) { this.identityValidUntil = identityValidUntil; }
    public String getTargetCountry() { return targetCountry; }
    public void setTargetCountry(String targetCountry) { this.targetCountry = targetCountry; }
    public String getJobSeekingGoal() { return jobSeekingGoal; }
    public void setJobSeekingGoal(String jobSeekingGoal) { this.jobSeekingGoal = jobSeekingGoal; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<EducationPeriodEntity> getEducationPeriods() { return educationPeriods; }
    public List<WorkExperienceEntity> getWorkExperiences() { return workExperiences; }
    public List<ProfileSkillEntity> getSkills() { return skills; }
    public List<LanguageProficiencyEntity> getLanguageProficiencies() { return languageProficiencies; }
    public List<WorkAuthorizationEntity> getWorkAuthorizations() { return workAuthorizations; }
    public List<TargetRoleEntity> getTargetRoles() { return targetRoles; }
}
