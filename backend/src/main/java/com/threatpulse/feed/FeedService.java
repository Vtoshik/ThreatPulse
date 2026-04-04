package com.threatpulse.feed;

import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.exception.ResourceNotFoundException;
import com.threatpulse.feed.dto.ThreatPageResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for retrieving and transforming threat data.
 * <p>
 * Handles pagination, sorting, and mapping of Threat entities
 * to DTOs used in API responses.
 */
@Service
@RequiredArgsConstructor
public class FeedService {
    private final ThreatRepository threatRepository;

    /**
     * Retrieves a paginated list of threats ordered by collection time (descending).
     *
     * @param page page number
     * @param size page size
     * @return paginated threat response
     */
    public ThreatPageResponse getThreats(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Threat> threatPage = threatRepository.findAllByOrderByCollectedAtDesc(pageable);

        List<ThreatResponse> threats = threatPage.getContent().stream()
                .map(this::toResponse).toList();

        return new ThreatPageResponse(
                threats,
                threatPage.getNumber(),
                threatPage.getSize(),
                threatPage.getTotalElements(),
                threatPage.getTotalPages()
        );
    }

    /**
     * Retrieves a threat by ID.
     *
     * @param id the threat identifier
     * @return mapped ThreatResponse
     * @throws ResourceNotFoundException if the threat does not exist
     */
    public ThreatResponse getThreatById(Long id) {
        Threat threat = threatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threat not found: " + id));
        return toResponse(threat);
    }

    /**
     * Maps a Threat entity to a ThreatResponse DTO.
     *
     * @param threat the entity to map
     * @return DTO representation of the threat
     */
    private ThreatResponse toResponse(Threat threat) {
        return new ThreatResponse(
                threat.getId(),
                threat.getExternalId(),
                threat.getTitle(),
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
