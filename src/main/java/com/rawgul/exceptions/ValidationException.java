package com.rawgul.exceptions;

/**
 * Exception thrown when business validation fails.
 * 
 * @author Raghul
 * @since 1.0
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
