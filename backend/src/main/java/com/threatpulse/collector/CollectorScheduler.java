package com.threatpulse.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs all collectors on a schedule, and once right after startup.
 * <p>
 * Can be switched off with app.collector.enabled=false. The test profile does this,
 * so tests do not call the real NVD and RSS servers and do not push background
 * traffic through the pipeline while a test is running.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.collector.enabled", havingValue = "true", matchIfMissing = true)
public class CollectorScheduler {
    private final NvdCollector nvdCollector;
    private final RssCollector rssCollector;

    @Scheduled(fixedDelay = 2 * 60 * 60 * 1000) // every 2 hours in ms
    public void runAll() {
        log.info("Running all collectors");
        nvdCollector.collect();
        rssCollector.collect();
        log.info("All collectors finished");
    }
}
