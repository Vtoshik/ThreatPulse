package com.threatpulse.alerts;

/**
 * An optional outgoing channel for alerts, for example email.
 * <p>
 * The in-app alert (history row plus live event) is always created and needs no notifier.
 * A notifier only adds another place where the alert shows up.
 */
public interface Notifier {
    /** Name written to the alert history, for example "EMAIL". */
    String channel();

    /**
     * Tries to deliver the message. Implementations must not throw for delivery problems,
     * they report them through the result.
     */
    NotificationResult send(AlertMessage message);
}
