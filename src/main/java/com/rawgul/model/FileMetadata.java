package com.rawgul.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FileMetadata Entity - Stores metadata about uploaded files
 * Supports both user profile pictures and support ticket attachments
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_type", columnList = "file_type"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_created_date", columnList = "created_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Original filename as uploaded by user
     */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /**
     * Stored filename (unique, generated)
     */
    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;

    /**
     * File path on server/cloud storage
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * MIME type (e.g., image/jpeg, application/pdf)
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * File size in bytes
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Type of file (PROFILE_PICTURE, TICKET_ATTACHMENT, DOCUMENT)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 50)
    private FileType fileType;

    /**
     * Associated user (for profile pictures)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_file_user"))
    private User user;

    /**
     * Associated support ticket (for attachments)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", foreignKey = @ForeignKey(name = "fk_file_ticket"))
    private SupportTicket supportTicket;

    /**
     * File description or notes
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Public access URL (if using cloud storage)
     */
    @Column(name = "access_url", length = 500)
    private String accessUrl;

    /**
     * Whether file is publicly accessible
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Audit fields
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    /**
     * File type enum
     */
    public enum FileType {
        PROFILE_PICTURE("Profile Picture"),
        TICKET_ATTACHMENT("Support Ticket Attachment"),
        DOCUMENT("General Document"),
        REPORT("Financial Report"),
        INVOICE("Invoice");

        private final String description;

        FileType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Helper method to get file extension
     */
    @Transient
    public String getFileExtension() {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return "";
    }

    /**
     * Helper method to check if file is an image
     */
    @Transient
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Helper method to get human-readable file size
     */
    @Transient
    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
