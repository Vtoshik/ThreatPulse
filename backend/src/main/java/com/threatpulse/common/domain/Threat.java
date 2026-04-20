package com.threatpulse.common.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a security threat or vulnerability.
 * <p>
 * Stores detailed information about detected threats, including metadata such as
 * title, description, severity, category, source, and timestamps.
 * <p>
 * This entity supports auditing for creation and modification tracking and
 * maintains a collection of affected technologies related to the threat.
 * <p>
 * Mapped to the "threats" table in the database.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "threats")
@EntityListeners(AuditingEntityListener.class)
public class Threat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique identifier from an external source (e.g., CVE, API, feed)
    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    // AI-generated summary of the threat (optional)
    @Column(name = "ai_summary")
    private String aiSummary;

    // Severity level of the threat (e.g., CRITICAL, HIGH)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    // Category of the threat (e.g., XSS, SQL Injection)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category")
    private ThreatCategory threatCategory;

    // URL of the source where the threat was discovered
    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    // Name of the source (e.g., NVD, blog, security feed)
    @Column(name = "source_name", nullable = false)
    private String sourceName;

    // When the threat was originally published
    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    // When the threat was collected by the system
    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt;

    // When the threat was analyzed (e.g., by AI or system)
    @Column(name = "analyzed_at")
    private OffsetDateTime analyzedAt;

    // List of affected technologies (stored in a separate collection table)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "threat_technologies",
            joinColumns = @JoinColumn(name = "threat_id")
    )
    @Column(name = "technology_name")
    private Set<String> affectedTechnologies = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
