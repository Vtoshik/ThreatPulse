package com.threatpulse.analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates text embeddings with the Hugging Face Inference API.
 * <p>
 * The provider's free credit is small, so every call goes through an {@link EmbeddingGuard}
 * (pause after errors, daily budget) and search queries are cached for a while. Every failure
 * path returns null, never an exception: the callers treat null as "no embedding" and fall back.
 */
@Component
@Slf4j
public class EmbeddingService {
    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private final EmbeddingGuard guard;
    private final QueryCache queryCache = new QueryCache(QUERY_CACHE_SIZE, QUERY_CACHE_TTL);
    private static final int EMBED_LENGTH = 384;
    private static final String QUERY_PREFIX =
            "Represent this sentence for searching relevant passages: ";
    private static final int MAX_DOCUMENT_CHARS = 1000;
    private static final int QUERY_CACHE_SIZE = 200;
    private static final Duration QUERY_CACHE_TTL = Duration.ofHours(1);

    @Autowired
    public EmbeddingService(
        @Value("${app.huggingface.api-key}") String apiKey,
        @Value("${app.huggingface.model}") String model,
        @Value("${app.huggingface.base-url}") String baseUrl,
        @Value("${app.huggingface.max-document-requests-per-day:300}") int maxDocumentsPerDay,
        @Value("${app.huggingface.max-query-requests-per-day:200}") int maxQueriesPerDay,
        @Value("${app.huggingface.out-of-credits-pause-hours:12}") int outOfCreditsPauseHours,
        RestClient.Builder restClientBuilder
    ){
        this(apiKey, model, baseUrl, restClientBuilder,
                new EmbeddingGuard(Clock.systemUTC(), Duration.ofHours(outOfCreditsPauseHours),
                        maxDocumentsPerDay, maxQueriesPerDay));
    }

    /** No daily limits. Kept for callers and tests that do not care about the budget. */
    public EmbeddingService(String apiKey, String model, String baseUrl, RestClient.Builder restClientBuilder) {
        this(apiKey, model, baseUrl, restClientBuilder, EmbeddingGuard.unlimited());
    }

    // Used by tests to control the clock and the limits of the guard
    EmbeddingService(String apiKey, String model, String baseUrl, RestClient.Builder restClientBuilder,
                     EmbeddingGuard guard) {
        this.apiKey = apiKey;
        this.guard = guard;
        if (isNullOrBlank(apiKey)) {
            log.warn("Hugging Face API key is missing, embeddings are disabled");
        }

        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private static boolean isNullOrBlank(String s){
        return s == null || s.isBlank();
    }

    private float[] embed(String text, EmbeddingGuard.Kind kind){
        if (isNullOrBlank(apiKey) || isNullOrBlank(text)) return null;
        // No request at all while paused or when today's budget is used up
        if (!guard.tryAcquire(kind)) return null;
        Map<String, Object> requestBody = Map.of("inputs", text);
        float[] vector;

        try {
            vector = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/" + model + "/pipeline/feature-extraction")
                        .build())
                .body(requestBody).retrieve().body(float[].class);

        } catch (RestClientResponseException e ){
            log.error("Hugging Face call failed with code: {} and error: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            guard.recordFailure(e.getStatusCode().value(), retryAfter(e));
            return null;
        } catch (Exception e) {
            log.error("Hugging Face call failed for text", e);
            guard.recordFailure(0, null);
            return null;
        }

        // The provider answered, so the call worked, whatever the content is
        guard.recordSuccess();

        if (vector == null){
            log.error("Final vector is empty");
            return null;
        }

        if (vector.length != EMBED_LENGTH) {
            log.error("Expected vector length: {}, got: {} ", EMBED_LENGTH, vector.length);
            return null;
        }

        return vector;
    }

    private static Duration retryAfter(RestClientResponseException e) {
        String value = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (value == null) return null;
        try {
            return Duration.ofSeconds(Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public float[] embedDocument(String text) {
        if (isNullOrBlank(text)) return null;
        text = text.substring(0, Math.min(text.length(), MAX_DOCUMENT_CHARS));
        return embed(text, EmbeddingGuard.Kind.DOCUMENT);
    }

    public float[] embedQuery(String text) {
        if (isNullOrBlank(text)) return null;

        // The same question asked again must not cost another call
        String cacheKey = text.trim().toLowerCase(Locale.ROOT);
        float[] cached = queryCache.get(cacheKey);
        if (cached != null) return cached;

        float[] vector = embed(QUERY_PREFIX + text, EmbeddingGuard.Kind.QUERY);
        if (vector != null) {
            queryCache.put(cacheKey, vector);
        }
        return vector;
    }

    /**
     * Small in-memory cache with a size limit (least recently used goes first) and a time limit.
     * Only successful vectors are stored, so a failure is never cached.
     */
    private static final class QueryCache {
        private record Entry(float[] vector, long expiresAtMillis) {}

        private final Duration ttl;
        private final Map<String, Entry> entries;

        QueryCache(int maxSize, Duration ttl) {
            this.ttl = ttl;
            // accessOrder = true: a read moves the entry to the end, the eldest is evicted first
            this.entries = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                    return size() > maxSize;
                }
            };
        }

        synchronized float[] get(String key) {
            Entry entry = entries.get(key);
            if (entry == null) return null;
            if (entry.expiresAtMillis() < System.currentTimeMillis()) {
                entries.remove(key);
                return null;
            }
            return entry.vector();
        }

        synchronized void put(String key, float[] vector) {
            entries.put(key, new Entry(vector, System.currentTimeMillis() + ttl.toMillis()));
        }
    }
}
