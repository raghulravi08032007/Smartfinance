package com.rawgul.exceptions;

/**
 * Exception thrown when a user account is locked due to security reasons.
 * 
 * @author Raghul
 * @since 1.0
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }

}
