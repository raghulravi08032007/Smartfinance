package com.rawgul.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validation error response DTO for field-specific validation errors.
 * 
 * This response is returned when request validation fails (@Valid annotation).
 * Provides detailed field-level error messages for better client-side handling.
 * 
 * Example:
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Request validation failed",
 *   "timestamp": "2025-11-03T10:30:00",
 *   "fieldErrors": [
 *     {
 *       "field": "email",
 *       "message": "Email is required",
 *       "rejectedValue": null
 *     },
 *     {
 *       "field": "password",
 *       "message": "Password must be at least 8 characters",
 *       "rejectedValue": "123"
 *     }
 *   ]
 * }
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {

    /**
     * HTTP status code (always 400 for validation errors)
     */
    private int status;

    /**
     * HTTP status text (e.g., "Validation Failed")
     */
    private String error;

    /**
     * General error message
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
     * List of field-specific validation errors
     */
    @Builder.Default
    private List<FieldError> fieldErrors = new ArrayList<>();

    /**
     * Field-specific validation error details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        
        /**
         * Field name that failed validation
         */
        private String field;

        /**
         * Validation error message for this field
         */
        private String message;

        /**
         * The rejected value (optional, can be null or sensitive)
         */
        private Object rejectedValue;

        /**
         * Error code for programmatic handling (optional)
         */
        private String code;

        /**
         * Simple constructor with field and message.
         */
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }

    /**
     * Adds a field error to the list.
     * 
     * @param field field name
     * @param message error message
     */
    public void addFieldError(String field, String message) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new ArrayList<>();
        }
        this.fieldErrors.add(new FieldError(field, message));
    }

    /**
     * Adds a field error with rejected value.
     * 
     * @param field field name
     * @param message error message
     * @param rejectedValue the rejected value
     */
    public void addFieldError(String field, String message, Object rejectedValue) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new ArrayList<>();
        }
        this.fieldErrors.add(new FieldError(field, message, rejectedValue, null));
    }

    /**
     * Creates a simple map of field errors (field -> message).
     * Useful for backwards compatibility.
     * 
     * @return map of field errors
     */
    public Map<String, String> getSimpleFieldErrors() {
        Map<String, String> errors = new java.util.HashMap<>();
        if (fieldErrors != null) {
            fieldErrors.forEach(error -> errors.put(error.getField(), error.getMessage()));
        }
        return errors;
    }

}
