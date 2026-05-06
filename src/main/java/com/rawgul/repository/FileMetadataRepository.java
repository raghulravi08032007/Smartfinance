package com.rawgul.repository;

import com.rawgul.model.FileMetadata;
import com.rawgul.model.FileMetadata.FileType;
import com.rawgul.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FileMetadata entity
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Find file by stored filename
     */
    Optional<FileMetadata> findByStoredFilename(String storedFilename);

    /**
     * Find all files by user
     */
    List<FileMetadata> findByUserOrderByCreatedDateDesc(User user);

    /**
     * Find all files by user ID
     */
    List<FileMetadata> findByUser_IdOrderByCreatedDateDesc(Long userId);

    /**
     * Find all files by support ticket ID
     */
    List<FileMetadata> findBySupportTicket_IdOrderByCreatedDateDesc(Long ticketId);

    /**
     * Find files by file type
     */
    List<FileMetadata> findByFileTypeOrderByCreatedDateDesc(FileType fileType);

    /**
     * Find user's profile picture
     */
    Optional<FileMetadata> findFirstByUserAndFileTypeOrderByCreatedDateDesc(User user, FileType fileType);

    /**
     * Find user's profile picture by user ID
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.user.id = :userId AND f.fileType = :fileType ORDER BY f.createdDate DESC")
    Optional<FileMetadata> findLatestByUserIdAndFileType(@Param("userId") Long userId, @Param("fileType") FileType fileType);

    /**
     * Count files by user
     */
    long countByUser(User user);

    /**
     * Count files by user ID
     */
    long countByUser_Id(Long userId);

    /**
     * Get total file size for user
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.user.id = :userId")
    Long getTotalFileSizeByUserId(@Param("userId") Long userId);

    /**
     * Find public files
     */
    List<FileMetadata> findByIsPublicTrueOrderByCreatedDateDesc();

    /**
     * Delete all files by user
     */
    void deleteByUser(User user);

    /**
     * Delete all files by support ticket
     */
    void deleteAllBySupportTicket_Id(Long ticketId);
}
