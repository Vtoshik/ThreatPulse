package com.threatpulse.alerts;

import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.ThreatRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.pipeline.kafka-enabled", havingValue = "false")
public class AlertScheduler {

    private final AlertService alertService;
    private final ThreatRepository threatRepository;

    @Transactional
    @Scheduled(initialDelay = 60_000, fixedDelay = 10 * 60 * 1000)
    public void checkRecentThreats() {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        List<Threat> recent = threatRepository.findByAnalyzedAtAfter(since);
        if (recent.isEmpty()) return;
        log.info("AlertScheduler: checking {} recent threats against alert rules", recent.size());
        for (Threat threat : recent) {
            alertService.checkAndNotifyForThreat(threat);
        }
    }
}