package com.threatpulse.common.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 * <p>
 * Returns HTTP 404 status code.
 */
public class ResourceNotFoundException extends ThreatPulseException {
    public ResourceNotFoundException(String message) {
        super(message, 404);
    }
}
