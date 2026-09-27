package com.threatpulse.analyzer;

import com.threatpulse.analyzer.dto.AnalyzedThreatEvent;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ThreatMapperTest {
    private final ThreatMapper threatMapper = new ThreatMapper();

    @Test
    public void toThreat_shouldFallBackToInfo_whenSeverityIsInvalid() {
        AnalyzedThreatEvent badSeverityEvent = new
                AnalyzedThreatEvent(
                "ext-3", "title", "desc", "summary",
                "UNKNOWN_SEVERITY", "OTHER", List.of(),
                "action", "https://example.com", "NVD",
                OffsetDateTime.now()
        );

        Threat threat = threatMapper.toThreat(badSeverityEvent);

        assertThat(threat.getSeverity()).isEqualTo(Severity.INFO);
    }
}