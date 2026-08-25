package com.jobhelper.leetcode.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class LeetCodeContentClient {
    private static final Logger log = LoggerFactory.getLogger(LeetCodeContentClient.class);
    private static final String GRAPHQL = "https://leetcode.com/graphql";
    private static final String QUERY = """
            query getQuestionDetail($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                content
                exampleTestcases
                title
              }
            }
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public LeetCodeContentClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<FetchedContent> fetch(String titleSlug) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", QUERY);
            ObjectNode variables = body.putObject("variables");
            variables.put("titleSlug", titleSlug);

            HttpRequest req = HttpRequest.newBuilder(URI.create(GRAPHQL))
                    .timeout(Duration.ofSeconds(25))
                    .header("Content-Type", "application/json")
                    .header("Referer", "https://leetcode.com/problems/" + titleSlug + "/")
                    .header("User-Agent", "JobHelper/1.0 (local study tool)")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("LeetCode GraphQL HTTP {} for slug={}", res.statusCode(), titleSlug);
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(res.body());
            JsonNode q = root.path("data").path("question");
            if (q.isMissingNode() || q.isNull()) {
                log.warn("LeetCode GraphQL empty question for slug={}", titleSlug);
                return Optional.empty();
            }
            String html = textOrNull(q.get("content"));
            String examples = textOrNull(q.get("exampleTestcases"));
            if (html == null || html.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new FetchedContent(html, examples == null ? "" : examples));
        } catch (Exception e) {
            log.warn("LeetCode fetch failed for {}: {}", titleSlug, e.getMessage());
            return Optional.empty();
        }
    }

    private String textOrNull(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        String s = n.asText();
        return s == null || s.isBlank() ? null : s;
    }

    public record FetchedContent(String statementHtml, String examples) {}
}
