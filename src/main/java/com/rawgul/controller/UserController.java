package com.rawgul.controller;

import com.rawgul.dto.MessageResponse;
import com.rawgul.dto.PasswordChangeRequest;
import com.rawgul.dto.UserProfileUpdateRequest;
import com.rawgul.model.User;
import com.rawgul.service.AuthService;
import com.rawgul.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller.
 * Handles user profile management and administrative operations.
 * 
 * @author Raghul
 * @since 1.0
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Get all users (admin only).
     * 
     * GET /api/users
     * 
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.debug("Fetching all users");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID.
     * 
     * GET /api/users/{id}
     * 
     * @param id the user ID
     * @return User entity
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.debug("Fetching user with id: {}", id);
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get current user's profile.
     * 
     * GET /api/users/profile
     * 
     * @return Current user's profile
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getCurrentUserProfile() {
        log.debug("Fetching current user profile");
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    /**
     * Update current user's profile.
     * 
     * PUT /api/users/profile
     * 
     * @param updateRequest the profile update request
     * @return Updated user profile
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateUserProfile(@Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        log.info("Updating user profile");
        User currentUser = authService.getCurrentUser();
        
        // Create updated user object
        User updatedUser = User.builder()
                .firstName(updateRequest.getFirstName())
                .lastName(updateRequest.getLastName())
                .mobileNumber(updateRequest.getMobileNumber())
                .build();
        
        User savedUser = userService.updateUserProfile(currentUser.getId(), updatedUser);
        return ResponseEntity.ok(savedUser);
    }

    /**
     * Change current user's password.
     * 
     * POST /api/users/change-password
     * 
     * @param passwordChangeRequest the password change request
     * @return MessageResponse with success message
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody PasswordChangeRequest passwordChangeRequest) {
        log.info("Password change request received");
        User currentUser = authService.getCurrentUser();
        
        userService.updatePassword(
            currentUser.getId(),
            passwordChangeRequest.getCurrentPassword(),
            passwordChangeRequest.getNewPassword()
        );
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    /**
     * Activate user account (admin only).
     * 
     * PUT /api/users/{id}/activate
     * 
     * @param id the user ID
     * @return MessageResponse with success message
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable Long id) {
        log.info("Activating user account: {}", id);
        userService.activateUser(id);
        return ResponseEntity.ok(new MessageResponse("User account activated successfully"));
    }

    /**
     * Deactivate user account (admin only).
     * 
     * PUT /api/users/{id}/deactivate
     * 
     * @param id the user ID
     * @return MessageResponse with success message
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user account: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(new MessageResponse("User account deactivated successfully"));
    }

    /**
     * Lock user account (admin only).
     * 
     * PUT /api/users/{id}/lock
     * 
     * @param id the user ID
     * @return MessageResponse with success message
     */
    @PutMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> lockUser(@PathVariable Long id) {
        log.info("Locking user account: {}", id);
        userService.lockUserAccount(id);
        return ResponseEntity.ok(new MessageResponse("User account locked successfully"));
    }

    /**
     * Unlock user account (admin only).
     * 
     * PUT /api/users/{id}/unlock
     * 
     * @param id the user ID
     * @return MessageResponse with success message
     */
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> unlockUser(@PathVariable Long id) {
        log.info("Unlocking user account: {}", id);
        userService.unlockUserAccount(id);
        return ResponseEntity.ok(new MessageResponse("User account unlocked successfully"));
    }

    /**
     * Delete user account (admin only).
     * 
     * DELETE /api/users/{id}
     * 
     * @param id the user ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
