package com.threatpulse.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EmbeddingServiceTest {
    private static final String BASE_URL = "http://hf.test/models";
    private static final String MODEL = "BAAI/bge-small-en-v1.5";
    private static final String ENDPOINT = BASE_URL + "/" + MODEL + "/pipeline/feature-extraction";
    private static final String QUERY_PREFIX = "Represent this sentence for searching relevant passages: ";

    private MockRestServiceServer server;
    private EmbeddingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new EmbeddingService("test-key", MODEL, BASE_URL,
                builder);
    }

    /** Builds a JSON array with the given number of identical floats, e.g. [0.1,0.1,0.1]. */
    private static String vectorJson(int size) {
        return "[" + String.join(",", Collections.nCopies(size, "0.1")) + "]";
    }

    @Test
    void embedDocument_shouldCallEndpointAndReturnVector_whenResponseIsValid() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().json("{\"inputs\":\"Spring RCE advisory\"}"))
                .andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        float[] result = service.embedDocument("Spring RCE advisory");

        assertThat(result).isNotNull().hasSize(384);
        server.verify();
    }

    @Test
    void embedDocument_shouldReturnNull_whenApiReturnsServerError() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Model is currently loading\"}"));

        float[] result = service.embedDocument("some text");

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void embedDocument_shouldReturnNull_whenApiReturnsTooManyRequests() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(service.embedDocument("some text")).isNull();
        server.verify();
    }

    @Test
    void embedDocument_shouldReturnNull_whenVectorHasWrongLength() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(vectorJson(10), MediaType.APPLICATION_JSON));

        assertThat(service.embedDocument("some text")).isNull();
        server.verify();
    }

    @Test
    void embedDocument_shouldTruncateText_whenLongerThanLimit() {
        String longText = "a".repeat(5000);
        String expectedSent = "a".repeat(1000);

        server.expect(requestTo(ENDPOINT))
                .andExpect(content().json("{\"inputs\":\"" + expectedSent + "\"}"))
                .andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(service.embedDocument(longText)).hasSize(384);
        server.verify();
    }

    @Test
    void embedDocument_shouldNotCallApi_whenTextIsNullOrBlank() {
        // No expectations registered: any HTTP request would make the mock server fail the test.
        assertThat(service.embedDocument(null)).isNull();
        assertThat(service.embedDocument("")).isNull();
        assertThat(service.embedDocument("   ")).isNull();
        server.verify();
    }

    @Test
    void embedDocument_shouldNotCallApi_whenApiKeyIsBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer noCallServer = MockRestServiceServer.bindTo(builder).build();
        EmbeddingService disabledService = new EmbeddingService("", MODEL, BASE_URL, builder);

        assertThat(disabledService.embedDocument("some text")).isNull();
        noCallServer.verify();
    }

    @Test
    void embedQuery_shouldAddPrefixToQuery() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(content().json("{\"inputs\":\"" + QUERY_PREFIX + "spring rce\"}"))
                .andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(service.embedQuery("spring rce")).hasSize(384);
        server.verify();
    }

    @Test
    void embedQuery_shouldNotCallApi_whenQueryIsNullOrBlank() {
        assertThat(service.embedQuery(null)).isNull();
        assertThat(service.embedQuery("  ")).isNull();
        server.verify();
    }

    // ── Credit protection ────────────────────────────────────────────────────────────

    private static final class MutableClock extends java.time.Clock {
        private java.time.Instant now = java.time.Instant.parse("2026-11-02T10:00:00Z");

        void advance(java.time.Duration duration) { now = now.plus(duration); }

        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public java.time.Instant instant() { return now; }
    }

    private EmbeddingService guardedService(MockRestServiceServer[] serverHolder, MutableClock clock,
                                            int maxDocuments, int maxQueries) {
        RestClient.Builder builder = RestClient.builder();
        serverHolder[0] = MockRestServiceServer.bindTo(builder).build();
        EmbeddingGuard guard = new EmbeddingGuard(clock, java.time.Duration.ofHours(12), maxDocuments, maxQueries);
        return new EmbeddingService("test-key", MODEL, BASE_URL, builder, guard);
    }

    @Test
    void embedDocument_shouldSendNoRequest_whileThePauseAfterNoCreditLasts() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        MutableClock clock = new MutableClock();
        EmbeddingService guarded = guardedService(holder, clock, 0, 0);
        // Exactly one request is expected: a second one would fail the test
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"no credits\"}"));

        assertThat(guarded.embedDocument("first")).isNull();
        assertThat(guarded.embedDocument("second")).isNull();
        assertThat(guarded.embedQuery("a search")).isNull();

        holder[0].verify();
    }

    @Test
    void embedDocument_shouldTryAgain_afterThePauseEnded() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        MutableClock clock = new MutableClock();
        EmbeddingService guarded = guardedService(holder, clock, 0, 0);
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED));
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(guarded.embedDocument("first")).isNull();
        clock.advance(java.time.Duration.ofHours(13));
        assertThat(guarded.embedDocument("second")).hasSize(384);

        holder[0].verify();
    }

    @Test
    void embedDocument_shouldStopAtTheDailyBudget() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        MutableClock clock = new MutableClock();
        EmbeddingService guarded = guardedService(holder, clock, 2, 0);
        holder[0].expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(ENDPOINT))
                .andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(guarded.embedDocument("one")).isNotNull();
        assertThat(guarded.embedDocument("two")).isNotNull();
        assertThat(guarded.embedDocument("three")).isNull();

        holder[0].verify();
    }

    @Test
    void embedQuery_shouldStillWork_whenTheDocumentBudgetIsUsedUp() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        MutableClock clock = new MutableClock();
        EmbeddingService guarded = guardedService(holder, clock, 1, 5);
        holder[0].expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(ENDPOINT))
                .andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(guarded.embedDocument("one")).isNotNull();
        assertThat(guarded.embedDocument("two")).isNull();
        // The backfill used up its own budget, search is not affected
        assertThat(guarded.embedQuery("a search")).isNotNull();

        holder[0].verify();
    }

    @Test
    void embedQuery_shouldCallTheApiOnlyOnce_forTheSameQuestion() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        EmbeddingService guarded = guardedService(holder, new MutableClock(), 0, 0);
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        float[] first = guarded.embedQuery("Remote Code Execution");
        // Same words, other spelling of case and spaces: still the cached answer
        float[] second = guarded.embedQuery("  remote code execution ");

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
        holder[0].verify();
    }

    @Test
    void embedQuery_shouldNotCacheAFailure() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        EmbeddingService guarded = guardedService(holder, new MutableClock(), 0, 0);
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(guarded.embedQuery("spring rce")).isNull();
        assertThat(guarded.embedQuery("spring rce")).hasSize(384);

        holder[0].verify();
    }

    @Test
    void embedDocument_shouldPauseForTheTimeInRetryAfter_onRateLimit() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        MutableClock clock = new MutableClock();
        EmbeddingService guarded = guardedService(holder, clock, 0, 0);
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "120"));
        holder[0].expect(requestTo(ENDPOINT)).andRespond(withSuccess(vectorJson(384), MediaType.APPLICATION_JSON));

        assertThat(guarded.embedDocument("one")).isNull();
        clock.advance(java.time.Duration.ofSeconds(60));
        assertThat(guarded.embedDocument("two")).isNull();   // still paused, no request
        clock.advance(java.time.Duration.ofSeconds(90));
        assertThat(guarded.embedDocument("three")).isNotNull();

        holder[0].verify();
    }
}
