package com.rawgul.service;

import com.rawgul.exceptions.ResourceAlreadyExistsException;
import com.rawgul.exceptions.ResourceNotFoundException;
import com.rawgul.exceptions.ValidationException;
import com.rawgul.model.Role;
import com.rawgul.model.User;
import com.rawgul.repository.RoleRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service class for User management operations.
 * Handles user registration, profile management, password updates, and account operations.
 * 
 * @author Raghul
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Get all users (admin operation).
     * 
     * @return List of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Get user by ID.
     * 
     * @param id the user ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Get user by username.
     * 
     * @param username the username
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * Get user by email.
     * 
     * @param email the email address
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    /**
     * Get active user by username.
     * 
     * @param username the username
     * @return User entity if active
     * @throws ResourceNotFoundException if user not found or not active
     */
    @Transactional(readOnly = true)
    public User getActiveUserByUsername(String username) {
        log.debug("Fetching active user with username: {}", username);
        return userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found with username: " + username));
    }

    /**
     * Get active user by email.
     * 
     * @param email the email address
     * @return User entity if active
     * @throws ResourceNotFoundException if user not found or not active
     */
    @Transactional(readOnly = true)
    public User getActiveUserByEmail(String email) {
        log.debug("Fetching active user with email: {}", email);
        return userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found with email: " + email));
    }

    /**
     * Create a new user.
     * 
     * @param user the user to create
     * @return Created user
     * @throws ResourceAlreadyExistsException if username or email already exists
     */
    public User createUser(User user) {
        log.info("Creating new user with username: {}", user.getUsername());
        
        validateUserForCreation(user);
        
        // Encode password if not already encoded
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        // Set default values
        if (user.getActive() == null) {
            user.setActive(true);
        }
        if (user.getAccountLocked() == null) {
            user.setAccountLocked(false);
        }
        if (user.getFailedLoginAttempts() == null) {
            user.setFailedLoginAttempts(0);
        }
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Update user profile information.
     * 
     * @param userId the ID of the user to update
     * @param updatedUser the updated user data
     * @return Updated user
     * @throws ResourceNotFoundException if user not found
     */
    public User updateUserProfile(Long userId, User updatedUser) {
        log.info("Updating profile for user id: {}", userId);
        
        User existingUser = getUserById(userId);
        
        // Update allowed fields
        if (updatedUser.getFirstName() != null) {
            existingUser.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null) {
            existingUser.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getMobileNumber() != null) {
            // Check if mobile number is already used by another user
            if (userRepository.existsByMobileNumber(updatedUser.getMobileNumber()) &&
                !updatedUser.getMobileNumber().equals(existingUser.getMobileNumber())) {
                throw new ResourceAlreadyExistsException("Mobile number is already in use");
            }
            existingUser.setMobileNumber(updatedUser.getMobileNumber());
        }
        
        User savedUser = userRepository.save(existingUser);
        log.info("Profile updated successfully for user id: {}", userId);
        return savedUser;
    }

    /**
     * Update user password.
     * 
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     * @throws ValidationException if current password is incorrect
     */
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Updating password for user id: {}", userId);
        
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Failed password update attempt for user id: {} - incorrect current password", userId);
            throw new ValidationException("Current password is incorrect");
        }
        
        // Validate new password
        validatePassword(newPassword);
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password updated successfully for user id: {}", userId);
    }

    /**
     * Update user roles (admin operation).
     * 
     * @param userId the user ID
     * @param roles the new roles
     * @return Updated user
     */
    public User updateUserRoles(Long userId, Set<Role> roles) {
        log.info("Updating roles for user id: {}", userId);
        
        User user = getUserById(userId);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        log.info("Roles updated successfully for user id: {}", userId);
        return savedUser;
    }
    
    /**
     * Get all available roles in the system.
     * 
     * @return List of all roles
     */
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Activate user account.
     * 
     * @param userId the user ID
     * @return Updated user
     */
    public User activateUser(Long userId) {
        log.info("Activating user account for id: {}", userId);
        
        User user = getUserById(userId);
        user.setActive(true);
        user.setAccountLocked(false);
        user.resetFailedLoginAttempts();
        
        User savedUser = userRepository.save(user);
        log.info("User account activated successfully for id: {}", userId);
        return savedUser;
    }

    /**
     * Deactivate user account.
     * 
     * @param userId the user ID
     * @return Updated user
     */
    public User deactivateUser(Long userId) {
        log.info("Deactivating user account for id: {}", userId);
        
        User user = getUserById(userId);
        user.setActive(false);
        
        User savedUser = userRepository.save(user);
        log.info("User account deactivated successfully for id: {}", userId);
        return savedUser;
    }

    /**
     * Lock user account (due to security reasons).
     * 
     * @param userId the user ID
     * @return Updated user
     */
    public User lockUserAccount(Long userId) {
        log.info("Locking user account for id: {}", userId);
        
        User user = getUserById(userId);
        user.setAccountLocked(true);
        
        User savedUser = userRepository.save(user);
        log.warn("User account locked for id: {}", userId);
        return savedUser;
    }

    /**
     * Unlock user account.
     * 
     * @param userId the user ID
     * @return Updated user
     */
    public User unlockUserAccount(Long userId) {
        log.info("Unlocking user account for id: {}", userId);
        
        User user = getUserById(userId);
        user.setAccountLocked(false);
        user.resetFailedLoginAttempts();
        
        User savedUser = userRepository.save(user);
        log.info("User account unlocked successfully for id: {}", userId);
        return savedUser;
    }

    /**
     * Handle failed login attempt.
     * Increments failed login attempts and locks account if threshold exceeded.
     * 
     * @param username the username
     */
    public void handleFailedLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.incrementFailedLoginAttempts();
            
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                log.warn("User account locked due to {} failed login attempts: {}", 
                        MAX_FAILED_ATTEMPTS, username);
            }
            
            userRepository.save(user);
        });
    }

    /**
     * Handle successful login.
     * Resets failed login attempts and updates last login time.
     * 
     * @param username the username
     */
    public void handleSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.resetFailedLoginAttempts();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login time for user: {}", username);
        });
    }

    /**
     * Delete user account.
     * 
     * @param userId the user ID
     * @throws ResourceNotFoundException if user not found
     */
    public void deleteUser(Long userId) {
        log.info("Deleting user with id: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        
        userRepository.deleteById(userId);
        log.info("User deleted successfully with id: {}", userId);
    }

    /**
     * Check if username exists.
     * 
     * @param username the username to check
     * @return true if exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists.
     * 
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if mobile number exists.
     * 
     * @param mobileNumber the mobile number to check
     * @return true if exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByMobileNumber(String mobileNumber) {
        return userRepository.existsByMobileNumber(mobileNumber);
    }

    /**
     * Validate user data for creation.
     * 
     * @param user the user to validate
     * @throws ResourceAlreadyExistsException if username, email, or mobile already exists
     * @throws ValidationException if required fields are missing
     */
    private void validateUserForCreation(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }
        
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResourceAlreadyExistsException("Username is already taken: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already in use: " + user.getEmail());
        }
        
        if (user.getMobileNumber() != null && userRepository.existsByMobileNumber(user.getMobileNumber())) {
            throw new ResourceAlreadyExistsException("Mobile number is already in use: " + user.getMobileNumber());
        }
    }

    /**
     * Validate password strength.
     * 
     * @param password the password to validate
     * @throws ValidationException if password doesn't meet requirements
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new ValidationException("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new ValidationException("Password must contain at least one special character");
        }
    }

}
