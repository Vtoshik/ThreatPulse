package com.threatpulse.alerts;

import com.threatpulse.common.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EmailNotifierTest {
    private static final String BASE_URL = "http://resend.test";

    private MockRestServiceServer server;
    private EmailNotifier notifier;

    private final AlertMessage message = new AlertMessage(
            "Spring RCE", "A remote code execution flaw", Severity.CRITICAL, "https://example.com/advisory");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        notifier = new EmailNotifier(builder, "test-key", BASE_URL, "alerts@example.com",
                "me@example.com, other@example.com");
    }

    @Test
    void send_shouldPostToResendAndReportSent() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().json("{\"from\":\"alerts@example.com\","
                        + "\"to\":[\"me@example.com\",\"other@example.com\"]}"))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        assertThat(notifier.send(message)).isEqualTo(NotificationResult.SENT);
        server.verify();
    }

    @Test
    void send_shouldReportPermanentFailure_whenResendRejectsTheRequest() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON).body("{\"message\":\"domain not verified\"}"));

        // A failure must never look like a success, or the alert would be recorded as sent
        assertThat(notifier.send(message)).isEqualTo(NotificationResult.PERMANENT_FAILURE);
    }

    @Test
    void send_shouldReportRetryableFailure_onRateLimit() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(notifier.send(message)).isEqualTo(NotificationResult.RETRYABLE_FAILURE);
    }

    @Test
    void send_shouldReportRetryableFailure_onServerError() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThat(notifier.send(message)).isEqualTo(NotificationResult.RETRYABLE_FAILURE);
    }

    @Test
    void send_shouldReportRetryableFailure_whenTheServerCannotBeReached() {
        server.expect(requestTo(BASE_URL + "/emails"))
                .andRespond(withException(new IOException("timeout")));

        assertThat(notifier.send(message)).isEqualTo(NotificationResult.RETRYABLE_FAILURE);
    }

    @Test
    void buildHtml_shouldEscapeUntrustedText() {
        AlertMessage hostile = new AlertMessage(
                "<script>alert(1)</script>", "<img src=x onerror=alert(2)>", Severity.HIGH,
                "javascript:alert(3)");

        String html = EmailNotifier.buildHtml(hostile);

        assertThat(html).doesNotContain("<script>").doesNotContain("<img");
        assertThat(html).contains("&lt;script&gt;");
        // Only http(s) links are shown
        assertThat(html).doesNotContain("javascript:");
    }

    @Test
    void buildSubject_shouldNotContainLineBreaks() {
        AlertMessage message = new AlertMessage("first\r\nBcc: x@evil.test", "s", Severity.LOW, null);

        assertThat(EmailNotifier.buildSubject(message)).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void constructor_shouldFail_whenNoRecipientIsConfigured() {
        assertThatThrownBy(() -> new EmailNotifier(RestClient.builder(), "key", BASE_URL, "a@example.com", " "))
                .isInstanceOf(IllegalStateException.class);
    }
}
