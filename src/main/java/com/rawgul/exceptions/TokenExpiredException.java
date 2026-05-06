package com.rawgul.exceptions;

import java.util.Date;

/**
 * Exception thrown when a JWT token or reset token has expired.
 * 
 * This exception should be thrown when:
 * - JWT access token has expired
 * - JWT refresh token has expired
 * - Password reset token has expired
 * - Email verification token has expired
 * 
 * Results in HTTP 401 Unauthorized response.
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
public class TokenExpiredException extends RuntimeException {

    private final Date expiredAt;

    /**
     * Constructs a new TokenExpiredException with a default message.
     */
    public TokenExpiredException() {
        super("Token has expired");
        this.expiredAt = null;
    }

    /**
     * Constructs a new TokenExpiredException with the specified detail message.
     * 
     * @param message the detail message
     */
    public TokenExpiredException(String message) {
        super(message);
        this.expiredAt = null;
    }

    /**
     * Constructs a new TokenExpiredException with the specified detail message and expiration date.
     * 
     * @param message the detail message
     * @param expiredAt the date when the token expired
     */
    public TokenExpiredException(String message, Date expiredAt) {
        super(message);
        this.expiredAt = expiredAt;
    }

    /**
     * Constructs a new TokenExpiredException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
        this.expiredAt = null;
    }

    /**
     * Gets the expiration date of the token.
     * 
     * @return the expiration date, or null if not provided
     */
    public Date getExpiredAt() {
        return expiredAt;
    }

    /**
     * Constructs a TokenExpiredException for JWT access token.
     * 
     * @return TokenExpiredException with formatted message
     */
    public static TokenExpiredException jwtAccessToken() {
        return new TokenExpiredException("JWT access token has expired. Please refresh your token or log in again.");
    }

    /**
     * Constructs a TokenExpiredException for JWT refresh token.
     * 
     * @return TokenExpiredException with formatted message
     */
    public static TokenExpiredException jwtRefreshToken() {
        return new TokenExpiredException("JWT refresh token has expired. Please log in again.");
    }

    /**
     * Constructs a TokenExpiredException for password reset token.
     * 
     * @return TokenExpiredException with formatted message
     */
    public static TokenExpiredException passwordResetToken() {
        return new TokenExpiredException("Password reset token has expired. Please request a new one.");
    }

}
