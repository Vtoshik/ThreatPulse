package com.threatpulse.feed;

import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing Threat entities.
 * <p>
 * Extends JpaRepository to provide CRUD operations and defines
 * custom query methods for sorting and filtering threats.
 */
public interface ThreatRepository extends JpaRepository<Threat, Long>, JpaSpecificationExecutor<Threat> {
    @Override
    @EntityGraph(attributePaths = "affectedTechnologies")
    Page<Threat> findAll(Specification<Threat> spec, Pageable pageable);

    Page<Threat> findAllByOrderByCollectedAtDesc(Pageable pageable);
    Page<Threat> findBySeverityOrderByCollectedAtDesc(Severity severity, Pageable pageable);
    boolean existsByExternalId(String externalId);
    Optional<Threat> findByExternalId(String externalId);
    List<Threat> findByAnalyzedAtAfter(OffsetDateTime after);
}
