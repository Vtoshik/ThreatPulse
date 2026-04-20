package com.threatpulse.feed;

import com.threatpulse.feed.dto.ThreatPageResponse;
import com.threatpulse.feed.dto.ThreatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for retrieving threat data.
 * <p>
 * Provides access to:
 * - Paginated list of threats
 * - Individual threat details by ID
 */
@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    /**
     * Retrieves a paginated list of threats.
     *
     * @param page page number (default: 0)
     * @param size number of items per page (default: 20)
     * @return paginated threat response
     */
    @GetMapping
    public ResponseEntity<ThreatPageResponse> getThreats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(feedService.getThreats(page, size));
    }

    /**
     * Retrieves a single threat by its ID.
     *
     * @param id the threat identifier
     * @return threat details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ThreatResponse> getThreatById(@PathVariable Long id) {
        return ResponseEntity.ok(feedService.getThreatById(id));
    }
}
