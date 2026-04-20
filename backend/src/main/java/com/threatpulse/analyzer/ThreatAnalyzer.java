package com.threatpulse.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.collector.dto.RawThreatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatAnalyzer {
    private final GroqAiClient groqAiClient;

    public AnalyzedThreatEvent analyze(RawThreatEvent raw) {
        try {
            ThreatAnalysis aiResponse = groqAiClient.analyze(raw.title(), raw.description());

            if (aiResponse == null) {
                // AI failed - return event with defaults so pipeline continues
                return new AnalyzedThreatEvent(
                        raw.externalId(), raw.title(), raw.description(),
                        null, "INFO", "OTHER", List.of(), null,
                        raw.sourceUrl(), raw.sourceName(), raw.publishedAt()
                );
            }

            String summary = aiResponse.getSummary();
            String severity = aiResponse.getSeverity();
            String category = aiResponse.getCategory();
            String action = aiResponse.getRecommendedAction();
            List<String> affectedTechnologies = aiResponse.getAffectedTechnologies();

            return new AnalyzedThreatEvent(
                    raw.externalId(), raw.title(), raw.description(),
                    summary, severity, category, affectedTechnologies, action,
                    raw.sourceUrl(), raw.sourceName(), raw.publishedAt()
            );
        } catch (Exception e) {
            log.error("Failed to get aiResponse response for {}", raw.externalId(), e);
            return new AnalyzedThreatEvent(
                    raw.externalId(), raw.title(), raw.description(),
                    null, "INFO", "OTHER", List.of(), null,
                    raw.sourceUrl(), raw.sourceName(), raw.publishedAt()
            );
        }
    }
}
