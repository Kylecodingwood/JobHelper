package com.jobhelper.behavioral.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

final class BehavioralSupport {
    static final String SOURCE_CURATED = "CURATED";
    static final String SOURCE_USER_DEFINED = "USER_DEFINED";
    static final String VISIBILITY_ACTIVE = "ACTIVE";
    static final String VISIBILITY_HIDDEN = "HIDDEN";
    static final String STATUS_DRAFT = "DRAFT";
    static final String STATUS_COMPLETE = "COMPLETE";
    static final String ANSWER_STATUS_DRAFT = "DRAFT";
    static final String ANSWER_STATUS_FEEDBACK_AVAILABLE = "FEEDBACK_AVAILABLE";
    static final String ORIGIN_DETERMINISTIC = "DETERMINISTIC_RULE";
    static final String RULE_VERSION = "bhv-local-v1";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BehavioralSupport() {}

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String evidenceStatus(String situation, String task, String action, String result) {
        if (isBlank(situation) || isBlank(task) || isBlank(action) || isBlank(result)) {
            return STATUS_DRAFT;
        }
        return STATUS_COMPLETE;
    }

    static String encodeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(tags);
        } catch (Exception e) {
            return String.join(",", tags);
        }
    }

    @SuppressWarnings("unchecked")
    static List<String> decodeTags(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }
        try {
            if (raw.trim().startsWith("[")) {
                return MAPPER.readValue(raw, new TypeReference<List<String>>() {});
            }
        } catch (Exception ignored) {
            // fall through
        }
        String[] parts = raw.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                tags.add(part.trim());
            }
        }
        return tags;
    }

    static List<String> readStringList(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
        return List.of(String.valueOf(value));
    }

    static String readString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static List<UUID> readUuidList(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            out.add(UUID.fromString(String.valueOf(item)));
        }
        return Collections.unmodifiableList(out);
    }

    static int wordCount(String text) {
        if (isBlank(text)) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    static String normalizeDecision(String decision) {
        if (decision == null) {
            return null;
        }
        return decision.trim().toUpperCase(Locale.ROOT);
    }
}
