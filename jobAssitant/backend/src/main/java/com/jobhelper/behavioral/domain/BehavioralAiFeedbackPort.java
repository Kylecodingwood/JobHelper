package com.jobhelper.behavioral.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BehavioralAiFeedbackPort {
    Optional<AiFeedbackBundle> enhance(UUID answerVersionId, String consentToken);

    record AiFeedbackBundle(
            List<AiFeedbackItem> items
    ) {}

    record AiFeedbackItem(
            String dimension,
            String targetQuote,
            String issue,
            String rationale,
            String suggestion
    ) {}
}
