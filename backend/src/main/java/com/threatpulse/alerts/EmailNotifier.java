package com.threatpulse.alerts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Sends alerts as email through the Resend API.
 * <p>
 * Off by default. The recipients are set in the configuration (app.notifications.email.to),
 * no address is ever stored in the database.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class EmailNotifier implements Notifier {
    private final RestClient restClient;
    private final String sourceEmail;
    private final List<String> recipients;

    public EmailNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${app.resend.api-key:}") String apiKey,
            @Value("${app.resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${app.resend.source-email:}") String sourceEmail,
            @Value("${app.notifications.email.to:}") String recipients
    ) {
        this.recipients = Arrays.stream(recipients.split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .toList();
        if (apiKey.isBlank() || sourceEmail.isBlank() || this.recipients.isEmpty()) {
            throw new IllegalStateException(
                    "Email alerts are enabled, so app.resend.api-key, app.resend.source-email "
                            + "and app.notifications.email.to must all be set");
        }
        this.sourceEmail = sourceEmail;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public NotificationResult send(AlertMessage message) {
        Map<String, Object> requestBody = Map.of(
                "from", sourceEmail,
                "to", recipients,
                "subject", buildSubject(message),
                "html", buildHtml(message)
        );

        try {
            restClient.post()
                    .uri("/emails")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Alert email sent: {}", message.title());
            return NotificationResult.SENT;
        } catch (RestClientResponseException e) {
            // The default status handler throws for every 4xx and 5xx answer
            HttpStatusCode status = e.getStatusCode();
            boolean temporary = status.value() == 429 || status.is5xxServerError();
            log.error("Resend rejected the alert email: {} {}", status.value(),
                    e.getResponseBodyAsString());
            return temporary ? NotificationResult.RETRYABLE_FAILURE
                    : NotificationResult.PERMANENT_FAILURE;
        } catch (ResourceAccessException e) {
            // Connection refused, DNS failure or a timeout
            log.error("Resend could not be reached: {}", e.getMessage());
            return NotificationResult.RETRYABLE_FAILURE;
        } catch (RuntimeException e) {
            log.error("Unexpected error while sending the alert email", e);
            return NotificationResult.PERMANENT_FAILURE;
        }
    }

    static String buildSubject(AlertMessage message) {
        // A line break in a header value would let text injected by a source add headers
        String title = message.title() == null ? "" : message.title().replaceAll("[\\r\\n]+", " ");
        return "[" + message.severity() + "] " + title;
    }

    /**
     * The title and the summary come from feeds and from an LLM, so they are untrusted.
     * Everything is HTML-escaped and only http(s) links are used.
     */
    static String buildHtml(AlertMessage message) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>").append(HtmlUtils.htmlEscape(nullToEmpty(message.title()))).append("</h2>");
        html.append("<p><strong>Severity:</strong> ").append(message.severity()).append("</p>");
        html.append("<p>").append(HtmlUtils.htmlEscape(nullToEmpty(message.summary()))).append("</p>");
        String url = message.sourceUrl();
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
            html.append("<p><a href=\"").append(HtmlUtils.htmlEscape(url)).append("\">Source</a></p>");
        }
        return html.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
