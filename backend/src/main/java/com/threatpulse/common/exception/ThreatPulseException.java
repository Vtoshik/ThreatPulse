package com.threatpulse.common.exception;

/**
 * Base custom exception for the application.
 * <p>
 * Extends RuntimeException and includes an HTTP status code
 * to standardize error handling across the system.
 */
public class ThreatPulseException extends RuntimeException {
    private final int statusCode;

    public ThreatPulseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
