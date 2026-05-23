package com.threatpulse.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GroqAiClient {
    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public GroqAiClient(
            @Value("${app.groq.api-key}") String apiKey,
            @Value("${app.groq.model}") String model,
            @Value("${app.groq.base-url}") String baseUrl, ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public ThreatAnalysis analyze(String title, String description) {
        String prompt = buildPrompt(title, description);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a cybersecurity analyst. Respond only with valid JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (req, res) -> log.error("Groq API error: {} - {}", res.getStatusCode(), res.getBody()))
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            content = content.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();

            return objectMapper.readValue(content, ThreatAnalysis.class);


        } catch (Exception e) {
            log.error("Groq API call failed for title: {}", title , e);
            return null;
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
