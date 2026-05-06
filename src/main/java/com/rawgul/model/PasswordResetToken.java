package com.rawgul.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PasswordResetToken Entity - Represents a password reset token for user authentication
 * Maps to the frontend forgot-password.html functionality
 */
@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_expiry_date", columnList = "expiry_date"),
    @Index(name = "idx_user_id_reset", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Token is required")
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "used_date")
    private LocalDateTime usedDate;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // Relationship with User
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reset_token_user"))
    private User user;

    // Audit field
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // Constants
    private static final int EXPIRATION_HOURS = 24; // Token valid for 24 hours

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (this.token == null) {
            this.token = generateToken();
        }
        if (this.expiryDate == null) {
            this.expiryDate = calculateExpiryDate();
        }
    }

    // Business methods
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusHours(EXPIRATION_HOURS);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    public boolean isUsed() {
        return Boolean.TRUE.equals(this.used);
    }

    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedDate = LocalDateTime.now();
    }

    public long getMinutesUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), this.expiryDate).toMinutes();
    }

    public String getExpiryStatus() {
        if (this.used) {
            return "Used";
        }
        if (isExpired()) {
            return "Expired";
        }
        long minutesLeft = getMinutesUntilExpiry();
        if (minutesLeft < 60) {
            return "Expires in " + minutesLeft + " minutes";
        }
        return "Expires in " + (minutesLeft / 60) + " hours";
    }

}
