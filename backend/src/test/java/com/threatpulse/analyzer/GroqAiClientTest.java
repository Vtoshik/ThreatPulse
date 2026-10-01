package com.threatpulse.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class GroqAiClientTest {
    private static final String BASE_URL = "http://groq.test/openai/v1";
    private static final String ENDPOINT = BASE_URL + "/chat/completions";
    private static final String ANALYSIS_JSON = """
            {"summary":"A flaw allows remote code execution.","severity":"HIGH","category":"RCE",
             "affected_technologies":["spring-boot"],"recommended_action":"Upgrade."}""";
    private static final String RATE_LIMIT_BODY =
            "{\"error\":{\"message\":\"Rate limit reached\",\"type\":\"tokens\"}}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Long> sleeps = new ArrayList<>();
    private final AtomicInteger throttleCalls = new AtomicInteger();

    private MockRestServiceServer server;
    private GroqAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = newClient(builder, 3, 60);
    }

    private GroqAiClient newClient(RestClient.Builder builder, int maxRetries, int maxWaitSeconds) {
        // Counts how many times the throttle is asked, without ever waiting
        RequestThrottle countingThrottle = new RequestThrottle(0) {
            @Override
            public void acquire() {
                throttleCalls.incrementAndGet();
            }
        };
        return new GroqAiClient("test-key", "openai/gpt-oss-120b", BASE_URL, maxRetries,
                maxWaitSeconds, objectMapper, builder, countingThrottle, sleeps::add);
    }

    private String completion(String messageContent) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of("choices",
                List.of(Map.of("message", Map.of("content", messageContent)))));
    }

    private String successBody() throws JsonProcessingException {
        return completion(ANALYSIS_JSON);
    }

    private void expectRateLimit(String retryAfter) {
        var response = withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(RATE_LIMIT_BODY);
        if (retryAfter != null) {
            response = response.header(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        server.expect(requestTo(ENDPOINT)).andRespond(response);
    }

    private void expectSuccess() throws JsonProcessingException {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));
    }

    @Test
    void analyze_shouldSendModelKeyAndJsonMode_andParseTheAnswer() throws Exception {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().json("""
                        {"model":"openai/gpt-oss-120b","response_format":{"type":"json_object"}}"""))
                .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON));

        ThreatAnalysis result = client.analyze("Spring RCE", "A flaw in Spring.");

        assertThat(result).isNotNull();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getCategory()).isEqualTo("RCE");
        assertThat(result.getAffectedTechnologies()).containsExactly("spring-boot");
        assertThat(sleeps).isEmpty();
        server.verify();
    }

    @Test
    void analyze_shouldStripMarkdownFences_aroundTheJson() throws Exception {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(completion("```json\n" + ANALYSIS_JSON + "\n```"),
                        MediaType.APPLICATION_JSON));

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNotNull();
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void analyze_shouldWaitForRetryAfterAndRetry_whenRateLimited() throws Exception {
        expectRateLimit("7");
        expectSuccess();

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNotNull();
        assertThat(sleeps).containsExactly(7000L);
        server.verify();
    }

    @Test
    void analyze_shouldUseExponentialBackoff_whenThereIsNoRetryAfterHeader() throws Exception {
        expectRateLimit(null);
        expectRateLimit(null);
        expectSuccess();

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNotNull();
        assertThat(sleeps).containsExactly(2000L, 4000L);
        server.verify();
    }

    @Test
    void analyze_shouldFallBackToBackoff_whenRetryAfterIsNotANumber() throws Exception {
        expectRateLimit("Wed, 21 Oct 2026 07:28:00 GMT");
        expectSuccess();

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNotNull();
        assertThat(sleeps).containsExactly(2000L);
    }

    @Test
    void analyze_shouldGiveUpAfterTheLastRetry_andReturnNull() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = newClient(builder, 2, 60);
        expectRateLimit(null);
        expectRateLimit(null);
        expectRateLimit(null);

        ThreatAnalysis result = client.analyze("title", "description");

        // One first try and two retries, then it stops
        assertThat(result).isNull();
        assertThat(sleeps).containsExactly(2000L, 4000L);
        server.verify();
    }

    @Test
    void analyze_shouldNotWait_whenGroqAsksForALongerWaitThanAllowed() {
        // For example the daily token limit: waiting an hour would block the pipeline
        expectRateLimit("3600");

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNull();
        assertThat(sleeps).isEmpty();
        server.verify();
    }

    @Test
    void analyze_shouldRetry_whenServerIsTemporarilyUnavailable() throws Exception {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectSuccess();

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNotNull();
        assertThat(sleeps).containsExactly(2000L);
        server.verify();
    }

    @Test
    void analyze_shouldNotRetry_whenTheRequestIsRejected() {
        // A 400 or 401 will not get better by trying again
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"model not found\"}}"));

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNull();
        assertThat(sleeps).isEmpty();
        server.verify();
    }

    @Test
    void analyze_shouldAskTheThrottleBeforeEveryAttempt() throws Exception {
        expectRateLimit("1");
        expectSuccess();

        client.analyze("title", "description");

        // The retry counts against the rate limit like any other request
        assertThat(throttleCalls.get()).isEqualTo(2);
    }

    @Test
    void analyze_shouldReturnNull_whenTheAnswerHasNoChoices() {
        // A 200 response that is not a normal answer used to cause a NullPointerException
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"error\":{\"message\":\"something\"}}",
                        MediaType.APPLICATION_JSON));

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNull();
    }

    @Test
    void analyze_shouldReturnNull_whenTheModelDoesNotReturnJson() throws Exception {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(completion("Sorry, I cannot help with that."),
                        MediaType.APPLICATION_JSON));

        ThreatAnalysis result = client.analyze("title", "description");

        assertThat(result).isNull();
    }
}
