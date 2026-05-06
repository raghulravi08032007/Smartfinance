package com.rawgul.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * SupportTicket Entity - Represents a support request from a user
 * Maps to the frontend support.html form
 */
@Entity
@Table(name = "support_tickets", indexes = {
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_priority", columnList = "priority"),
    @Index(name = "idx_ticket_created_date", columnList = "created_date"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    @Column(nullable = false, length = 5000, columnDefinition = "TEXT")
    private String message;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 50)
    private String ticketNumber;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @Size(max = 5000, message = "Resolution notes must not exceed 5000 characters")
    @Column(name = "resolution_notes", length = 5000, columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    // Relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_ticket_user"))
    private User user;

    // Audit fields
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Enums
    public enum TicketStatus {
        OPEN("Open - Awaiting review"),
        IN_PROGRESS("In Progress - Being addressed"),
        PENDING_USER("Pending - Waiting for user response"),
        RESOLVED("Resolved - Issue fixed"),
        CLOSED("Closed - Ticket completed");

        private final String description;

        TicketStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum TicketPriority {
        LOW("Low - General question"),
        MEDIUM("Medium - Need assistance"),
        HIGH("High - Urgent issue"),
        CRITICAL("Critical - System down");

        private final String description;

        TicketPriority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (this.ticketNumber == null) {
            this.ticketNumber = generateTicketNumber();
        }
    }

    // Business methods
    private String generateTicketNumber() {
        return "TKT-" + System.currentTimeMillis();
    }

    public void resolve(String notes) {
        this.status = TicketStatus.RESOLVED;
        this.resolvedDate = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    public void close() {
        this.status = TicketStatus.CLOSED;
    }

    public void assignTo(String assignee) {
        this.assignedTo = assignee;
        this.status = TicketStatus.IN_PROGRESS;
    }

    public boolean isOpen() {
        return this.status == TicketStatus.OPEN || this.status == TicketStatus.IN_PROGRESS;
    }

    public boolean isResolved() {
        return this.status == TicketStatus.RESOLVED || this.status == TicketStatus.CLOSED;
    }

}
