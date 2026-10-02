package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.analyzer.dto.ThreatAnalysis;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Threat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Turns a raw threat into an analyzed one with the help of the AI.
 * <p>
 * When the AI cannot give a usable answer the result is empty. The caller decides what to do,
 * and must not invent a severity: a made-up "INFO" looks like a real analysis.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatAnalyzer {
    private final GroqAiClient groqAiClient;

    /**
     * @return the analyzed threat, or empty if the AI analysis is not available
     */
    public Optional<AnalyzedThreatEvent> analyze(RawThreatEvent raw) {
        try {
            ThreatAnalysis aiResponse = groqAiClient.analyze(raw.title(), raw.description());

            if (aiResponse == null) {
                log.warn("AI analysis not available for {}", raw.externalId());
                return Optional.empty();
            }

            // The prompt asks for a summary every time, so no summary means a broken answer
            if (aiResponse.getSummary() == null || aiResponse.getSummary().isBlank()) {
                log.warn("AI answer has no summary for {}", raw.externalId());
                return Optional.empty();
            }

            // The model may leave the list out, which must not break the mapping later
            List<String> affectedTechnologies = aiResponse.getAffectedTechnologies() == null
                    ? List.of()
                    : aiResponse.getAffectedTechnologies();

            return Optional.of(new AnalyzedThreatEvent(
                    raw.externalId(), raw.title(), raw.description(),
                    aiResponse.getSummary(), aiResponse.getSeverity(), aiResponse.getCategory(),
                    affectedTechnologies, aiResponse.getRecommendedAction(),
                    raw.sourceUrl(), raw.sourceName(), raw.publishedAt()
            ));
        } catch (Exception e) {
            log.error("Failed to get aiResponse response for {}", raw.externalId(), e);
            return Optional.empty();
        }
    }

    /**
     * Analyzes a threat that is already stored, to retry an analysis that failed earlier.
     */
    public Optional<AnalyzedThreatEvent> analyze(Threat threat) {
        return analyze(new RawThreatEvent(
                threat.getExternalId(), threat.getTitle(), threat.getDescription(),
                threat.getSourceUrl(), threat.getSourceName(), threat.getPublishedAt(),
                "RESCAN"
        ));
    }
}
