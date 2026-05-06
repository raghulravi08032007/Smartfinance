package com.rawgul.service;

import com.rawgul.exception.FileStorageException;
import com.rawgul.exception.ResourceNotFoundException;
import com.rawgul.model.FileMetadata;
import com.rawgul.model.FileMetadata.FileType;
import com.rawgul.model.SupportTicket;
import com.rawgul.model.User;
import com.rawgul.repository.FileMetadataRepository;
import com.rawgul.repository.SupportTicketRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for handling file storage operations
 * Supports local file system storage with metadata tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    @Value("${file.upload.allowed-types:image/jpeg,image/png,image/gif,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document}")
    private String allowedContentTypes;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/csv"
    );

    /**
     * Initialize storage directory
     */
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
            
            // Create subdirectories for different file types
            Files.createDirectories(uploadPath.resolve("profiles"));
            Files.createDirectories(uploadPath.resolve("tickets"));
            Files.createDirectories(uploadPath.resolve("documents"));
            
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    /**
     * Store file with metadata
     */
    @Transactional
    public FileMetadata storeFile(MultipartFile file, FileType fileType, Long userId, Long ticketId, String description) {
        // Validate file
        validateFile(file, fileType);

        // Get user if userId provided
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        }

        // Get ticket if ticketId provided
        SupportTicket ticket = null;
        if (ticketId != null) {
            ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
        }

        // Clean filename
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // Generate unique filename
        String storedFilename = generateUniqueFilename(originalFilename);

        // Determine subdirectory based on file type
        String subDir = getSubdirectory(fileType);
        Path targetLocation = Paths.get(uploadDir).resolve(subDir).resolve(storedFilename);

        try {
            // Ensure directory exists
            Files.createDirectories(targetLocation.getParent());

            // Copy file to target location
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("File stored successfully: {}", storedFilename);

            // Create and save metadata
            FileMetadata metadata = FileMetadata.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(targetLocation.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .fileType(fileType)
                .user(user)
                .supportTicket(ticket)
                .description(description)
                .isPublic(fileType == FileType.PROFILE_PICTURE) // Profile pictures are public by default
                .createdBy(user != null ? user.getUsername() : "system")
                .build();

            metadata = fileMetadataRepository.save(metadata);

            log.info("File metadata saved: id={}, filename={}", metadata.getId(), storedFilename);

            return metadata;

        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Store profile picture for user
     */
    @Transactional
    public FileMetadata storeProfilePicture(MultipartFile file, Long userId) {
        // Delete old profile picture if exists
        Optional<FileMetadata> oldProfilePic = fileMetadataRepository
            .findLatestByUserIdAndFileType(userId, FileType.PROFILE_PICTURE);
        
        oldProfilePic.ifPresent(this::deleteFile);

        return storeFile(file, FileType.PROFILE_PICTURE, userId, null, "Profile Picture");
    }

    /**
     * Store ticket attachment
     */
    @Transactional
    public FileMetadata storeTicketAttachment(MultipartFile file, Long ticketId, Long userId) {
        return storeFile(file, FileType.TICKET_ATTACHMENT, userId, ticketId, "Ticket Attachment");
    }

    /**
     * Load file as Resource
     */
    public Resource loadFileAsResource(String filename) {
        try {
            // Search in all subdirectories
            Path filePath = findFile(filename);
            if (filePath == null) {
                throw new ResourceNotFoundException("File not found: " + filename);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or not readable: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("File not found: " + filename, e);
        }
    }

    /**
     * Get file metadata by ID
     */
    public FileMetadata getFileMetadata(Long fileId) {
        return fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
    }

    /**
     * Get file metadata by stored filename
     */
    public FileMetadata getFileMetadataByFilename(String filename) {
        return fileMetadataRepository.findByStoredFilename(filename)
            .orElseThrow(() -> new ResourceNotFoundException("File not found: " + filename));
    }

    /**
     * Get all files for user
     */
    public List<FileMetadata> getUserFiles(Long userId) {
        return fileMetadataRepository.findByUser_IdOrderByCreatedDateDesc(userId);
    }

    /**
     * Get all files for support ticket
     */
    public List<FileMetadata> getTicketFiles(Long ticketId) {
        return fileMetadataRepository.findBySupportTicket_IdOrderByCreatedDateDesc(ticketId);
    }

    /**
     * Get user's profile picture
     */
    public Optional<FileMetadata> getUserProfilePicture(Long userId) {
        return fileMetadataRepository.findLatestByUserIdAndFileType(userId, FileType.PROFILE_PICTURE);
    }

    /**
     * Delete file and metadata
     */
    @Transactional
    public void deleteFile(Long fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        deleteFile(metadata);
    }

    /**
     * Delete file and metadata
     */
    @Transactional
    public void deleteFile(FileMetadata metadata) {
        try {
            // Delete physical file
            Path filePath = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(filePath);
            
            // Delete metadata
            fileMetadataRepository.delete(metadata);
            
            log.info("File deleted: {}", metadata.getStoredFilename());
        } catch (IOException e) {
            log.error("Failed to delete file: {}", metadata.getStoredFilename(), e);
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    /**
     * Delete all files for user
     */
    @Transactional
    public void deleteUserFiles(Long userId) {
        List<FileMetadata> files = getUserFiles(userId);
        files.forEach(this::deleteFile);
        log.info("Deleted {} files for user {}", files.size(), userId);
    }

    /**
     * Delete all files for ticket
     */
    @Transactional
    public void deleteTicketFiles(Long ticketId) {
        List<FileMetadata> files = getTicketFiles(ticketId);
        files.forEach(this::deleteFile);
        log.info("Deleted {} files for ticket {}", files.size(), ticketId);
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file, FileType fileType) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException(String.format(
                "File size exceeds maximum allowed size of %d MB",
                maxFileSize / (1024 * 1024)
            ));
        }

        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new FileStorageException("Invalid filename: " + filename);
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new FileStorageException("File content type is unknown");
        }

        // Validate content type based on file type
        if (fileType == FileType.PROFILE_PICTURE) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new FileStorageException("Invalid image type. Allowed types: JPEG, PNG, GIF, WEBP");
            }
        } else if (fileType == FileType.TICKET_ATTACHMENT || fileType == FileType.DOCUMENT) {
            Set<String> allowedTypes = new HashSet<>();
            allowedTypes.addAll(ALLOWED_IMAGE_TYPES);
            allowedTypes.addAll(ALLOWED_DOCUMENT_TYPES);
            
            if (!allowedTypes.contains(contentType.toLowerCase())) {
                throw new FileStorageException("Invalid file type. Allowed types: Images, PDF, Word, Excel, Text");
            }
        }

        log.info("File validation passed: {}", filename);
    }

    /**
     * Generate unique filename with timestamp
     */
    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";
        
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        return timestamp + "_" + uuid + extension;
    }

    /**
     * Get subdirectory based on file type
     */
    private String getSubdirectory(FileType fileType) {
        return switch (fileType) {
            case PROFILE_PICTURE -> "profiles";
            case TICKET_ATTACHMENT -> "tickets";
            default -> "documents";
        };
    }

    /**
     * Find file in subdirectories
     */
    private Path findFile(String filename) {
        String[] subdirs = {"profiles", "tickets", "documents"};
        
        for (String subdir : subdirs) {
            Path filePath = Paths.get(uploadDir).resolve(subdir).resolve(filename);
            if (Files.exists(filePath)) {
                return filePath;
            }
        }
        
        // Try root directory as fallback
        Path rootPath = Paths.get(uploadDir).resolve(filename);
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        
        return null;
    }

    /**
     * Get storage statistics for user
     */
    public Map<String, Object> getUserStorageStats(Long userId) {
        long fileCount = fileMetadataRepository.countByUser_Id(userId);
        Long totalSize = fileMetadataRepository.getTotalFileSizeByUserId(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("fileCount", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("totalSizeFormatted", formatFileSize(totalSize));
        stats.put("maxSize", maxFileSize);
        stats.put("maxSizeFormatted", formatFileSize(maxFileSize));
        stats.put("usedPercentage", (totalSize * 100.0) / maxFileSize);
        
        return stats;
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
