package com.rawgul.service;

import com.rawgul.dto.PasswordResetConfirmRequest;
import com.rawgul.dto.PasswordResetRequest;
import com.rawgul.exceptions.ResourceNotFoundException;
import com.rawgul.model.PasswordResetToken;
import com.rawgul.model.User;
import com.rawgul.repository.PasswordResetTokenRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // private final EmailService emailService; // To be implemented

    /**
     * Create a password reset token and send email to user.
     * Implements rate limiting to prevent abuse.
     * 
     * @param request the password reset request
     * @param ipAddress the client IP address
     * @param userAgent the client user agent
     * @return Created password reset token
     * @throws ResourceNotFoundException if email not found
     * @throws com.rawgul.exceptions.RateLimitExceededException if too many requests
     */
    public PasswordResetToken initiatePasswordReset(PasswordResetRequest request, String ipAddress, String userAgent) {
        log.info("Password reset initiated for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with that email address"));

        // Check if user has too many recent reset requests (prevent abuse)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long recentTokenCount = tokenRepository.countRecentTokensByUser(user.getId(), oneHourAgo);
        
        if (recentTokenCount >= 3) {
            log.warn("Rate limit exceeded for password reset: {}", request.getEmail());
            throw new com.rawgul.exceptions.RateLimitExceededException(
                "Too many password reset requests. Please try again after 1 hour.");
        }

        // Invalidate any existing unused tokens for this user
        invalidateExistingTokens(user);

        // Create new token
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        token = tokenRepository.save(token);
        
        log.info("Password reset token created for user: {} with token: {}", user.getEmail(), token.getToken());

        // TODO: Send email with reset link
        // String resetLink = "http://your-domain.com/reset-password?token=" + token.getToken();
        // emailService.sendPasswordResetEmail(user.getEmail(), resetLink, token.getMinutesUntilExpiry());
        
        return token;
    }

    /**
     * Validate token and reset password.
     * 
     * @param request the password reset confirmation request
     * @throws com.rawgul.exceptions.ValidationException if passwords don't match
     * @throws com.rawgul.exceptions.InvalidTokenException if token is invalid or expired
     * @throws ResourceNotFoundException if token not found
     */
    public void resetPassword(PasswordResetConfirmRequest request) {
        log.info("Password reset attempt with token: {}", request.getToken());
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new com.rawgul.exceptions.ValidationException("Passwords do not match");
        }
        
        // Validate password strength
        validatePasswordStrength(request.getNewPassword());

        // Find and validate token
        PasswordResetToken token = tokenRepository.findValidToken(request.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new com.rawgul.exceptions.InvalidTokenException(
                    "Invalid or expired password reset token"));

        if (token.isUsed()) {
            log.warn("Attempt to reuse password reset token: {}", request.getToken());
            throw new com.rawgul.exceptions.InvalidTokenException(
                "This password reset token has already been used");
        }

        if (token.isExpired()) {
            log.warn("Attempt to use expired password reset token: {}", request.getToken());
            throw new com.rawgul.exceptions.InvalidTokenException(
                "This password reset token has expired. Please request a new one.");
        }

        // Update user password
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.resetFailedLoginAttempts(); // Reset any lockout
        user.setAccountLocked(false); // Unlock account if locked
        userRepository.save(user);

        // Mark token as used
        token.markAsUsed();
        tokenRepository.save(token);

        log.info("Password successfully reset for user: {}", user.getEmail());

        // TODO: Send confirmation email
        // emailService.sendPasswordChangedEmail(user.getEmail());
    }
    
    /**
     * Validate password strength requirements.
     * 
     * @param password the password to validate
     * @throws com.rawgul.exceptions.ValidationException if password doesn't meet requirements
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new com.rawgul.exceptions.ValidationException(
                "Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new com.rawgul.exceptions.ValidationException(
                "Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new com.rawgul.exceptions.ValidationException(
                "Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new com.rawgul.exceptions.ValidationException(
                "Password must contain at least one digit");
        }
    }

    /**
     * Validate if a token is valid
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String tokenString) {
        return tokenRepository.findValidToken(tokenString, LocalDateTime.now()).isPresent();
    }

    /**
     * Get token details for validation
     */
    @Transactional(readOnly = true)
    public PasswordResetToken getTokenByValue(String tokenString) {
        return tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
    }

    /**
     * Invalidate all existing tokens for a user
     */
    private void invalidateExistingTokens(User user) {
        List<PasswordResetToken> existingTokens = tokenRepository.findByUser(user);
        existingTokens.stream()
                .filter(token -> !token.isUsed() && !token.isExpired())
                .forEach(token -> {
                    token.markAsUsed();
                    tokenRepository.save(token);
                });
    }

    /**
     * Clean up expired tokens (should be run periodically via scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7); // Delete tokens older than 7 days
        List<PasswordResetToken> expiredTokens = tokenRepository.findByExpiryDateBefore(cutoffDate);
        
        if (!expiredTokens.isEmpty()) {
            tokenRepository.deleteAll(expiredTokens);
            log.info("Cleaned up {} expired password reset tokens", expiredTokens.size());
        }
    }

    /**
     * Get all tokens for a user (admin function)
     */
    @Transactional(readOnly = true)
    public List<PasswordResetToken> getTokensByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return tokenRepository.findByUser(user);
    }

}
