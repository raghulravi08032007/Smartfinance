package com.rawgul.repository;

import com.rawgul.model.PasswordResetToken;
import com.rawgul.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PasswordResetToken entity.
 * Provides database access methods for password reset token management.
 * 
 * @author Raghul
 * @since 1.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find password reset token by token string.
     * 
     * @param token the token string to search for
     * @return Optional containing the token if found, empty otherwise
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all tokens for a specific user.
     * 
     * @param user the user entity
     * @return List of password reset tokens for the user
     */
    List<PasswordResetToken> findByUser(User user);

    /**
     * Find valid token for a user (not used and not expired).
     * 
     * @param user the user entity
     * @param currentDate the current date/time
     * @return Optional containing the valid token if found
     */
    Optional<PasswordResetToken> findByUserAndUsedFalseAndExpiryDateAfter(User user, LocalDateTime currentDate);

    /**
     * Find valid (not expired and not used) token by token string.
     * 
     * @param token the token string
     * @param currentDate the current date/time
     * @return Optional containing the valid token if found, empty otherwise
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.used = false AND t.expiryDate > :currentDate")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("currentDate") LocalDateTime currentDate);

    /**
     * Count recent tokens for a user within a time period.
     * Used for rate limiting password reset requests.
     * 
     * @param userId the ID of the user
     * @param since the start of the time period
     * @return Number of tokens created since the specified time
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.id = :userId AND t.createdDate > :since")
    Long countRecentTokensByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Delete all expired tokens.
     * This method is used by scheduled cleanup tasks.
     * 
     * @param date the current date/time
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :date")
    void deleteExpiredTokens(@Param("date") LocalDateTime date);

    /**
     * Delete all tokens for a specific user by user ID.
     * 
     * @param userId the ID of the user
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Find tokens that have expired.
     * 
     * @param date the current date/time
     * @return List of expired tokens
     */
    List<PasswordResetToken> findByExpiryDateBefore(LocalDateTime date);

    /**
     * Delete all tokens that expired before the given date.
     * Alternative method for cleanup operations.
     * 
     * @param date the cutoff date/time
     * @return Number of deleted tokens
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :date")
    int deleteByExpiryDateBefore(@Param("date") LocalDateTime date);

    /**
     * Check if a valid token exists.
     * 
     * @param token the token string
     * @return true if valid unused token exists, false otherwise
     */
    boolean existsByTokenAndUsedFalse(String token);

    /**
     * Find all tokens for a specific user ID.
     * 
     * @param userId the ID of the user
     * @return List of password reset tokens for the user
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.id = :userId ORDER BY prt.createdDate DESC")
    List<PasswordResetToken> findByUserId(@Param("userId") Long userId);

    /**
     * Find all active (not expired and not used) tokens for a user.
     * 
     * @param userId the ID of the user
     * @param now the current date/time
     * @return List of active tokens for the user
     */
    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.expiryDate > :now AND prt.used = false ORDER BY prt.createdDate DESC")
    List<PasswordResetToken> findActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * Invalidate all tokens for a user by marking them as used.
     * 
     * @param userId the ID of the user
     * @return Number of tokens invalidated
     */
    @Transactional
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user.id = :userId AND prt.used = false")
    int invalidateAllUserTokens(@Param("userId") Long userId);

}
