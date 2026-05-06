package com.rawgul.service;

import com.rawgul.dto.JwtResponse;
import com.rawgul.dto.LoginRequest;
import com.rawgul.dto.SignupRequest;
import com.rawgul.exceptions.AccountLockedException;
import com.rawgul.exceptions.ResourceAlreadyExistsException;
import com.rawgul.exceptions.ValidationException;
import com.rawgul.model.Role;
import com.rawgul.model.Role.RoleType;
import com.rawgul.model.User;
import com.rawgul.repository.RoleRepository;
import com.rawgul.repository.UserRepository;
import com.rawgul.security.JwtTokenProvider;
import com.rawgul.security.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Service for handling user authentication and registration.
 * Manages JWT token generation, validation, and user login/signup operations.
 * 
 * @author Raghul
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * Authenticate user and generate JWT token.
     * 
     * @param loginRequest the login credentials
     * @return JwtResponse containing token and user details
     * @throws BadCredentialsException if credentials are invalid
     * @throws AccountLockedException if account is locked
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authentication attempt for user: {}", loginRequest.getUsername());
        
        try {
            // Check if user exists and is not locked
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
            
            if (user.getAccountLocked() != null && user.getAccountLocked()) {
                log.warn("Login attempt for locked account: {}", loginRequest.getUsername());
                throw new AccountLockedException("Account is locked due to multiple failed login attempts. Please contact support.");
            }
            
            if (user.getActive() != null && !user.getActive()) {
                log.warn("Login attempt for inactive account: {}", loginRequest.getUsername());
                throw new AccountLockedException("Account is not active. Please contact support.");
            }
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Generate JWT token
            String jwt = jwtTokenProvider.generateToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());
            
            // Handle successful login
            userService.handleSuccessfulLogin(loginRequest.getUsername());
            
            log.info("User authenticated successfully: {}", loginRequest.getUsername());

            return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
            );
            
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", loginRequest.getUsername());
            userService.handleFailedLogin(loginRequest.getUsername());
            throw e;
        }
    }

    /**
     * Register a new user account.
     * 
     * @param signupRequest the registration data
     * @return Created user entity
     * @throws ResourceAlreadyExistsException if username/email already exists
     * @throws ValidationException if validation fails
     */
    public User registerUser(SignupRequest signupRequest) {
        log.info("User registration attempt for username: {}", signupRequest.getUsername());
        
        // Validate passwords match
        if (!signupRequest.passwordsMatch()) {
            throw new ValidationException("Passwords do not match");
        }

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new ResourceAlreadyExistsException("Username is already taken");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already in use");
        }
        
        if (signupRequest.getMobile() != null && 
            userRepository.existsByMobileNumber(signupRequest.getMobile())) {
            throw new ResourceAlreadyExistsException("Mobile number is already in use");
        }

        // Parse full name if provided
        signupRequest.parseFullName();

        // Create user entity
        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .firstName(signupRequest.getFirstName())
                .lastName(signupRequest.getLastName())
                .mobileNumber(signupRequest.getMobile())
                .active(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        // Assign roles
        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role not found"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found"));
                        roles.add(adminRole);
                        break;
                    case "moderator":
                        Role modRole = roleRepository.findByName(RoleType.ROLE_MODERATOR)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found"));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Error: Role not found"));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        log.info("User registered successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Validate JWT token.
     * 
     * @param token the JWT token
     * @return true if valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get username from JWT token.
     * 
     * @param token the JWT token
     * @return username from token
     */
    @Transactional(readOnly = true)
    public String getUsernameFromToken(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * Refresh JWT token.
     * Generates a new token for the authenticated user.
     * 
     * @return JwtResponse with new token
     */
    public JwtResponse refreshToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }
        
        String jwt = jwtTokenProvider.generateToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());
        
        log.info("Token refreshed for user: {}", userDetails.getUsername());
        
        return new JwtResponse(
            jwt,
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            roles
        );
    }

    /**
     * Logout user by invalidating the security context.
     */
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            log.info("User logged out: {}", authentication.getName());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Get currently authenticated user.
     * 
     * @return User entity
     * @throws RuntimeException if no user is authenticated
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No user is currently authenticated");
        }
        
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }

}
