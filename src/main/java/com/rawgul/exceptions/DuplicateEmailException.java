package com.rawgul.exceptions;

/**
 * Exception thrown when attempting to register or update a user with an email that already exists.
 * 
 * This exception should be thrown when:
 * - User registration with existing email
 * - User profile update with email already taken by another user
 * - Email uniqueness constraint violation
 * 
 * Results in HTTP 409 Conflict response.
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
public class DuplicateEmailException extends RuntimeException {

    private final String email;

    /**
     * Constructs a new DuplicateEmailException with a default message.
     */
    public DuplicateEmailException() {
        super("Email address is already in use");
        this.email = null;
    }

    /**
     * Constructs a new DuplicateEmailException with the specified email.
     * 
     * @param email the duplicate email address
     */
    public DuplicateEmailException(String email) {
        super("Email address is already in use: " + email);
        this.email = email;
    }

    /**
     * Constructs a new DuplicateEmailException with the specified detail message.
     * 
     * @param email the duplicate email address
     * @param message the detail message
     */
    public DuplicateEmailException(String email, String message) {
        super(message);
        this.email = email;
    }

    /**
     * Constructs a new DuplicateEmailException with the specified detail message and cause.
     * 
     * @param email the duplicate email address
     * @param message the detail message
     * @param cause the cause
     */
    public DuplicateEmailException(String email, String message, Throwable cause) {
        super(message, cause);
        this.email = email;
    }

    /**
     * Gets the duplicate email address.
     * 
     * @return the email address, or null if not provided
     */
    public String getEmail() {
        return email;
    }

}
