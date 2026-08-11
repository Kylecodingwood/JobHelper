package com.jobhelper.action.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jobhelper.action.infrastructure.ActionItemEntity;

class ActionPriorityServiceTest {

    private final ActionPriorityService service = new ActionPriorityService();

    @Test
    void pinElevatesSuggestionToUserPin() {
        ActionItemEntity a = base();
        a.setPriorityBand("SUGGESTION");
        a.setPinned(true);
        service.applyEffectiveBand(a);
        assertEquals("USER_PIN", a.getPriorityBand());
        assertTrue(a.getPrioritySortKey().startsWith("2|PIN|"));
    }

    @Test
    void deadlineBeatsPin() {
        ActionItemEntity a = base();
        a.setDeadline(Instant.now().plusSeconds(3600));
        a.setPinned(true);
        service.applyEffectiveBand(a);
        assertEquals("DEADLINE", a.getPriorityBand());
    }

    @Test
    void evidenceOrderedDeadlineBlockerPinSuggestion() {
        ActionItemEntity a = base();
        a.setDeadline(Instant.now().plusSeconds(60));
        a.setActionKind("RESOLVE_GATE");
        a.setPinned(true);
        a.setPrimaryReason("Gate 需要确认");
        List<Map<String, Object>> factors = service.buildFactors(a);
        assertEquals("DEADLINE", factors.get(0).get("factor"));
        assertTrue(factors.stream().anyMatch(f -> "BLOCKER".equals(f.get("factor"))));
        assertTrue(factors.stream().anyMatch(f -> "USER_PIN".equals(f.get("factor"))));
    }

    private ActionItemEntity base() {
        ActionItemEntity a = new ActionItemEntity();
        a.setActionId(UUID.randomUUID());
        a.setSourceDomain("JOB");
        a.setActionKind("REVIEW_NEW_JOB");
        a.setTitle("t");
        a.setStatus("OPEN");
        a.setActive(true);
        a.setPriorityBand("SUGGESTION");
        a.setPrioritySortKey("3|SUGGEST|x");
        a.setVersion(1);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }
}
