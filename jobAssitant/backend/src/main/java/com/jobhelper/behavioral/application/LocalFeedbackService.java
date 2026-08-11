package com.jobhelper.behavioral.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.jobhelper.behavioral.infrastructure.BhvStarEvidenceRevisionEntity;

@Service
public class LocalFeedbackService {
    private static final Pattern FIRST_PERSON_ACTION = Pattern.compile(
            "\\bI\\s+(?:led|built|implemented|created|decided|coordinated|drove|worked|developed|designed|"
                    + "organized|managed|resolved|negotiated|presented|delivered|fixed|improved|reduced|"
                    + "increased|collaborated|mentored|onboarded|delegated|prioritized|escalated|proposed|"
                    + "architected|automated|migrated|refactored|tested|deployed|wrote|spoke|facilitated)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_BACKGROUND = Pattern.compile(
            "\\b(?:when|during|while|after|before|last\\s+year|last\\s+month|in\\s+20\\d{2}|"
                    + "at\\s+the\\s+time|previously|early\\s+in|quarter|semester|sprint)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_WORDS = Pattern.compile(
            "\\b(?:result|impact|outcome|achieved|delivered|saved|increased|decreased|reduced|improved|"
                    + "grew|boosted|lowered|accelerated|enabled|prevented|resolved|met|exceeded)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VAGUE_WORDS = Pattern.compile(
            "\\b(?:significantly|greatly|improved|better|successful|helped|various|many|several|some|"
                    + "good|great|nice|strong|effective|efficient)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern METRIC = Pattern.compile(
            "\\d|%|\\bx\\b|percent|percentage|hours|days|weeks|months|users|customers|requests|ms|seconds",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IMPROVEMENT_CLAIM = Pattern.compile(
            "\\b(?:improv(?:e|ed|ing)|increas(?:e|ed|ing)|reduc(?:e|ed|ing)|decreas(?:e|ed|ing)|"
                    + "grow(?:th|ing)?|boost(?:ed|ing)?|optimiz(?:e|ed|ing)|enhanc(?:e|ed|ing))\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMEBOX = Pattern.compile(
            "\\b(?:week|weeks|month|months|quarter|quarters|day|days|year|years|january|february|march|"
                    + "april|may|june|july|august|september|october|november|december|q[1-4]|20\\d{2})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DUTY_SENTENCE = Pattern.compile(
            "^\\s*(?:responsible for|duties included|my role was to|tasks included)\\b",
            Pattern.CASE_INSENSITIVE);

    public record FeedbackDraft(
            String dimension,
            String ruleId,
            String targetQuote,
            String issue,
            String rationale,
            String suggestion
    ) {}

    public List<FeedbackDraft> evaluate(String bodyText, List<BhvStarEvidenceRevisionEntity> revisions) {
        List<FeedbackDraft> items = new ArrayList<>();
        String text = bodyText == null ? "" : bodyText;

        for (BhvStarEvidenceRevisionEntity revision : revisions) {
            checkEvidenceField(items, revision, revision.getSituation(), "BHV-L-S01",
                    "Situation is missing in linked STAR evidence",
                    "Add context about when and where the situation occurred.");
            checkEvidenceField(items, revision, revision.getTask(), "BHV-L-S02",
                    "Task is missing in linked STAR evidence",
                    "Clarify your responsibility or goal in that situation.");
            checkEvidenceField(items, revision, revision.getAction(), "BHV-L-S03",
                    "Action is missing in linked STAR evidence",
                    "Describe the specific actions you personally took.");
            checkEvidenceField(items, revision, revision.getResult(), "BHV-L-S04",
                    "Result is missing in linked STAR evidence",
                    "State the measurable or observable outcome.");
        }

        if (revisions.isEmpty() || needsBodyHeuristics(revisions)) {
            evaluateBodyHeuristics(items, text, revisions);
        }

        evaluateSpecificity(items, text);
        return items;
    }

    private void checkEvidenceField(
            List<FeedbackDraft> items,
            BhvStarEvidenceRevisionEntity revision,
            String fieldValue,
            String ruleId,
            String issue,
            String suggestion) {
        if (!BehavioralSupport.isBlank(fieldValue)) {
            return;
        }
        items.add(new FeedbackDraft(
                "STAR_COMPLETENESS",
                ruleId,
                excerpt(revision.getSituation(), revision.getTask(), revision.getAction(), revision.getResult()),
                issue,
                "Answer references STAR evidence with an incomplete revision.",
                suggestion));
    }

    private boolean needsBodyHeuristics(List<BhvStarEvidenceRevisionEntity> revisions) {
        for (BhvStarEvidenceRevisionEntity revision : revisions) {
            if (BehavioralSupport.isBlank(revision.getSituation())
                    || BehavioralSupport.isBlank(revision.getTask())
                    || BehavioralSupport.isBlank(revision.getAction())
                    || BehavioralSupport.isBlank(revision.getResult())) {
                return true;
            }
        }
        return false;
    }

    private void evaluateBodyHeuristics(
            List<FeedbackDraft> items,
            String text,
            List<BhvStarEvidenceRevisionEntity> revisions) {
        if (BehavioralSupport.wordCount(text) < 80 && !TIME_BACKGROUND.matcher(text).find()) {
            items.add(new FeedbackDraft(
                    "STAR_COMPLETENESS",
                    "BHV-L-S10",
                    excerpt(text, 120),
                    "Answer lacks situational context",
                    "Short answers without time or background cues often miss Situation.",
                    "Add when and where the example happened before describing actions."));
        }

        if (!FIRST_PERSON_ACTION.matcher(text).find()) {
            items.add(new FeedbackDraft(
                    "STAR_COMPLETENESS",
                    "BHV-L-S11",
                    excerpt(text, 120),
                    "Answer lacks first-person actions",
                    "Behavioral answers should show what you personally did.",
                    "Use explicit first-person verbs such as \"I led\", \"I implemented\", or \"I decided\"."));
        }

        boolean anyResultEmpty = revisions.stream().anyMatch(r -> BehavioralSupport.isBlank(r.getResult()));
        if (!RESULT_WORDS.matcher(text).find() && (revisions.isEmpty() || anyResultEmpty)) {
            items.add(new FeedbackDraft(
                    "STAR_COMPLETENESS",
                    "BHV-L-S12",
                    excerpt(text, 120),
                    "Answer lacks a clear result or impact",
                    "No result language was detected and linked evidence results are empty.",
                    "Close with a concrete outcome, impact, or lesson learned."));
        }
    }

    private void evaluateSpecificity(List<FeedbackDraft> items, String text) {
        if (BehavioralSupport.isBlank(text)) {
            return;
        }

        Matcher vagueMatcher = VAGUE_WORDS.matcher(text);
        while (vagueMatcher.find()) {
            String sentence = sentenceContaining(text, vagueMatcher.start());
            if (!METRIC.matcher(sentence).find()) {
                items.add(new FeedbackDraft(
                        "EVIDENCE_SPECIFICITY",
                        "BHV-L-E01",
                        excerpt(sentence, 160),
                        "Vague wording without verifiable detail",
                        "Generic adjectives without numbers or time bounds reduce credibility.",
                        "Replace vague claims with a metric, baseline, or time-bound outcome you can verify."));
                break;
            }
        }

        Matcher improvementMatcher = IMPROVEMENT_CLAIM.matcher(text);
        while (improvementMatcher.find()) {
            String sentence = sentenceContaining(text, improvementMatcher.start());
            if (!METRIC.matcher(sentence).find()) {
                items.add(new FeedbackDraft(
                        "EVIDENCE_SPECIFICITY",
                        "BHV-L-E02",
                        excerpt(sentence, 160),
                        "Improvement claim lacks measurable detail",
                        "Claims about improvement should include a number, ratio, or named metric.",
                        "Add a percentage, count, or before/after metric instead of an unqualified improvement claim."));
                break;
            }
        }

        if (containsContinuousImprovement(text) && !TIMEBOX.matcher(text).find()) {
            items.add(new FeedbackDraft(
                    "EVIDENCE_SPECIFICITY",
                    "BHV-L-E03",
                    excerpt(text, 160),
                    "Sustained improvement lacks a time box",
                    "Ongoing improvement stories need a timeframe to be verifiable.",
                    "Specify whether the improvement happened over weeks, months, or a quarter."));
        }

        if (hasDutyStacking(text)) {
            items.add(new FeedbackDraft(
                    "EVIDENCE_SPECIFICITY",
                    "BHV-L-E04",
                    excerpt(text, 160),
                    "Answer reads like a duty list",
                    "Multiple consecutive responsibility statements without personal action detail.",
                    "Pick one responsibility and explain what you personally did and what changed."));
        }
    }

    private boolean containsContinuousImprovement(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("over time") || lower.contains("continuously") || lower.contains("ongoing")
                || lower.contains("consistently") || lower.contains("regularly improved");
    }

    private boolean hasDutyStacking(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        int consecutive = 0;
        for (String sentence : sentences) {
            if (DUTY_SENTENCE.matcher(sentence).find()) {
                consecutive++;
                if (consecutive >= 2) {
                    return true;
                }
            } else if (!sentence.isBlank()) {
                consecutive = 0;
            }
        }
        return false;
    }

    private String sentenceContaining(String text, int index) {
        int start = text.lastIndexOf('.', index);
        if (start < 0) {
            start = text.lastIndexOf('!', index);
        }
        if (start < 0) {
            start = text.lastIndexOf('?', index);
        }
        start = start < 0 ? 0 : start + 1;

        int end = text.indexOf('.', index);
        if (end < 0) {
            end = text.indexOf('!', index);
        }
        if (end < 0) {
            end = text.indexOf('?', index);
        }
        if (end < 0) {
            end = text.length();
        } else {
            end = end + 1;
        }
        return text.substring(start, end).trim();
    }

    private String excerpt(String text, int maxLen) {
        if (BehavioralSupport.isBlank(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen).trim() + "...";
    }

    private String excerpt(String situation, String task, String action, String result) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, situation);
        appendPart(sb, task);
        appendPart(sb, action);
        appendPart(sb, result);
        return excerpt(sb.toString(), 160);
    }

    private void appendPart(StringBuilder sb, String part) {
        if (!BehavioralSupport.isBlank(part)) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(part.trim());
        }
    }
}
