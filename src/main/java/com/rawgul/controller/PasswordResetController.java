package com.rawgul.controller;

import com.rawgul.dto.MessageResponse;
import com.rawgul.dto.PasswordResetConfirmRequest;
import com.rawgul.dto.PasswordResetRequest;
import com.rawgul.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Password Reset Controller.
 * Handles password reset operations and maps to frontend forgot-password.html form.
 * 
 * @author Raghul
 * @since 1.0
 */
@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Request password reset.
     * Initiates password reset by sending email with reset link.
     * 
     * POST /api/password-reset/request
     * 
     * @param request the password reset request with email
     * @param httpRequest the HTTP servlet request
     * @return MessageResponse with success message
     */
    @PostMapping("/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        passwordResetService.initiatePasswordReset(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(new MessageResponse(
            "If an account exists with that email, a password reset link has been sent. " +
            "Please check your email inbox."
        ));
    }
    
    /**
     * Legacy endpoint for forgot password (for backwards compatibility).
     * 
     * POST /api/auth/forgot-password
     * 
     * @param request the password reset request
     * @param httpRequest the HTTP servlet request
     * @return MessageResponse with success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        passwordResetService.initiatePasswordReset(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(new MessageResponse(
            "If an account exists with that email, a password reset link has been sent. " +
            "Please check your email inbox."
        ));
    }

    /**
     * Validate password reset token.
     * Checks if the token is valid and not expired.
     * 
     * GET /api/password-reset/validate
     * 
     * @param token the reset token
     * @return MessageResponse indicating token validity
     */
    @GetMapping("/validate")
    public ResponseEntity<MessageResponse> validateToken(@RequestParam String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("Token is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Invalid or expired token"));
        }
    }

    /**
     * Reset password using valid token.
     * Validates token and updates user password.
     * 
     * POST /api/password-reset/reset
     * 
     * @param request the password reset confirmation request
     * @return MessageResponse with success message
     */
    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        
        passwordResetService.resetPassword(request);
        
        return ResponseEntity.ok(new MessageResponse(
            "Your password has been successfully reset. You can now log in with your new password."
        ));
    }
    
    /**
     * Legacy endpoint for resetting password (for backwards compatibility).
     * 
     * POST /api/auth/reset-password
     * 
     * @param request the password reset confirmation request
     * @return MessageResponse with success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPasswordLegacy(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        
        passwordResetService.resetPassword(request);
        
        return ResponseEntity.ok(new MessageResponse(
            "Your password has been successfully reset. You can now log in with your new password."
        ));
    }

    /**
     * Helper method to get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}
