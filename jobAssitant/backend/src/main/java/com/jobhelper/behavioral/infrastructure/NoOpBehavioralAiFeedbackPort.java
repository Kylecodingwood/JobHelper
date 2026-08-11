package com.jobhelper.behavioral.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jobhelper.behavioral.domain.BehavioralAiFeedbackPort;

@Component
public class NoOpBehavioralAiFeedbackPort implements BehavioralAiFeedbackPort {
    @Override
    public Optional<AiFeedbackBundle> enhance(UUID answerVersionId, String consentToken) {
        return Optional.empty();
    }
}
