package com.jobhelper.job.infrastructure;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_search_settings")
public class JobSearchSettingsEntity {

    @Id
    private Short id = 1;

    @Column(name = "search_terms", nullable = false)
    private String searchTerms = "";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(String searchTerms) {
        this.searchTerms = searchTerms;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
