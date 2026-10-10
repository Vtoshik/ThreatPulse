package com.threatpulse.alerts;

/**
 * Outcome of one delivery attempt. The caller decides from this value whether the alert
 * may be recorded as sent, so a notifier must never hide a failure.
 */
public enum NotificationResult {
    /** The channel accepted the message. */
    SENT,
    /** A temporary problem (timeout, rate limit, server error). Trying again later may work. */
    RETRYABLE_FAILURE,
    /** The channel rejected the message for good (bad key, bad address). Retrying will not help. */
    PERMANENT_FAILURE
}
