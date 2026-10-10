package com.threatpulse.feed;

import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import com.threatpulse.common.exception.ResourceNotFoundException;
import com.threatpulse.feed.dto.ThreatPageResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import com.threatpulse.feed.dto.ThreatStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final ThreatResponseMapper threatResponseMapper;

    /**
     * Counts of analyzed threats by severity, for the dashboard. The database does the
     * counting, so the answer stays correct however many threats are stored.
     */
    @Cacheable(value = "threatStats")
    @Transactional(readOnly = true)
    public ThreatStatsResponse getStats() {
        long critical = 0, high = 0, medium = 0, low = 0, info = 0;
        for (Object[] row : threatRepository.countAnalyzedBySeverity()) {
            if (row[0] == null) {
                continue;
            }
            long count = ((Number) row[1]).longValue();
            switch ((Severity) row[0]) {
                case CRITICAL -> critical = count;
                case HIGH -> high = count;
                case MEDIUM -> medium = count;
                case LOW -> low = count;
                case INFO -> info = count;
            }
        }
        long last24h = threatRepository.countByAnalysisStatusAndPublishedAtAfter(
                AnalysisStatus.ANALYZED, OffsetDateTime.now().minusHours(24));
        return new ThreatStatsResponse(critical, high, medium, low, info,
                critical + high + medium + low + info, last24h);
    }

    /**
     * Retrieves a paginated list of threats ordered by collection time (descending).
     *
     * @param page page number
     * @param size page size
     * @return paginated threat response
     */
    public ThreatPageResponse getThreats(int page, int size) {
        return getThreats(page, size, null, null);
    }

    /**
     * Retrieves a paginated list of threats filtered by severity and search query.
     *
     * @param page page number
     * @param size page size
     * @param severity optional severity filter
     * @param query optional search text
     * @return paginated threat response
     */
    @Cacheable(value = "threats", key = "#page + '-' + #size + '-' + #severity + '-' + #query")
    public ThreatPageResponse getThreats(int page, int size, com.threatpulse.common.domain.Severity severity, String query) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Specification<Threat> specification = Specification
                .where(ThreatSpecifications.isAnalyzed())
                .and(ThreatSpecifications.withSeverity(severity))
                .and(ThreatSpecifications.matchesQuery(query));

        Page<Threat> threatPage = threatRepository.findAll(specification, pageable);

        List<ThreatResponse> threats = threatPage.getContent().stream()
                .map(threatResponseMapper::toThreatResponse).toList();

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
    @Transactional(readOnly = true)
    public ThreatResponse getThreatById(Long id) {
        // A threat that is still waiting for its analysis is treated as not existing
        Threat threat = threatRepository.findById(id)
                .filter(found -> found.getAnalysisStatus() == AnalysisStatus.ANALYZED)
                .orElseThrow(() -> new ResourceNotFoundException("Threat not found: " + id));
        return threatResponseMapper.toThreatResponse(threat);
    }
}
