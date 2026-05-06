package com.rawgul.repository;

import com.rawgul.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides database access methods for user management operations.
 * 
 * @author Raghul
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email address.
     * 
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given username.
     * 
     * @param username the username to check
     * @return true if user exists with this username, false otherwise
     */
    Boolean existsByUsername(String username);

    /**
     * Check if a user exists with the given email.
     * 
     * @param email the email address to check
     * @return true if user exists with this email, false otherwise
     */
    Boolean existsByEmail(String email);

    /**
     * Find user by username or email.
     * Useful for login where user can provide either credential.
     * 
     * @param username the username to search for
     * @param email the email to search for
     * @return Optional containing the user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    /**
     * Find active user by email.
     * Only returns user if account is active.
     * 
     * @param email the email address to search for
     * @return Optional containing the active user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    /**
     * Find active user by username.
     * Only returns user if account is active.
     * 
     * @param username the username to search for
     * @return Optional containing the active user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.active = true")
    Optional<User> findActiveUserByUsername(@Param("username") String username);

    /**
     * Find user by mobile number.
     * 
     * @param mobileNumber the mobile number to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByMobileNumber(String mobileNumber);

    /**
     * Check if a user exists with the given mobile number.
     * 
     * @param mobileNumber the mobile number to check
     * @return true if user exists with this mobile number, false otherwise
     */
    Boolean existsByMobileNumber(String mobileNumber);

}
