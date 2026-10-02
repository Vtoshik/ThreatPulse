package com.threatpulse.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the re-analysis regularly. It works in both pipeline modes: with Kafka the analyzed
 * threats are announced through the publisher, without Kafka the alert scheduler picks them up.
 * Can be switched off with app.reanalysis.enabled=false (the test profile does this).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.reanalysis.enabled", havingValue = "true", matchIfMissing = true)
public class ThreatReanalysisScheduler {
    private final ThreatReanalysisService threatReanalysisService;
    private final int batchSize;

    public ThreatReanalysisScheduler(
            ThreatReanalysisService threatReanalysisService,
            @Value("${app.reanalysis.batch-size:20}") int batchSize
    ) {
        this.threatReanalysisService = threatReanalysisService;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${app.reanalysis.initial-delay-ms:60000}",
            fixedDelayString = "${app.reanalysis.interval-ms:300000}"
    )
    public void run() {
        int analyzed = threatReanalysisService.analyzePending(batchSize);
        int embedded = threatReanalysisService.backfillEmbeddings(batchSize);

        if (analyzed > 0 || embedded > 0) {
            log.info("Re-analysis finished: {} threats analyzed, {} embeddings created",
                    analyzed, embedded);
        }
    }
}
