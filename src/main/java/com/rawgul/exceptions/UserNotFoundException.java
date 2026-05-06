package com.rawgul.exceptions;

/**
 * Exception thrown when a user is not found in the database.
 * 
 * This exception should be thrown when:
 * - User lookup by ID fails
 * - User lookup by username fails
 * - User lookup by email fails
 * 
 * Results in HTTP 404 Not Found response.
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new UserNotFoundException with the specified detail message.
     * 
     * @param message the detail message
     */
    public UserNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new UserNotFoundException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a UserNotFoundException with a formatted message for user ID.
     * 
     * @param userId the user ID that was not found
     * @return UserNotFoundException with formatted message
     */
    public static UserNotFoundException byId(Long userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    /**
     * Constructs a UserNotFoundException with a formatted message for username.
     * 
     * @param username the username that was not found
     * @return UserNotFoundException with formatted message
     */
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username);
    }

    /**
     * Constructs a UserNotFoundException with a formatted message for email.
     * 
     * @param email the email that was not found
     * @return UserNotFoundException with formatted message
     */
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

}
