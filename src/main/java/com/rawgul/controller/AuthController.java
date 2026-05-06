package com.rawgul.controller;

import com.rawgul.dto.JwtResponse;
import com.rawgul.dto.LoginRequest;
import com.rawgul.dto.MessageResponse;
import com.rawgul.dto.SignupRequest;
import com.rawgul.model.User;
import com.rawgul.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller.
 * Handles user authentication, registration, and token management.
 * 
 * @author Raghul
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * User login endpoint.
     * Authenticates user credentials and returns JWT token.
     * 
     * POST /api/auth/login
     * 
     * @param loginRequest the login credentials
     * @return JwtResponse with token and user details
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for user: {}", loginRequest.getUsername());
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * User registration endpoint.
     * Creates a new user account with the provided details.
     * 
     * POST /api/auth/register
     * 
     * @param signupRequest the registration details
     * @return MessageResponse with success message
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Registration request received for username: {}", signupRequest.getUsername());
        User user = authService.registerUser(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MessageResponse("User registered successfully with ID: " + user.getId()));
    }

    /**
     * Legacy signup endpoint (for backwards compatibility).
     * 
     * POST /api/auth/signup
     * 
     * @param signupRequest the registration details
     * @return MessageResponse with success message
     */
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signupUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Signup request received for username: {}", signupRequest.getUsername());
        User user = authService.registerUser(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MessageResponse("User registered successfully with ID: " + user.getId()));
    }

    /**
     * Refresh JWT token endpoint.
     * Generates a new JWT token for the authenticated user.
     * 
     * POST /api/auth/refresh
     * 
     * @return JwtResponse with new token
     */
    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JwtResponse> refreshToken() {
        log.info("Token refresh request received");
        JwtResponse jwtResponse = authService.refreshToken();
        return ResponseEntity.ok(jwtResponse);
    }

    /**
     * User logout endpoint.
     * Clears the security context.
     * 
     * POST /api/auth/logout
     * 
     * @return MessageResponse with success message
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout() {
        log.info("Logout request received");
        authService.logout();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    /**
     * Get current authenticated user details.
     * 
     * GET /api/auth/me
     * 
     * @return User entity with current user details
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getCurrentUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

}
