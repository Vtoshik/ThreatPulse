package com.threatpulse.alerts;

import com.threatpulse.common.domain.Severity;

/**
 * Channel independent content of one alert. A notifier decides how to show it.
 */
public record AlertMessage(
        String title,
        String summary,
        Severity severity,
        String sourceUrl
) {}
