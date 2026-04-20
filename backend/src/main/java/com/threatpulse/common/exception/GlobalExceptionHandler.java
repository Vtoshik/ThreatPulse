package com.threatpulse.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Centralizes exception handling across the application and ensures
 * consistent error responses for different types of exceptions.
 * <p>
 * Handles:
 * - Custom application exceptions (ThreatPulseException)
 * - Validation errors (MethodArgumentNotValidException)
 * - Generic unhandled exceptions
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Handles custom application exceptions.
     *
     * @param ex the thrown ThreatPulseException
     * @return standardized error response with custom status code
     */
    @ExceptionHandler(ThreatPulseException.class)
    public ResponseEntity<ErrorResponse> handleThreatPulseException(ThreatPulseException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(ex.getMessage(), ex.getStatusCode()));
    }

    /**
     * Handles validation errors triggered by @Valid annotations.
     * <p>
     * Aggregates field-level validation messages into a single response.
     *
     * @param ex the validation exception
     * @return error response with HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(new ErrorResponse(message, 400));
    }

    /**
     * Handles all uncaught exceptions.
     * <p>
     * Prevents internal errors from leaking sensitive details to clients.
     *
     * @param ex the exception
     * @return generic error response with HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(500).body(new ErrorResponse("Internal server error", 500));
    }
}
