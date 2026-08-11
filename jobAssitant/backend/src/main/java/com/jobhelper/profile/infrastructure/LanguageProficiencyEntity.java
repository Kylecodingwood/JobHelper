package com.jobhelper.profile.infrastructure;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "language_proficiency")
public class LanguageProficiencyEntity {
    @Id
    @Column(name = "language_proficiency_id")
    private UUID languageProficiencyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(nullable = false)
    private String proficiency;

    @Column(name = "evidence_ref")
    private String evidenceRef;

    private String notes;

    public UUID getLanguageProficiencyId() { return languageProficiencyId; }
    public void setLanguageProficiencyId(UUID id) { this.languageProficiencyId = id; }
    public ProfileEntity getProfile() { return profile; }
    public void setProfile(ProfileEntity profile) { this.profile = profile; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getProficiency() { return proficiency; }
    public void setProficiency(String proficiency) { this.proficiency = proficiency; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
