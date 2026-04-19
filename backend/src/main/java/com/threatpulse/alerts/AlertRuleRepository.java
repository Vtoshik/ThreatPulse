package com.threatpulse.alerts;

import com.threatpulse.common.domain.Severity;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    @Query(value = "SELECT * FROM alert_rules r WHERE r.active = true AND array_position(ARRAY['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'], r.min_severity::text) >= array_position(ARRAY['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'], :severity) AND (r.technologies_filter IS NULL or r.technologies_filter && CAST(:technologies as text[]))", nativeQuery = true)
    List<AlertRule> findMatchingRules(@Param("severity") String severity, @Param("technologies") String[] technologiesFilter);
}
