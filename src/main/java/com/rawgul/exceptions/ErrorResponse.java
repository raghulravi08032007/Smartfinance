package com.rawgul.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for REST API error handling.
 * 
 * This response is returned for all exceptions handled by GlobalExceptionHandler.
 * Provides consistent error structure across the application.
 * 
 * Fields:
 * - status: HTTP status code (e.g., 400, 404, 500)
 * - error: HTTP status text (e.g., "Bad Request", "Not Found")
 * - message: Human-readable error message
 * - timestamp: When the error occurred
 * - path: Request path that caused the error (optional)
 * - details: Additional error details (optional)
 * 
 * @author Raghul
 * @version 2.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code (e.g., 400, 404, 500)
     */
    private int status;

    /**
     * HTTP status text (e.g., "Bad Request", "Not Found", "Internal Server Error")
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;

    /**
     * Request path that caused the error (optional)
     */
    private String path;

    /**
     * Additional error details (optional)
     * Useful for providing extra context or debugging information
     */
    private Map<String, Object> details;

    /**
     * Simple constructor for basic error responses.
     * 
     * @param status HTTP status code
     * @param message error message
     * @param timestamp when error occurred
     */
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    /**
     * Constructor with error type.
     * 
     * @param status HTTP status code
     * @param error HTTP status text
     * @param message error message
     * @param timestamp when error occurred
     */
    public ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

}
