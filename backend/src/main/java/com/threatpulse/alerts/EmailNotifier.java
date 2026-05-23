package com.threatpulse.alerts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class EmailNotifier {
    private final RestClient restClient;
    @Value("${app.resend.source-email}")
    private String sourceEmail;

    public EmailNotifier(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.resend.base-url}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendAlert(String[] toEmail, String subject, String htmlContent) {
        Map<String, Object> requestBody = Map.of(
                "from", sourceEmail,
                "to", sourceEmail,
                "bcc", toEmail,
                "subject", subject,
                "html", htmlContent
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (req, res) -> {
                                String body = new String(res.getBody().readAllBytes());
                                log.error("Resend API error: {} — {}", res.getStatusCode(), body);
                            })
                    .toBodilessEntity();
            log.info("Alert email sent to {} recipients, subject: {}", toEmail.length, subject);
        } catch (Exception e) {
            log.error("Resend API call failed for subject: {}", subject, e);
        }
    }
}
