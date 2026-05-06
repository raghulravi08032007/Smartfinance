package com.rawgul.exceptions;

/**
 * Exception thrown when a token (JWT or password reset) is invalid.
 * 
 * @author Raghul
 * @since 1.0
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
