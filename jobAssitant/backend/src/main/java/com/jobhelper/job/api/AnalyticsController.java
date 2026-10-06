package com.jobhelper.job.api;

import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final CanonicalJobRepository jobRepository;

    public AnalyticsController(CanonicalJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/jobs")
    public Map<String, Object> jobFunnel() {
        long total = jobRepository.count();
        long gatePassed  = jobRepository.countByGateStatus("PASSED");
        long gateFailed  = jobRepository.countByGateStatus("FAILED");
        long shortlisted = jobRepository.countByJobStatus("SHORTLISTED");
        long applied     = jobRepository.countByJobStatus("APPLIED");
        long ignored     = jobRepository.countByJobStatus("IGNORED");
        long archived    = jobRepository.countByJobStatus("ARCHIVED");
        long rankHigh    = jobRepository.countByRankTier("HIGH");
        long rankMedium  = jobRepository.countByRankTier("MEDIUM");
        long rankLow     = jobRepository.countByRankTier("LOW");

        String passRate = total > 0
                ? Math.round(gatePassed * 100.0 / total) + "%"
                : "—";
        String applyRate = shortlisted > 0
                ? Math.round(applied * 100.0 / shortlisted) + "%"
                : "—";

        return Map.of(
                "total", total,
                "gate", Map.of(
                        "passed", gatePassed,
                        "failed", gateFailed,
                        "passRate", passRate),
                "status", Map.of(
                        "shortlisted", shortlisted,
                        "applied", applied,
                        "ignored", ignored,
                        "archived", archived,
                        "applyRate", applyRate),
                "rank", Map.of(
                        "high", rankHigh,
                        "medium", rankMedium,
                        "low", rankLow)
        );
    }
}
