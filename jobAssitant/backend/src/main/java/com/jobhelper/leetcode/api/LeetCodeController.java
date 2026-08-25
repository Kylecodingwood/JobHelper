package com.jobhelper.leetcode.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.leetcode.application.LeetCodeService;

@RestController
@RequestMapping("/api/v1/leetcode")
public class LeetCodeController {
    private final LeetCodeService leetCodeService;

    public LeetCodeController(LeetCodeService leetCodeService) {
        this.leetCodeService = leetCodeService;
    }

    @GetMapping("/problems")
    public Map<String, Object> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String mastery,
            @RequestParam(required = false, defaultValue = "all") String review) {
        return leetCodeService.listProblems(q, difficulty, mastery, review);
    }

    @GetMapping("/problems/{problemId}")
    public Map<String, Object> get(@PathVariable UUID problemId) {
        return leetCodeService.getProblem(problemId);
    }

    @PostMapping("/problems/{problemId}/content/refresh")
    public Map<String, Object> refreshContent(@PathVariable UUID problemId) {
        return leetCodeService.refreshContent(problemId);
    }

    @PutMapping("/problems/{problemId}/review")
    public Map<String, Object> upsertReview(@PathVariable UUID problemId, @RequestBody Map<String, Object> body) {
        return leetCodeService.upsertReview(problemId, body);
    }

    @DeleteMapping("/problems/{problemId}/review")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID problemId) {
        leetCodeService.deleteReview(problemId);
        return ResponseEntity.noContent().build();
    }
}
