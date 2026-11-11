package com.jobhelper.application.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.application.infrastructure.EmailApplicationEntity;
import com.jobhelper.application.infrastructure.EmailApplicationRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class EmailApplicationService {

    static final Set<String> CATEGORIES = Set.of(
            "grad_swe", "grad_tech", "grad_other",
            "intern_summer", "intern_long", "intern_unknown",
            "other", "unknown");
    static final Set<String> STATUSES = Set.of(
            "applied", "rejected", "oa", "hackerrank", "video", "offer");
    static final Set<String> PIPELINE_STATUSES = Set.of("oa", "hackerrank", "video", "offer");

    private final EmailApplicationRepository repository;

    public EmailApplicationService(EmailApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByCategoryAscCompanyNameAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<EmailApplicationEntity> all = repository.findAll();
        int total = all.size();
        long rejected = all.stream().filter(a -> "rejected".equals(a.getStatus())).count();
        long inPipeline = all.stream().filter(a -> PIPELINE_STATUSES.contains(a.getStatus())).count();
        long active = total - rejected;

        Map<String, Map<String, Object>> byCategory = new LinkedHashMap<>();
        for (String cat : List.of("grad_swe","grad_tech","grad_other","intern_summer","intern_long","intern_unknown","other","unknown")) {
            List<EmailApplicationEntity> catApps = all.stream()
                    .filter(a -> cat.equals(a.getCategory())).collect(Collectors.toList());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("total", catApps.size());
            m.put("rejected", catApps.stream().filter(a -> "rejected".equals(a.getStatus())).count());
            m.put("inPipeline", catApps.stream().filter(a -> PIPELINE_STATUSES.contains(a.getStatus())).count());
            byCategory.put(cat, m);
        }

        List<Map<String, Object>> pipeline = all.stream()
                .filter(a -> PIPELINE_STATUSES.contains(a.getStatus()))
                .map(a -> Map.<String, Object>of(
                        "appId", a.getAppId(),
                        "companyName", a.getCompanyName(),
                        "roleName", a.getRoleName(),
                        "status", a.getStatus()))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("active", active);
        result.put("rejected", rejected);
        result.put("inPipeline", inPipeline);
        result.put("byCategory", byCategory);
        result.put("pipeline", pipeline);
        return result;
    }

    @Transactional
    public Map<String, Object> bulkImport(List<Map<String, Object>> items) {
        Instant now = Instant.now();
        int imported = 0;
        for (Map<String, Object> item : items) {
            String company = str(item.get("companyName"));
            String role = str(item.get("roleName"));
            if (company == null || role == null) continue;
            EmailApplicationEntity e = repository.findByCompanyNameAndRoleName(company, role)
                    .orElse(null);
            if (e == null) {
                e = new EmailApplicationEntity();
                e.setAppId(UUID.randomUUID());
                e.setCreatedAt(now);
            }
            e.setCompanyName(company);
            e.setRoleName(role);
            e.setCategory(str(item.get("category"), "other"));
            e.setIsSwe(Boolean.TRUE.equals(item.get("isSwe")));
            e.setStatus(str(item.get("status"), "applied"));
            e.setNotes(str(item.get("notes"), null));
            e.setUpdatedAt(now);
            repository.save(e);
            imported++;
        }
        return Map.of("imported", imported);
    }

    @Transactional
    public Map<String, Object> patch(UUID appId, Map<String, Object> body) {
        EmailApplicationEntity e = repository.findById(appId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Application not found"));
        if (body.containsKey("status")) {
            String s = str(body.get("status"), e.getStatus());
            if (!STATUSES.contains(s)) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid status");
            e.setStatus(s);
        }
        if (body.containsKey("notes")) e.setNotes(str(body.get("notes"), null));
        e.setUpdatedAt(Instant.now());
        return toDto(repository.save(e));
    }

    private Map<String, Object> toDto(EmailApplicationEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appId", e.getAppId());
        m.put("companyName", e.getCompanyName());
        m.put("roleName", e.getRoleName());
        m.put("category", e.getCategory());
        m.put("isSwe", e.isIsSwe());
        m.put("status", e.getStatus());
        m.put("notes", e.getNotes());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }

    private String str(Object v) { return str(v, null); }
    private String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v);
        return s.isBlank() || "null".equals(s) ? fallback : s.trim();
    }
}
