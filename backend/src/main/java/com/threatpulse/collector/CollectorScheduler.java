package com.threatpulse.collector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
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
