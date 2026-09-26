package com.threatpulse.analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@Slf4j
public class EmbeddingService {
    private final RestClient restClient;
    private final String model;
    private final String apiKey;
    private static final int EMBED_LENGTH = 384;
    private static final String QUERY_PREFIX =
            "Represent this sentence for searching relevant passages: ";
    private static final int MAX_DOCUMENT_CHARS = 1000;

    public EmbeddingService(
        @Value("${app.huggingface.api-key}") String apiKey,
        @Value("${app.huggingface.model}") String model,
        @Value("${app.huggingface.base-url}") String baseUrl,
        RestClient.Builder restClientBuilder
    ){
        this.apiKey = apiKey;
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

    private float[] embed(String text){
        if (isNullOrBlank(apiKey) || isNullOrBlank(text)) return null;
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
            return null;
        } catch (Exception e) {
            log.error("Hugging Face call failed for text", e);
            return null;
        }

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

    public float[] embedDocument(String text) {
        if (isNullOrBlank(text)) return null;
        text = text.substring(0, Math.min(text.length(), MAX_DOCUMENT_CHARS));
        return embed(text);
    }

    public float[] embedQuery(String text) {
        if (isNullOrBlank(text)) return null;
        return embed(QUERY_PREFIX + text);
    }

}
