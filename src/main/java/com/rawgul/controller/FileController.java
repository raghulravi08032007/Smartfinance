package com.rawgul.controller;

import com.rawgul.model.FileMetadata;
import com.rawgul.model.FileMetadata.FileType;
import com.rawgul.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for file upload and download operations
 * Handles profile pictures, ticket attachments, and documents
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload file (generic endpoint)
     * POST /api/files/upload
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", defaultValue = "DOCUMENT") String fileTypeStr,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "ticketId", required = false) Long ticketId,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        
        try {
            log.info("File upload request: filename={}, type={}, userId={}, ticketId={}", 
                file.getOriginalFilename(), fileTypeStr, userId, ticketId);

            FileType fileType = FileType.valueOf(fileTypeStr);

            // If userId not provided, use authenticated user
            if (userId == null && authentication != null) {
                // You can extract userId from authentication if needed
                // For now, we'll let the service handle it
            }

            FileMetadata metadata = fileStorageService.storeFile(file, fileType, userId, ticketId, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("file", convertToResponse(metadata));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("File upload failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Upload profile picture
     * POST /api/files/profile-picture
     */
    @PostMapping("/profile-picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            Authentication authentication) {
        
        try {
            log.info("Profile picture upload request: userId={}, filename={}", userId, file.getOriginalFilename());

            FileMetadata metadata = fileStorageService.storeProfilePicture(file, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile picture uploaded successfully");
            response.put("file", convertToResponse(metadata));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Profile picture upload failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Profile picture upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Upload ticket attachment
     * POST /api/files/ticket-attachment
     */
    @PostMapping("/ticket-attachment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> uploadTicketAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ticketId") Long ticketId,
            @RequestParam(value = "userId", required = false) Long userId,
            Authentication authentication) {
        
        try {
            log.info("Ticket attachment upload request: ticketId={}, filename={}", ticketId, file.getOriginalFilename());

            FileMetadata metadata = fileStorageService.storeTicketAttachment(file, ticketId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attachment uploaded successfully");
            response.put("file", convertToResponse(metadata));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ticket attachment upload failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Attachment upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Download file by filename
     * GET /api/files/download/{filename}
     */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String filename,
            HttpServletRequest request) {
        
        try {
            log.info("File download request: filename={}", filename);

            // Load file as Resource
            Resource resource = fileStorageService.loadFileAsResource(filename);

            // Get file metadata
            FileMetadata metadata = fileStorageService.getFileMetadataByFilename(filename);

            // Determine content type
            String contentType = metadata.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("File download failed: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * View/display file inline (for images, PDFs)
     * GET /api/files/view/{filename}
     */
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String filename,
            HttpServletRequest request) {
        
        try {
            log.info("File view request: filename={}", filename);

            Resource resource = fileStorageService.loadFileAsResource(filename);
            FileMetadata metadata = fileStorageService.getFileMetadataByFilename(filename);

            String contentType = metadata.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("File view failed: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get file metadata by ID
     * GET /api/files/{fileId}
     */
    @GetMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFileMetadata(@PathVariable Long fileId) {
        try {
            FileMetadata metadata = fileStorageService.getFileMetadata(fileId);
            return ResponseEntity.ok(convertToResponse(metadata));
        } catch (Exception e) {
            log.error("Failed to get file metadata: fileId={}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all files for user
     * GET /api/files/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getUserFiles(@PathVariable Long userId) {
        try {
            List<FileMetadata> files = fileStorageService.getUserFiles(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", files.stream().map(this::convertToResponse).toList());
            response.put("count", files.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get user files: userId={}", userId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve files");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all files for support ticket
     * GET /api/files/ticket/{ticketId}
     */
    @GetMapping("/ticket/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTicketFiles(@PathVariable Long ticketId) {
        try {
            List<FileMetadata> files = fileStorageService.getTicketFiles(ticketId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", files.stream().map(this::convertToResponse).toList());
            response.put("count", files.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get ticket files: ticketId={}", ticketId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve files");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user's profile picture
     * GET /api/files/profile-picture/{userId}
     */
    @GetMapping("/profile-picture/{userId}")
    public ResponseEntity<Resource> getUserProfilePicture(@PathVariable Long userId) {
        try {
            FileMetadata metadata = fileStorageService.getUserProfilePicture(userId)
                    .orElseThrow(() -> new RuntimeException("Profile picture not found"));

            Resource resource = fileStorageService.loadFileAsResource(metadata.getStoredFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(metadata.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Profile picture not found for userId={}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete file
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable Long fileId) {
        try {
            fileStorageService.deleteFile(fileId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete file: fileId={}", fileId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get storage statistics for user
     * GET /api/files/storage-stats/{userId}
     */
    @GetMapping("/storage-stats/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStorageStats(@PathVariable Long userId) {
        try {
            Map<String, Object> stats = fileStorageService.getUserStorageStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get storage stats: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to get storage statistics"
            ));
        }
    }

    /**
     * Convert FileMetadata to response map
     */
    private Map<String, Object> convertToResponse(FileMetadata metadata) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", metadata.getId());
        response.put("originalFilename", metadata.getOriginalFilename());
        response.put("storedFilename", metadata.getStoredFilename());
        response.put("contentType", metadata.getContentType());
        response.put("fileSize", metadata.getFileSize());
        response.put("fileSizeFormatted", metadata.getFormattedFileSize());
        response.put("fileType", metadata.getFileType().name());
        response.put("description", metadata.getDescription());
        response.put("isPublic", metadata.getIsPublic());
        response.put("createdDate", metadata.getCreatedDate());
        response.put("downloadUrl", "/api/files/download/" + metadata.getStoredFilename());
        response.put("viewUrl", "/api/files/view/" + metadata.getStoredFilename());
        response.put("isImage", metadata.isImage());
        
        if (metadata.getUser() != null) {
            response.put("userId", metadata.getUser().getId());
        }
        if (metadata.getSupportTicket() != null) {
            response.put("ticketId", metadata.getSupportTicket().getId());
        }
        
        return response;
    }
}
