package com.threatpulse.common.exception;

/**
 * Standard error response model returned by the API.
 * <p>
 * Encapsulates error details including a human-readable message
 * and the corresponding HTTP status code.
 */
public record ErrorResponse(String message, int status) {}
