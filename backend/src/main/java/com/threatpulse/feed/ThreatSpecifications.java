package com.threatpulse.feed;

import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.SetJoin;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Reusable JPA specifications for threat filtering and search.
 */
final class ThreatSpecifications {
    private ThreatSpecifications() {
    }

    static Specification<Threat> withSeverity(Severity severity) {
        return (root, query, cb) -> severity == null ? cb.conjunction() : cb.equal(root.get("severity"), severity);
    }

    static Specification<Threat> matchesQuery(String queryText) {
        return (root, query, cb) -> {
            if (queryText == null || queryText.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + queryText.toLowerCase(Locale.ROOT) + "%";
            SetJoin<Threat, String> technologies = root.joinSet("affectedTechnologies", JoinType.LEFT);

            query.distinct(true);

            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("externalId")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("aiSummary"), "")), pattern),
                    cb.like(cb.lower(root.get("sourceName")), pattern),
                    cb.like(cb.lower(root.get("sourceUrl")), pattern),
                    cb.like(cb.lower(technologies), pattern)
            );
        };
    }
}
