package com.jobhelper.behavioral.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobhelper.behavioral.application.BehavioralAnswerService;
import com.jobhelper.behavioral.application.BehavioralEvidenceService;
import com.jobhelper.behavioral.application.BehavioralFeedbackService;
import com.jobhelper.behavioral.application.BehavioralQuestionService;
import com.jobhelper.shared.web.ApiException;

@RestController
@RequestMapping("/api/v1/behavioral")
public class BehavioralController {
    private final BehavioralQuestionService questionService;
    private final BehavioralEvidenceService evidenceService;
    private final BehavioralAnswerService answerService;
    private final BehavioralFeedbackService feedbackService;

    public BehavioralController(
            BehavioralQuestionService questionService,
            BehavioralEvidenceService evidenceService,
            BehavioralAnswerService answerService,
            BehavioralFeedbackService feedbackService) {
        this.questionService = questionService;
        this.evidenceService = evidenceService;
        this.answerService = answerService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/questions")
    public Map<String, Object> listQuestions(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "visibility", required = false) String visibility) {
        return questionService.list(topic, sourceType, visibility);
    }

    @PostMapping("/questions")
    public ResponseEntity<Map<String, Object>> createQuestion(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(body));
    }

    @PatchMapping("/questions/{questionId}")
    public Map<String, Object> patchQuestion(
            @PathVariable UUID questionId,
            @RequestBody Map<String, Object> body) {
        return questionService.patch(questionId, body);
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        questionService.delete(questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/evidence")
    public Map<String, Object> listEvidence() {
        return evidenceService.list();
    }

    @PostMapping("/evidence")
    public ResponseEntity<Map<String, Object>> createEvidence(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(evidenceService.create(body));
    }

    @PutMapping("/evidence/{evidenceId}")
    public Map<String, Object> updateEvidence(
            @PathVariable UUID evidenceId,
            @RequestBody Map<String, Object> body) {
        return evidenceService.update(evidenceId, body);
    }

    @DeleteMapping("/evidence/{evidenceId}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable UUID evidenceId) {
        evidenceService.delete(evidenceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/answers")
    public Map<String, Object> listAnswers(@RequestParam(value = "questionId", required = false) UUID questionId) {
        return answerService.list(questionId);
    }

    @PostMapping("/answers")
    public ResponseEntity<Map<String, Object>> createAnswer(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(answerService.create(body));
    }

    @GetMapping("/answers/{answerId}")
    public Map<String, Object> getAnswer(@PathVariable UUID answerId) {
        return answerService.get(answerId);
    }

    @PostMapping("/answers/{answerId}/versions")
    public ResponseEntity<Map<String, Object>> createAnswerVersion(
            @PathVariable UUID answerId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(answerService.createVersion(answerId, body));
    }

    @PostMapping("/answers/{answerId}/versions/{versionId}/feedback")
    public ResponseEntity<Map<String, Object>> runLocalFeedback(
            @PathVariable UUID answerId,
            @PathVariable UUID versionId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.runLocalFeedback(answerId, versionId));
    }

    @PostMapping("/answers/{answerId}/versions/{versionId}/feedback/ai")
    public ResponseEntity<Map<String, Object>> aiFeedback(
            @PathVariable UUID answerId,
            @PathVariable UUID versionId) {
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "AI_NOT_ENABLED", "AI feedback is not enabled");
    }

    @GetMapping("/feedback/{feedbackId}")
    public Map<String, Object> getFeedback(@PathVariable UUID feedbackId) {
        return feedbackService.get(feedbackId);
    }

    @PostMapping("/feedback/{feedbackId}/items/{itemId}/decision")
    public Map<String, Object> decideFeedbackItem(
            @PathVariable UUID feedbackId,
            @PathVariable UUID itemId,
            @RequestBody Map<String, Object> body) {
        return feedbackService.decide(feedbackId, itemId, body);
    }
}
