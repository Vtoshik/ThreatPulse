package com.threatpulse.collector;

import com.threatpulse.collector.dto.RawThreatEvent;

public interface ThreatEventPublisher {
    void publish(RawThreatEvent event);
}