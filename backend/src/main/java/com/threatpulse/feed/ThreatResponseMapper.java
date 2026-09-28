package com.threatpulse.feed;

import com.threatpulse.common.domain.Threat;
import com.threatpulse.feed.dto.ThreatResponse;
import org.springframework.stereotype.Component;

@Component
public class ThreatResponseMapper {

    /**
     * Maps a Threat entity to a ThreatResponse DTO.
     *
     * @param threat the entity to map
     * @return DTO representation of the threat
     */
    public ThreatResponse toThreatResponse(Threat threat) {
        return new ThreatResponse(
                threat.getId(),
                threat.getExternalId(),
                threat.getTitle(),
                threat.getDescription(),
                threat.getAiSummary(),
                threat.getSeverity(),
                threat.getThreatCategory(),
                threat.getSourceName(),
                threat.getSourceUrl(),
                threat.getPublishedAt(),
                threat.getAffectedTechnologies()
        );
    }
}
