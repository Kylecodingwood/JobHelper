package com.jobhelper.job.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.roadmap.domain.GrsAnchorResolver;

class GateRankAndAnchorTest {

    private final GateRankService gateRankService = new GateRankService();

    @Test
    void workAuthMissingExpectedStartDateNeedsConfirmation() {
        CanonicalJobEntity job = new CanonicalJobEntity();
        job.setTitle("Graduate Software Engineer");
        job.setLocation("Dublin, Ireland");
        job.setExpectedStartDate(null);
        ProfileEntity profile = new ProfileEntity();
        gateRankService.evaluate(job, profile);
        assertEquals("NEEDS_CONFIRMATION", job.getGateStatus());
    }

    @Test
    void grsAnchorBeforeGraduation() {
        assertEquals(
                java.time.LocalDate.of(2025, 9, 1),
                GrsAnchorResolver.resolve(java.time.LocalDate.of(2026, 6, 15)));
        assertEquals(
                java.time.LocalDate.of(2026, 9, 1),
                GrsAnchorResolver.resolve(java.time.LocalDate.of(2026, 9, 1)));
    }
}
