package com.jobhelper.job.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class DuplicateUrlNormalizeTest {

    private final DuplicateDetectionService service = new DuplicateDetectionService(
            null, null, null, null, new ObjectMapper());

    @Test
    void stripsTrailingSlashAndTrackingParams() {
        assertEquals(
                "https://example.com/jobs/1",
                service.normalizeUrl("HTTPS://Example.com/jobs/1/?utm_source=x&fbclid=1"));
    }

    @Test
    void blankBecomesNull() {
        assertNull(service.normalizeUrl("  "));
        assertNull(service.normalizeUrl(null));
    }
}
