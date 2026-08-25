package com.jobhelper.leetcode.application;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.jobhelper.leetcode.infrastructure.LeetCodeProblemEntity;
import com.jobhelper.leetcode.infrastructure.LeetCodeProblemRepository;
import com.jobhelper.leetcode.infrastructure.LeetCodeReviewEntity;
import com.jobhelper.leetcode.infrastructure.LeetCodeReviewRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class LeetCodeService implements ApplicationRunner {
    private static final Set<String> MASTERY = Set.of("confident", "partial", "weak");
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final LeetCodeProblemRepository problemRepository;
    private final LeetCodeReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    private final LeetCodeContentClient contentClient;

    public LeetCodeService(
            LeetCodeProblemRepository problemRepository,
            LeetCodeReviewRepository reviewRepository,
            ObjectMapper objectMapper,
            LeetCodeContentClient contentClient) {
        this.problemRepository = problemRepository;
        this.reviewRepository = reviewRepository;
        this.objectMapper = objectMapper;
        this.contentClient = contentClient;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedHot100IfEmpty();
    }

    @Transactional
    public void seedHot100IfEmpty() {
        if (problemRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("leetcode/hot100.json").getInputStream()) {
            JsonNode arr = objectMapper.readTree(in);
            int i = 0;
            for (JsonNode n : arr) {
                LeetCodeProblemEntity p = new LeetCodeProblemEntity();
                p.setProblemId(UUID.randomUUID());
                p.setProblemNumber(n.path("problemNumber").asInt());
                p.setTitle(n.path("title").asText());
                p.setSlug(n.path("slug").asText());
                p.setDifficulty(n.path("difficulty").asText("MEDIUM"));
                p.setTags(n.path("tags").asText(""));
                p.setUrl(n.path("url").asText());
                p.setSortOrder(n.path("sortOrder").asInt(++i));
                problemRepository.save(p);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed LeetCode Hot 100: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProblems(String q, String difficulty, String mastery, String reviewFilter) {
        String qn = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String diff = difficulty == null || difficulty.isBlank() || "all".equalsIgnoreCase(difficulty)
                ? null
                : difficulty.trim().toUpperCase(Locale.ROOT);
        if (diff != null && !DIFFICULTIES.contains(diff)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "difficulty must be EASY|MEDIUM|HARD|all");
        }
        String mast = mastery == null || mastery.isBlank() || "all".equalsIgnoreCase(mastery)
                ? null
                : mastery.trim().toLowerCase(Locale.ROOT);
        if (mast != null && !MASTERY.contains(mast)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "mastery must be confident|partial|weak|all");
        }
        String rf = reviewFilter == null ? "all" : reviewFilter.trim().toLowerCase(Locale.ROOT);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LeetCodeProblemEntity p : problemRepository.findAllByOrderBySortOrderAscProblemNumberAsc()) {
            if (diff != null && !diff.equals(p.getDifficulty())) {
                continue;
            }
            if (!qn.isEmpty()) {
                String hay = (p.getProblemNumber() + " " + p.getTitle() + " " + p.getTags()).toLowerCase(Locale.ROOT);
                if (!hay.contains(qn)) {
                    continue;
                }
            }
            var reviewOpt = reviewRepository.findByProblemId(p.getProblemId());
            if ("reviewed".equals(rf) && reviewOpt.isEmpty()) {
                continue;
            }
            if ("unreviewed".equals(rf) && reviewOpt.isPresent()) {
                continue;
            }
            if (mast != null) {
                if (reviewOpt.isEmpty() || !mast.equals(reviewOpt.get().getMastery())) {
                    continue;
                }
            }
            rows.add(toProblemSummary(p, reviewOpt.orElse(null)));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("problems", rows);
        out.put("total", rows.size());
        return out;
    }

    @Transactional
    public Map<String, Object> getProblem(UUID problemId) {
        LeetCodeProblemEntity p = ensureContent(loadProblem(problemId), false);
        return toProblemDetail(p);
    }

    @Transactional
    public Map<String, Object> refreshContent(UUID problemId) {
        LeetCodeProblemEntity p = ensureContent(loadProblem(problemId), true);
        return toProblemDetail(p);
    }

    private Map<String, Object> toProblemDetail(LeetCodeProblemEntity p) {
        Map<String, Object> out = toProblemSummary(p, reviewRepository.findByProblemId(p.getProblemId()).orElse(null));
        out.put("statementHtml", p.getStatementHtml());
        out.put("examples", p.getExamples());
        out.put("contentFetchedAt", p.getContentFetchedAt());
        out.put("hasContent", p.getStatementHtml() != null && !p.getStatementHtml().isBlank());
        reviewRepository.findByProblemId(p.getProblemId()).ifPresent(r -> out.put("review", toReviewDto(r)));
        return out;
    }

    private LeetCodeProblemEntity ensureContent(LeetCodeProblemEntity p, boolean force) {
        boolean missing = p.getStatementHtml() == null || p.getStatementHtml().isBlank();
        if (!force && !missing) {
            return p;
        }
        return contentClient.fetch(p.getSlug())
                .map(fetched -> {
                    p.setStatementHtml(fetched.statementHtml());
                    p.setExamples(fetched.examples());
                    p.setContentFetchedAt(Instant.now());
                    return problemRepository.save(p);
                })
                .orElse(p);
    }

    @Transactional
    public Map<String, Object> upsertReview(UUID problemId, Map<String, Object> body) {
        loadProblem(problemId);
        String confusion = str(body == null ? null : body.get("confusion"), null);
        if (confusion == null || confusion.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "confusion is required");
        }
        String mastery = str(body.get("mastery"), "partial");
        if (mastery == null || !MASTERY.contains(mastery.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "mastery must be confident|partial|weak");
        }
        mastery = mastery.toLowerCase(Locale.ROOT);

        Instant now = Instant.now();
        LeetCodeReviewEntity r = reviewRepository.findByProblemId(problemId).orElseGet(LeetCodeReviewEntity::new);
        if (r.getReviewId() == null) {
            r.setReviewId(UUID.randomUUID());
            r.setProblemId(problemId);
            r.setCreatedAt(now);
        }
        r.setConfusion(confusion.trim());
        r.setApproach(str(body.get("approach"), null));
        r.setKeyCode(str(body.get("keyCode"), null));
        r.setMastery(mastery);
        r.setNextReviewAt(parseNextReview(body.get("nextReviewAt")));
        r.setUpdatedAt(now);
        reviewRepository.save(r);

        Map<String, Object> out = toProblemSummary(loadProblem(problemId), r);
        out.put("review", toReviewDto(r));
        return out;
    }

    @Transactional
    public void deleteReview(UUID problemId) {
        loadProblem(problemId);
        reviewRepository.deleteByProblemId(problemId);
    }

    private Instant parseNextReview(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank() || "null".equals(String.valueOf(raw))) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        try {
            if (s.length() == 10) {
                return LocalDate.parse(s).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            return Instant.parse(s);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "nextReviewAt must be ISO date or instant");
        }
    }

    private LeetCodeProblemEntity loadProblem(UUID id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROBLEM_NOT_FOUND", "Problem not found"));
    }

    private Map<String, Object> toProblemSummary(LeetCodeProblemEntity p, LeetCodeReviewEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("problemId", p.getProblemId());
        m.put("problemNumber", p.getProblemNumber());
        m.put("title", p.getTitle());
        m.put("slug", p.getSlug());
        m.put("difficulty", p.getDifficulty());
        m.put("tags", p.getTags() == null || p.getTags().isBlank()
                ? List.of()
                : List.of(p.getTags().split("\\s*,\\s*")));
        m.put("url", p.getUrl());
        m.put("sortOrder", p.getSortOrder());
        m.put("hasReview", r != null);
        m.put("mastery", r == null ? null : r.getMastery());
        m.put("nextReviewAt", r == null ? null : r.getNextReviewAt());
        m.put("updatedAt", r == null ? null : r.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toReviewDto(LeetCodeReviewEntity r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reviewId", r.getReviewId());
        m.put("problemId", r.getProblemId());
        m.put("confusion", r.getConfusion());
        m.put("approach", r.getApproach());
        m.put("keyCode", r.getKeyCode());
        m.put("mastery", r.getMastery());
        m.put("nextReviewAt", r.getNextReviewAt());
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    private String str(Object v, String fallback) {
        if (v == null) {
            return fallback;
        }
        String s = String.valueOf(v);
        return s.isBlank() || "null".equals(s) ? fallback : s;
    }
}
