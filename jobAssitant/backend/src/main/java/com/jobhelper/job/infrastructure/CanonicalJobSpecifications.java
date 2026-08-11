package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

/**
 * Query filters for {@code GET /api/v1/jobs} (jobs-inbox api.md §GET /jobs, job-api/delta.md §1).
 */
public final class CanonicalJobSpecifications {

    private CanonicalJobSpecifications() {
    }

    public static Specification<CanonicalJobEntity> filter(
            List<String> status,
            List<String> rankTier,
            List<String> gateStatus,
            List<String> validityStatus,
            boolean includeHidden,
            boolean includeArchived,
            Boolean hasPendingDuplicate,
            String sourceCode,
            String q,
            String fit) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            List<String> normalizedStatus = normalize(status);
            if (!normalizedStatus.isEmpty()) {
                // Explicit status filter wins over the includeArchived default (e.g. status=ARCHIVED).
                predicate = cb.and(predicate, root.get("jobStatus").in(normalizedStatus));
            } else if (!includeArchived) {
                predicate = cb.and(predicate, cb.notEqual(root.get("jobStatus"), "ARCHIVED"));
            }

            List<String> normalizedRankTier = normalize(rankTier);
            if (!normalizedRankTier.isEmpty()) {
                predicate = cb.and(predicate, root.get("rankTier").in(normalizedRankTier));
            }

            List<String> normalizedGateStatus = normalize(gateStatus);
            if (!normalizedGateStatus.isEmpty()) {
                predicate = cb.and(predicate, root.get("gateStatus").in(normalizedGateStatus));
            }

            List<String> normalizedValidityStatus = normalize(validityStatus);
            if (!normalizedValidityStatus.isEmpty()) {
                predicate = cb.and(predicate, root.get("validityStatus").in(normalizedValidityStatus));
            }

            if (!includeHidden) {
                predicate = cb.and(predicate, cb.isFalse(root.get("hiddenByDefault")));
            }

            if (hasPendingDuplicate != null) {
                predicate = cb.and(predicate, cb.equal(root.get("hasPendingDuplicate"), hasPendingDuplicate));
            }

            if (sourceCode != null && !sourceCode.isBlank()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                var refRoot = sub.from(JobSourceRefEntity.class);
                sub.select(refRoot.get("jobId"))
                        .where(cb.equal(cb.lower(refRoot.get("sourceCode")), sourceCode.trim().toLowerCase(Locale.ROOT)));
                predicate = cb.and(predicate, root.get("jobId").in(sub));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), like);
                Predicate companyLike = cb.like(cb.lower(root.get("company")), like);
                predicate = cb.and(predicate, cb.or(titleLike, companyLike));
            }

            String fitNorm = fit == null ? "" : fit.trim().toLowerCase(Locale.ROOT);
            if ("junior".equals(fitNorm)) {
                // Prefer structured signals; avoid substring traps like "International" ~ "%intern%".
                Predicate sen = cb.lower(root.get("seniority")).in(List.of("junior", "intern"));
                Predicate dims = cb.like(root.get("gateDimensionsJson"), "%JUNIOR_FIT%");
                var title = cb.lower(root.get("title"));
                Predicate titleJunior = cb.or(
                        cb.like(title, "junior%"),
                        cb.like(title, "% junior%"),
                        cb.like(title, "graduate%"),
                        cb.like(title, "% graduate%"),
                        cb.like(title, "%internship%"),
                        cb.like(title, "intern %"),
                        cb.like(title, "% intern %"),
                        cb.like(title, "% intern"),
                        cb.like(title, "entry-level%"),
                        cb.like(title, "%entry-level%"),
                        cb.like(title, "%entry level%"),
                        cb.like(title, "%early career%"));
                predicate = cb.and(predicate, cb.or(sen, dims, titleJunior));
            } else if ("mid".equals(fitNorm)) {
                // Mid / unlabeled SWE: Cap MID_OK, and not junior-tagged.
                // Note: seniority NULL must not be excluded by NOT (seniority IN …).
                Predicate isJuniorSen = cb.and(
                        cb.isNotNull(root.get("seniority")),
                        cb.lower(root.get("seniority")).in(List.of("junior", "intern")));
                Predicate isJuniorDim = cb.like(root.get("gateDimensionsJson"), "%JUNIOR_FIT%");
                Predicate midOk = cb.like(root.get("gateDimensionsJson"), "%MID_OK%");
                predicate = cb.and(predicate, midOk, cb.not(cb.or(isJuniorSen, isJuniorDim)));
            }

            return predicate;
        };
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toUpperCase(Locale.ROOT))
                .toList();
    }
}
