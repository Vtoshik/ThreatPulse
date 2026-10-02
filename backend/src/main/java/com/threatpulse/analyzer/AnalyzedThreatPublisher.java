package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;

/**
 * Tells the rest of the system that a threat has been analyzed, so alerts and live updates can
 * react to it. How that happens depends on the pipeline mode: with Kafka it is an event,
 * without Kafka nothing is sent and the alert scheduler finds the threat by its analysis time.
 */
public interface AnalyzedThreatPublisher {
    void publish(AnalyzedThreatEvent event);
}
