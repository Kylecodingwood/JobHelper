package com.jobhelper.behavioral.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.behavioral.infrastructure.BhvQuestionEntity;
import com.jobhelper.behavioral.infrastructure.BhvQuestionRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class BehavioralQuestionService {
    private final BhvQuestionRepository repository;

    public BehavioralQuestionService(BhvQuestionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String topic, String sourceType, String visibility) {
        String effectiveVisibility = visibility == null || visibility.isBlank()
                ? BehavioralSupport.VISIBILITY_ACTIVE
                : visibility;
        List<Map<String, Object>> items = repository.findAll().stream()
                .filter(q -> topic == null || topic.isBlank() || topic.equals(q.getCompetencyTopic()))
                .filter(q -> sourceType == null || sourceType.isBlank() || sourceType.equals(q.getSourceType()))
                .filter(q -> effectiveVisibility.equals(q.getVisibility()))
                .map(this::toDto)
                .toList();
        return Map.of("items", items);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String text = BehavioralSupport.readString(body, "text");
        String competencyTopic = BehavioralSupport.readString(body, "competencyTopic");
        if (BehavioralSupport.isBlank(text) || BehavioralSupport.isBlank(competencyTopic)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "text and competencyTopic required");
        }

        Instant now = Instant.now();
        BhvQuestionEntity entity = new BhvQuestionEntity();
        entity.setQuestionId(UUID.randomUUID());
        entity.setText(text.trim());
        entity.setCompetencyTopic(competencyTopic.trim());
        entity.setSourceType(BehavioralSupport.SOURCE_USER_DEFINED);
        entity.setVisibility(BehavioralSupport.VISIBILITY_ACTIVE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        repository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public Map<String, Object> patch(UUID questionId, Map<String, Object> body) {
        BhvQuestionEntity entity = load(questionId);
        boolean curated = BehavioralSupport.SOURCE_CURATED.equals(entity.getSourceType());

        if (curated) {
            if (body.containsKey("text") || body.containsKey("competencyTopic")) {
                throw new ApiException(HttpStatus.CONFLICT, "CURATED_IMMUTABLE", "Curated question text cannot be changed");
            }
            String visibility = BehavioralSupport.readString(body, "visibility");
            if (visibility != null) {
                validateVisibility(visibility);
                entity.setVisibility(visibility.trim().toUpperCase());
            }
        } else {
            String text = BehavioralSupport.readString(body, "text");
            if (text != null && !text.isBlank()) {
                entity.setText(text.trim());
            }
            String competencyTopic = BehavioralSupport.readString(body, "competencyTopic");
            if (competencyTopic != null && !competencyTopic.isBlank()) {
                entity.setCompetencyTopic(competencyTopic.trim());
            }
            String visibility = BehavioralSupport.readString(body, "visibility");
            if (visibility != null) {
                validateVisibility(visibility);
                entity.setVisibility(visibility.trim().toUpperCase());
            }
        }

        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public void delete(UUID questionId) {
        BhvQuestionEntity entity = load(questionId);
        if (BehavioralSupport.SOURCE_CURATED.equals(entity.getSourceType())) {
            throw new ApiException(HttpStatus.CONFLICT, "CURATED_IMMUTABLE", "Curated questions cannot be deleted");
        }
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public BhvQuestionEntity load(UUID questionId) {
        return repository.findById(questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Question not found"));
    }

    private void validateVisibility(String visibility) {
        String normalized = visibility.trim().toUpperCase();
        if (!BehavioralSupport.VISIBILITY_ACTIVE.equals(normalized)
                && !BehavioralSupport.VISIBILITY_HIDDEN.equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "visibility must be ACTIVE or HIDDEN");
        }
    }

    private Map<String, Object> toDto(BhvQuestionEntity q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("questionId", q.getQuestionId());
        m.put("text", q.getText());
        m.put("competencyTopic", q.getCompetencyTopic());
        m.put("sourceType", q.getSourceType());
        m.put("curationVersion", q.getCurationVersion());
        m.put("visibility", q.getVisibility());
        m.put("createdAt", q.getCreatedAt());
        m.put("updatedAt", q.getUpdatedAt());
        return m;
    }
}
