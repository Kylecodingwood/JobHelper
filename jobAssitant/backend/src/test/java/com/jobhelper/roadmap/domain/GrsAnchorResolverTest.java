package com.jobhelper.roadmap.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class GrsAnchorResolverTest {

    @Test
    void nullGraduation_returnsNull() {
        assertNull(GrsAnchorResolver.resolve(null));
    }

    @Test
    void graduationAfterSep1_usesSameYear() {
        assertEquals(LocalDate.of(2026, 9, 1), GrsAnchorResolver.resolve(LocalDate.of(2026, 12, 15)));
    }

    @Test
    void graduationBeforeSep1_usesPreviousYear() {
        assertEquals(LocalDate.of(2025, 9, 1), GrsAnchorResolver.resolve(LocalDate.of(2026, 6, 15)));
    }
}
