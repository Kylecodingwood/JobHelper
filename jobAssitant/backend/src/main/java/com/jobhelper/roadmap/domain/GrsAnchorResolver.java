package com.jobhelper.roadmap.domain;

import java.time.LocalDate;

/**
 * Graduate Recruitment Season (GRS) anchor: Sep 1 of graduation year,
 * or previous Sep 1 when that date would fall after graduation.
 */
public final class GrsAnchorResolver {
    private GrsAnchorResolver() {}

    public static LocalDate resolve(LocalDate graduation) {
        if (graduation == null) {
            return null;
        }
        LocalDate candidate = LocalDate.of(graduation.getYear(), 9, 1);
        if (candidate.isAfter(graduation)) {
            candidate = LocalDate.of(graduation.getYear() - 1, 9, 1);
        }
        return candidate;
    }
}
