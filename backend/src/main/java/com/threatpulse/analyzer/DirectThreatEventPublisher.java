package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.collector.ThreatEventPublisher;
import com.threatpulse.collector.dto.RawThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.domain.ThreatCategory;
import com.threatpulse.feed.ThreatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class DirectThreatEventPublisher implements ThreatEventPublisher {
    private final ThreatAnalyzer threatAnalyzer;
    private final ThreatRepository threatRepository;

    @Override
    @CacheEvict(value = "threats", allEntries = true)
    public void publish(RawThreatEvent event) {
        if (threatRepository.existsByExternalId(event.externalId())) {
            log.info("Threat already exists, skipping: {}", event.externalId());
            return;
        }

        log.info("Analyzing threat (direct pipeline): {}", event.externalId());

        AnalyzedThreatEvent analyzed = threatAnalyzer.analyze(event);

        Threat threat = new Threat();
        threat.setExternalId(analyzed.externalId());
        threat.setTitle(analyzed.title());
        threat.setDescription(analyzed.description());
        threat.setAiSummary(analyzed.aiSummary());
        threat.setSourceUrl(analyzed.sourceUrl());
        threat.setSourceName(analyzed.sourceName());
        threat.setPublishedAt(analyzed.publishedAt());
        threat.setCollectedAt(OffsetDateTime.now());
        threat.setAnalyzedAt(OffsetDateTime.now());
        threat.setAffectedTechnologies(new HashSet<>(analyzed.affectedTechnologies()));

        Severity severity;
        try {
            severity = Severity.valueOf(analyzed.severity());
        } catch (Exception e) {
            log.error("Failed to cast severity: {}", analyzed.severity(), e);
            severity = Severity.INFO;
        }

        ThreatCategory category;
        try {
            category = ThreatCategory.valueOf(analyzed.category());
        } catch (Exception e) {
            log.error("Failed to cast category: {}", analyzed.category(), e);
            category = ThreatCategory.OTHER;
        }

        threat.setSeverity(severity);
        threat.setThreatCategory(category);
        threatRepository.save(threat);

        log.info("Threat saved: {}", analyzed.externalId());
    }
}