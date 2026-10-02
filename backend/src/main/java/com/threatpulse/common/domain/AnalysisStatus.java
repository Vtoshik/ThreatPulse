package com.threatpulse.common.domain;

/**
 * Whether the AI analysis of a threat has finished.
 * <p>
 * A threat whose analysis failed (rate limit, outage, daily limit) is still saved,
 * because its title, link and text are real, but it stays hidden from users and alerts
 * until a later attempt succeeds. This way a failed analysis never shows up as a
 * made-up "INFO" threat.
 */
public enum AnalysisStatus {
    ANALYZED,         // analysis finished, the threat is visible to users and alerts
    PENDING_ANALYSIS  // analysis failed or has not run yet, the threat is hidden
}
