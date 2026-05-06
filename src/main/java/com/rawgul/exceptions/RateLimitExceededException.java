package com.rawgul.exceptions;

/**
 * Exception thrown when rate limit is exceeded for an operation.
 * 
 * @author Raghul
 * @since 1.0
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

}
