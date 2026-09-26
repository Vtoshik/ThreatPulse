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
}
