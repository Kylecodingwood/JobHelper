package com.jobhelper.roadmap.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a simple Markdown checklist into template task definitions.
 *
 * <pre>
 * # Phase name
 * - [ ] Task title
 * - [ ] Another task {key=cv.extra offset=-10 depends=cv.base-draft priority=HIGH}
 * </pre>
 */
public final class TemplateMarkdownParser {
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.*\\S)\\s*$");
    private static final Pattern CHECKLIST = Pattern.compile("^[-*]\\s*\\[( |x|X)]\\s*(.*)$");
    private static final Pattern META = Pattern.compile("\\{([^}]*)}\\s*$");

    private TemplateMarkdownParser() {}

    public static ParseResult parse(String markdownContent) {
        List<String> phases = new ArrayList<>();
        List<ParsedTask> tasks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();

        if (markdownContent == null || markdownContent.isBlank()) {
            errors.add(new ParseError(0, "RDM-MD-EMPTY-CONTENT", "markdownContent is empty"));
            return new ParseResult(false, phases, tasks, warnings, errors);
        }

        String currentPhase = null;
        String[] lines = markdownContent.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String raw = lines[i];
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            Matcher headingMatcher = HEADING.matcher(line);
            if (headingMatcher.matches()) {
                currentPhase = headingMatcher.group(1).strip();
                if (!phases.contains(currentPhase)) {
                    phases.add(currentPhase);
                }
                continue;
            }
            Matcher checklistMatcher = CHECKLIST.matcher(line);
            if (checklistMatcher.matches()) {
                String rest = checklistMatcher.group(2).strip();
                String meta = null;
                Matcher metaMatcher = META.matcher(rest);
                if (metaMatcher.find()) {
                    meta = metaMatcher.group(1);
                    rest = rest.substring(0, metaMatcher.start()).strip();
                }
                if (rest.isEmpty()) {
                    errors.add(new ParseError(lineNo, "RDM-MD-EMPTY-TITLE", "Checklist item has no title"));
                    continue;
                }
                String phase = currentPhase;
                if (phase == null) {
                    phase = "未分类";
                    warnings.add("line " + lineNo + ": no preceding '#' phase heading, defaulted to 未分类");
                    if (!phases.contains(phase)) {
                        phases.add(phase);
                    }
                }
                MetaFields fields = parseMeta(meta);
                String key = fields.key != null ? fields.key : slug(rest);
                String uniqueKey = key;
                int suffix = 2;
                while (!usedKeys.add(uniqueKey)) {
                    uniqueKey = key + "-" + suffix++;
                }
                tasks.add(new ParsedTask(
                        uniqueKey,
                        rest,
                        phase,
                        fields.anchor,
                        fields.offsetDays,
                        fields.dependsOn,
                        fields.completionCriteria,
                        fields.priority == null ? "MEDIUM" : fields.priority));
                continue;
            }
            if (line.startsWith("-") || line.startsWith("*")) {
                warnings.add("line " + lineNo + ": ignored malformed checklist line");
            }
        }

        if (tasks.isEmpty()) {
            errors.add(new ParseError(0, "RDM-MD-NO-TASKS", "No '- [ ] task' checklist items found"));
        }

        boolean parseOk = errors.isEmpty();
        return new ParseResult(parseOk, phases, tasks, warnings, errors);
    }

    private static MetaFields parseMeta(String meta) {
        MetaFields fields = new MetaFields();
        if (meta == null || meta.isBlank()) {
            return fields;
        }
        for (String part : meta.split("\\s+")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = part.substring(0, eq).strip().toLowerCase(Locale.ROOT);
            String v = part.substring(eq + 1).strip();
            switch (k) {
                case "key" -> fields.key = v;
                case "anchor" -> fields.anchor = v.toUpperCase(Locale.ROOT);
                case "offset" -> fields.offsetDays = parseIntSafe(v);
                case "depends" -> fields.dependsOn = List.of(v.split(","))
                        .stream().map(String::strip).filter(s -> !s.isEmpty()).toList();
                case "priority" -> fields.priority = v.toUpperCase(Locale.ROOT);
                case "criteria" -> fields.completionCriteria = v.replace('_', ' ');
                default -> { /* ignore unknown metadata keys */ }
            }
        }
        return fields;
    }

    private static int parseIntSafe(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String slug(String title) {
        String s = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return (s.isEmpty() ? "task" : s);
    }

    private static class MetaFields {
        String key;
        String anchor;
        int offsetDays;
        List<String> dependsOn = List.of();
        String completionCriteria;
        String priority;
    }

    public record ParseError(int line, String code, String message) {}

    public record ParsedTask(
            String templateTaskKey,
            String title,
            String phase,
            String anchorType,
            int relativeOffsetDays,
            List<String> dependsOn,
            String completionCriteria,
            String priority) {}

    public record ParseResult(
            boolean parseOk,
            List<String> phases,
            List<ParsedTask> tasks,
            List<String> warnings,
            List<ParseError> errors) {}
}
