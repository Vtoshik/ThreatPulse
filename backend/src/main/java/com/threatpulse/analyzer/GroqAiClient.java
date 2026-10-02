package com.threatpulse.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends a threat to the Groq chat API and reads back the structured analysis.
 * <p>
 * Groq limits both requests and tokens per minute on the free tier, so every call goes through
 * a {@link RequestThrottle}. If Groq still answers with a rate limit or a server error, the call
 * is retried after the wait time Groq asks for (Retry-After header) or after a short backoff.
 * When the analysis cannot be obtained, {@link #analyze} returns null and never throws.
 */
@Slf4j
@Component
public class GroqAiClient {
    private static final long BASE_BACKOFF_MILLIS = 2000;
    // Rate limit and temporary server problems are worth another try, other errors are not
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);
    // A request that is bigger than the tokens-per-minute limit is rejected every time, so the
    // text is cut. About 4000 characters is roughly 1000 tokens, enough to understand a threat.
    private static final int MAX_TITLE_CHARS = 500;
    private static final int MAX_DESCRIPTION_CHARS = 4000;

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;
    private final RequestThrottle throttle;
    private final RequestThrottle.Sleeper sleeper;
    private final int maxRetries;
    private final long maxRetryWaitMillis;

    @Autowired
    public GroqAiClient(
            @Value("${app.groq.api-key}") String apiKey,
            @Value("${app.groq.model}") String model,
            @Value("${app.groq.base-url}") String baseUrl,
            @Value("${app.groq.requests-per-minute:12}") int requestsPerMinute,
            @Value("${app.groq.max-retries:3}") int maxRetries,
            @Value("${app.groq.max-retry-wait-seconds:60}") int maxRetryWaitSeconds,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this(apiKey, model, baseUrl, maxRetries, maxRetryWaitSeconds, objectMapper,
                restClientBuilder, new RequestThrottle(requestsPerMinute), Thread::sleep);
    }

    // Used by tests to replace the throttle and the sleep
    GroqAiClient(String apiKey, String model, String baseUrl, int maxRetries,
                 int maxRetryWaitSeconds, ObjectMapper objectMapper,
                 RestClient.Builder restClientBuilder, RequestThrottle throttle,
                 RequestThrottle.Sleeper sleeper) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.throttle = throttle;
        this.sleeper = sleeper;
        this.maxRetries = maxRetries;
        this.maxRetryWaitMillis = maxRetryWaitSeconds * 1000L;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * @return the analysis, or null if it could not be obtained
     */
    public ThreatAnalysis analyze(String title, String description) {
        Map<String, Object> requestBody = buildRequestBody(buildPrompt(
                limit(title, MAX_TITLE_CHARS), limit(description, MAX_DESCRIPTION_CHARS)));

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            throttle.acquire();

            try {
                String response = restClient.post()
                        .uri("/chat/completions")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                return parseAnalysis(response, title);

            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();

                if (!RETRYABLE_STATUSES.contains(status) || attempt == maxRetries) {
                    log.error("Groq API error {} for title: {} - {}",
                            status, title, e.getResponseBodyAsString());
                    return null;
                }

                long waitMillis = retryDelayMillis(e, attempt);
                if (waitMillis > maxRetryWaitMillis) {
                    // For example the daily token limit is used up: waiting would block for hours
                    log.error("Groq API error {} for title: {}, asked to wait {} ms which is "
                            + "more than the allowed {} ms, giving up",
                            status, title, waitMillis, maxRetryWaitMillis);
                    return null;
                }

                log.warn("Groq API error {} for title: {}, retrying in {} ms (retry {} of {})",
                        status, title, waitMillis, attempt + 1, maxRetries);
                if (!pause(waitMillis)) {
                    return null;
                }

            } catch (Exception e) {
                log.error("Groq API call failed for title: {}", title, e);
                return null;
            }
        }

        return null;
    }

    private static String limit(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a cybersecurity analyst. Respond only with valid JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );
    }

    private ThreatAnalysis parseAnalysis(String response, String title) throws Exception {
        if (response == null || response.isBlank()) {
            log.error("Groq returned an empty response for title: {}", title);
            return null;
        }

        // path() never returns null, so a missing field does not cause a NullPointerException
        JsonNode contentNode = objectMapper.readTree(response)
                .path("choices").path(0).path("message").path("content");

        if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
            log.error("Groq response has no message content for title: {}", title);
            return null;
        }

        String content = contentNode.asText()
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*$", "")
                .trim();

        return objectMapper.readValue(content, ThreatAnalysis.class);
    }

    /**
     * Uses the Retry-After header (seconds) when Groq sends one, otherwise doubles the wait
     * on every attempt: 2 s, 4 s, 8 s.
     */
    private long retryDelayMillis(RestClientResponseException e, int attempt) {
        HttpHeaders headers = e.getResponseHeaders();
        String retryAfter = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);

        if (retryAfter != null) {
            try {
                return (long) (Double.parseDouble(retryAfter.trim()) * 1000);
            } catch (NumberFormatException ignored) {
                // The header can also be a date, which Groq does not use: fall back to backoff
            }
        }

        return BASE_BACKOFF_MILLIS << attempt;
    }

    /**
     * @return false if the thread was interrupted while waiting
     */
    private boolean pause(long millis) {
        try {
            sleeper.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String buildPrompt(String title, String description) {
        return """
                You are analyzing a cybersecurity item (may be a vulnerability, attack, breach, or security news).
                Respond with ONLY a valid JSON object — no markdown, no explanation.

                Rules:
                - summary: ALWAYS provide 2-4 sentences. Explain what happened, who is affected, and the risk.
                  If this is a security improvement or fix (not an attack), describe what protection it adds.
                - severity: CRITICAL (active exploitation/RCE), HIGH (serious vuln), MEDIUM (limited impact),
                  LOW (minor risk), INFO (news, fixes, advisories with no active threat)
                - category: RCE, XSS, SQLI, DATA_BREACH, SUPPLY_CHAIN, OTHER
                - affected_technologies: list of specific tech names (e.g. "nginx", "spring-boot", "npm").
                  Empty array [] only if truly generic with no specific tech mentioned.
                - recommended_action: one concrete sentence for a developer

                {
                    "summary": "...",
                    "severity": "CRITICAL|HIGH|MEDIUM|LOW|INFO",
                    "category": "RCE|XSS|SQLI|DATA_BREACH|SUPPLY_CHAIN|OTHER",
                    "affected_technologies": ["tech1", "tech2"],
                    "recommended_action": "..."
                }

                Title: %s
                Description: %s
                """.formatted(title, description);
    }
}
