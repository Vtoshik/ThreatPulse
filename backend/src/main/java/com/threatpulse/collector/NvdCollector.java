package com.threatpulse.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.config.KafkaConfig;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class NvdCollector {
    private final KafkaTemplate<String, RawThreatEvent> kafkaTemplate;
    private final ThreatRepository threatRepository;
    private final ObjectMapper objectMapper;

    private static final String NVD_API_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";

    public void collect() {
        log.info("Starting NVD collection");
        RestClient client = RestClient.create();

        try {
            String response = client.get()
                    .uri(NVD_API_URL + "?resultsPerPage=20&startIndex=0")
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode vulnerabilities = rootNode.path("vulnerabilities");

            if (vulnerabilities.isArray()) {
                log.info("Found {} vulnerabilities", vulnerabilities.size());

                for (JsonNode v : vulnerabilities) {
                    JsonNode cve = v.path("cve");
                    String cveId = cve.path("id").asText();

                    // NVD stores description in "descriptions" array, not "description"
                    String description = cve.path("descriptions")
                            .findValuesAsText("value").stream()
                            .findFirst()
                            .orElse("No description");

                    String publishedStr = cve.path("published").asText();
                    OffsetDateTime published = null;
                    if (!publishedStr.isEmpty()) {
                        try {
                            // NVD format: "2024-01-15T10:00:00.000" — no timezone, assume UTC
                            published = java.time.LocalDateTime
                                    .parse(publishedStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    .atOffset(ZoneOffset.UTC);
                        } catch (Exception e) {
                            log.warn("Failed to parse published date: {} for CVE {}", publishedStr, cveId);
                        }
                    }

                    String sourceName = cve.path("sourceIdentifier").asText();
                    String sourceUrl = "";
                    JsonNode references = cve.path("references");
                    if (references.isArray() && !references.isEmpty()) {
                        sourceUrl = references.get(0).path("url").asText();
                    }

                    log.info("CVE: {} | Source: {} | URL: {}", cveId, sourceName, sourceUrl);

                    if (!threatRepository.existsByExternalId(cveId)) {
                        RawThreatEvent event = new RawThreatEvent(
                                cveId, cveId, description, sourceUrl, sourceName, published, "CVE"
                        );
                        kafkaTemplate.send(KafkaConfig.RAW_THREATS_TOPIC, event.externalId(), event);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect NVD data", e);
        }
    }
}
