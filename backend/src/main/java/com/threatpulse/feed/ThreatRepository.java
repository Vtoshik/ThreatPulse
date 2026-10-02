package com.threatpulse.feed;

import com.threatpulse.common.domain.AnalysisStatus;
import com.threatpulse.common.domain.Severity;
import com.threatpulse.common.domain.Threat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing Threat entities.
 * <p>
 * Extends JpaRepository to provide CRUD operations and defines
 * custom query methods for sorting and filtering threats.
 */
public interface ThreatRepository extends JpaRepository<Threat, Long>,
        JpaSpecificationExecutor<Threat> {
    @Override
    @EntityGraph(attributePaths = "affectedTechnologies")
    Page<Threat> findAll(Specification<Threat> spec, Pageable pageable);

    Page<Threat> findAllByOrderByCollectedAtDesc(Pageable pageable);
    Page<Threat> findBySeverityOrderByCollectedAtDesc(Severity severity,
                                                      Pageable pageable);
    boolean existsByExternalId(String externalId);
    Optional<Threat> findByExternalId(String externalId);
    // Only threats whose analysis finished can raise alerts
    List<Threat> findByAnalysisStatusAndAnalyzedAtAfter(AnalysisStatus status,
                                                        OffsetDateTime after);
    boolean existsByIdAndAnalysisStatus(Long id, AnalysisStatus status);

    /**
     * Threats waiting for their analysis, the ones tried least recently first (never tried
     * comes before everything else). A threat that keeps failing moves to the back of the
     * queue after each attempt, so it cannot block the others.
     */
    @Query("""
            SELECT t FROM Threat t
            WHERE t.analysisStatus = com.threatpulse.common.domain.AnalysisStatus.PENDING_ANALYSIS
            ORDER BY t.analysisAttemptedAt ASC NULLS FIRST, t.collectedAt ASC
            """)
    List<Threat> findPendingForAnalysis(Pageable pageable);

    /** Analyzed threats that have no embedding yet, newest first so recent news is searchable first. */
    @Query("""
            SELECT t FROM Threat t
            WHERE t.analysisStatus = com.threatpulse.common.domain.AnalysisStatus.ANALYZED
              AND t.embedding IS NULL
            ORDER BY t.collectedAt DESC
            """)
    List<Threat> findAnalyzedWithoutEmbedding(Pageable pageable);

    @Query(value = """
                SELECT * FROM threats
                WHERE embedding IS NOT NULL
                    AND analysis_status = 'ANALYZED'
                    AND (:severity IS NULL OR severity = CAST(:severity AS severity_level_enum))
                ORDER BY embedding <=> CAST(:queryVector AS vector)
                LIMIT :limit
                """, nativeQuery = true)
    List<Threat> findNearest(@Param("queryVector") String queryVector,
                             @Nullable @Param("severity") String severity,
                             @Param("limit") int limit);
}
