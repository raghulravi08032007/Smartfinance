package com.rawgul.exceptions;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 * 
 * This exception should be thrown when:
 * - Username/password combination is incorrect
 * - Login attempt with invalid credentials
 * - Authentication fails during login
 * 
 * Results in HTTP 401 Unauthorized response.
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Constructs a new InvalidCredentialsException with a default message.
     */
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }

    /**
     * Constructs a new InvalidCredentialsException with the specified detail message.
     * 
     * @param message the detail message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidCredentialsException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

}
