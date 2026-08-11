package com.jobhelper.action.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jobhelper.action.infrastructure.ActionItemEntity;

/**
 * Priority stack: deadline → blocker → pin → suggestion (action-api / specification §6).
 */
@Service
public class ActionPriorityService {

    public void recalculate(ActionItemEntity action) {
        List<Map<String, Object>> factors = buildFactors(action);
        String band = factors.isEmpty() ? "SUGGESTION" : String.valueOf(factors.get(0).get("factor"));
        action.setPriorityBand(band);
        action.setPrioritySortKey(sortKey(band, action));
        if (factors.isEmpty()) {
            action.setPrimaryReason("默认建议");
        } else {
            action.setPrimaryReason(String.valueOf(factors.get(0).get("explanation")));
        }
    }

    public Map<String, Object> explain(ActionItemEntity action) {
        List<Map<String, Object>> factors = buildFactors(action);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calculationId", UUID.randomUUID().toString());
        body.put("priorityBand", action.getPriorityBand());
        body.put("factors", factors);
        body.put("ruleVersion", "priority-v1");
        return body;
    }

    public List<Map<String, Object>> buildFactors(ActionItemEntity action) {
        List<Map<String, Object>> factors = new ArrayList<>();
        int order = 1;
        Instant now = Instant.now();
        if (action.getDeadline() != null) {
            boolean overdue = !action.getDeadline().isAfter(now);
            factors.add(factor("DEADLINE", order++, action.getDeadline().toString(),
                    overdue ? "已到期或今日截止" : "临近截止驱动优先"));
        }
        if ("BLOCKER".equals(action.getPriorityBand())
                || "RESOLVE_GATE".equals(action.getActionKind())
                || (action.getPrimaryReason() != null && action.getPrimaryReason().toLowerCase().contains("gate"))) {
            // Keep blocker visible even after band recalc when kind/reason indicates it
            if (factors.stream().noneMatch(f -> "BLOCKER".equals(f.get("factor")))) {
                factors.add(factor("BLOCKER", order++, action.getActionKind(),
                        action.getPrimaryReason() != null ? action.getPrimaryReason() : "阻塞项需先处理"));
            }
        }
        if (action.isPinned()) {
            factors.add(factor("USER_PIN", order++, "true", "用户置顶提升优先级"));
        }
        if (factors.isEmpty() || factors.stream().noneMatch(f -> "SUGGESTION".equals(f.get("factor")))) {
            factors.add(factor("SUGGESTION", order,
                    action.getActionKind(),
                    action.getPrimaryReason() != null ? action.getPrimaryReason() : "常规建议"));
        }
        // If originally a blocker but deadline/pin present, ensure BLOCKER still in stack when kind says so
        if (("RESOLVE_GATE".equals(action.getActionKind()) || "BLOCKER".equals(guessIntrinsicBand(action)))
                && factors.stream().noneMatch(f -> "BLOCKER".equals(f.get("factor")))) {
            factors.add(1, factor("BLOCKER", 2, action.getActionKind(), "阻塞项需先处理"));
            renumber(factors);
        }
        return factors;
    }

    /** Effective band after PIN: pin elevates to USER_PIN unless deadline/blocker already higher. */
    public String effectiveBand(ActionItemEntity action) {
        if (action.getDeadline() != null) {
            return "DEADLINE";
        }
        if ("RESOLVE_GATE".equals(action.getActionKind()) || "BLOCKER".equals(guessIntrinsicBand(action))) {
            return "BLOCKER";
        }
        if (action.isPinned()) {
            return "USER_PIN";
        }
        return "SUGGESTION";
    }

    public void applyEffectiveBand(ActionItemEntity action) {
        String band = effectiveBand(action);
        action.setPriorityBand(band);
        action.setPrioritySortKey(sortKey(band, action));
        List<Map<String, Object>> factors = buildFactors(action);
        if (!factors.isEmpty()) {
            action.setPrimaryReason(String.valueOf(factors.get(0).get("explanation")));
        }
    }

    private String guessIntrinsicBand(ActionItemEntity action) {
        String sort = action.getPrioritySortKey();
        if (sort != null && sort.contains("BLOCK")) {
            return "BLOCKER";
        }
        return action.getPriorityBand();
    }

    private String sortKey(String band, ActionItemEntity action) {
        String id = action.getActionId() != null ? action.getActionId().toString() : "x";
        return switch (band) {
            case "DEADLINE" -> "0|DEAD|" + (action.getDeadline() != null ? action.getDeadline() : "") + "|" + id;
            case "BLOCKER" -> "1|BLOCK|" + id;
            case "USER_PIN" -> "2|PIN|" + id;
            default -> "3|SUGGEST|" + id;
        };
    }

    private Map<String, Object> factor(String name, int order, String factValue, String explanation) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("factor", name);
        m.put("effectiveOrder", order);
        m.put("factValue", factValue);
        m.put("explanation", explanation);
        return m;
    }

    private void renumber(List<Map<String, Object>> factors) {
        for (int i = 0; i < factors.size(); i++) {
            factors.get(i).put("effectiveOrder", i + 1);
        }
    }
}
