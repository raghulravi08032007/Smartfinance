/**
 * File Upload Component
 * Handles file uploads with drag-and-drop, preview, and validation
 */

class FileUploadManager {
    constructor(options = {}) {
        this.apiBaseUrl = options.apiBaseUrl || '/api/files';
        this.maxFileSize = options.maxFileSize || 10 * 1024 * 1024; // 10MB default
        this.allowedTypes = options.allowedTypes || [
            'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'text/plain', 'text/csv'
        ];
        
        this.uploadArea = null;
        this.fileInput = null;
        this.previewList = null;
        
        this.init();
    }

    /**
     * Initialize the file upload component
     */
    init() {
        this.setupUploadArea();
        this.setupEventListeners();
        this.loadUserFiles();
        this.loadStorageStats();
    }

    /**
     * Setup upload area elements
     */
    setupUploadArea() {
        this.uploadArea = document.getElementById('file-upload-area');
        this.fileInput = document.getElementById('file-input');
        this.previewList = document.getElementById('file-preview-list');
    }

    /**
     * Setup event listeners
     */
    setupEventListeners() {
        if (!this.uploadArea || !this.fileInput) return;

        // Click to upload
        this.uploadArea.addEventListener('click', (e) => {
            if (!e.target.closest('.file-upload-button')) {
                this.fileInput.click();
            }
        });

        // File selection
        this.fileInput.addEventListener('change', (e) => {
            this.handleFiles(e.target.files);
        });

        // Drag and drop events
        this.uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            this.uploadArea.classList.add('drag-over');
        });

        this.uploadArea.addEventListener('dragleave', (e) => {
            e.preventDefault();
            this.uploadArea.classList.remove('drag-over');
        });

        this.uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            this.uploadArea.classList.remove('drag-over');
            this.handleFiles(e.dataTransfer.files);
        });
    }

    /**
     * Handle selected files
     */
    async handleFiles(files) {
        if (!files || files.length === 0) return;

        for (let file of files) {
            if (this.validateFile(file)) {
                await this.uploadFile(file);
            }
        }

        // Clear input
        this.fileInput.value = '';
    }

    /**
     * Validate file before upload
     */
    validateFile(file) {
        // Check file size
        if (file.size > this.maxFileSize) {
            this.showToast(
                `File "${file.name}" is too large. Maximum size is ${this.formatFileSize(this.maxFileSize)}`,
                'error'
            );
            return false;
        }

        // Check file type
        if (!this.allowedTypes.includes(file.type)) {
            this.showToast(
                `File type "${file.type}" is not allowed for "${file.name}"`,
                'error'
            );
            return false;
        }

        return true;
    }

    /**
     * Upload file to server
     */
    async uploadFile(file, fileType = 'DOCUMENT', userId = null, ticketId = null) {
        try {
            // Show progress
            this.showUploadProgress();

            // Create FormData
            const formData = new FormData();
            formData.append('file', file);
            formData.append('fileType', fileType);
            if (userId) formData.append('userId', userId);
            if (ticketId) formData.append('ticketId', ticketId);
            formData.append('description', `Uploaded: ${file.name}`);

            // Upload file
            const response = await fetch(`${this.apiBaseUrl}/upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error('Upload failed');
            }

            const result = await response.json();

            this.hideUploadProgress();
            this.showToast('File uploaded successfully!', 'success');
            
            // Refresh file list
            this.loadUserFiles();
            this.loadStorageStats();

            return result;

        } catch (error) {
            console.error('Upload error:', error);
            this.hideUploadProgress();
            this.showToast('Upload failed: ' + error.message, 'error');
            throw error;
        }
    }

    /**
     * Upload profile picture
     */
    async uploadProfilePicture(file, userId) {
        try {
            this.showUploadProgress();

            const formData = new FormData();
            formData.append('file', file);
            formData.append('userId', userId);

            const response = await fetch(`${this.apiBaseUrl}/profile-picture`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error('Profile picture upload failed');
            }

            const result = await response.json();

            this.hideUploadProgress();
            this.showToast('Profile picture updated successfully!', 'success');

            return result;

        } catch (error) {
            console.error('Profile picture upload error:', error);
            this.hideUploadProgress();
            this.showToast('Profile picture upload failed: ' + error.message, 'error');
            throw error;
        }
    }

    /**
     * Upload ticket attachment
     */
    async uploadTicketAttachment(file, ticketId, userId = null) {
        try {
            this.showUploadProgress();

            const formData = new FormData();
            formData.append('file', file);
            formData.append('ticketId', ticketId);
            if (userId) formData.append('userId', userId);

            const response = await fetch(`${this.apiBaseUrl}/ticket-attachment`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                },
                body: formData
            });

            if (!response.ok) {
                throw new Error('Attachment upload failed');
            }

            const result = await response.json();

            this.hideUploadProgress();
            this.showToast('Attachment uploaded successfully!', 'success');

            return result;

        } catch (error) {
            console.error('Attachment upload error:', error);
            this.hideUploadProgress();
            this.showToast('Attachment upload failed: ' + error.message, 'error');
            throw error;
        }
    }

    /**
     * Load user files
     */
    async loadUserFiles(userId = null) {
        try {
            if (!userId) {
                const user = UserManager.getCurrentUser();
                if (!user) return;
                userId = user.id;
            }

            const response = await fetch(`${this.apiBaseUrl}/user/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load files');
            }

            const result = await response.json();
            this.renderFileList(result.files || []);

        } catch (error) {
            console.error('Load files error:', error);
        }
    }

    /**
     * Load ticket files
     */
    async loadTicketFiles(ticketId) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/ticket/${ticketId}`, {
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load ticket files');
            }

            const result = await response.json();
            this.renderFileList(result.files || []);

        } catch (error) {
            console.error('Load ticket files error:', error);
        }
    }

    /**
     * Render file list
     */
    renderFileList(files) {
        if (!this.previewList) return;

        if (files.length === 0) {
            this.previewList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-folder-open"></i>
                    <p>No files uploaded yet</p>
                </div>
            `;
            return;
        }

        const html = files.map(file => this.createFilePreviewHTML(file)).join('');
        this.previewList.innerHTML = html;

        // Attach event listeners
        files.forEach(file => {
            this.attachFileActions(file.id);
        });
    }

    /**
     * Create file preview HTML
     */
    createFilePreviewHTML(file) {
        const iconClass = this.getFileIconClass(file.contentType);
        const icon = this.getFileIcon(file.contentType);

        let preview = '';
        if (file.isImage) {
            preview = `<img src="${file.viewUrl}" alt="${file.originalFilename}" class="file-preview-image">`;
        } else {
            preview = `<div class="file-preview-icon ${iconClass}"><i class="${icon}"></i></div>`;
        }

        return `
            <div class="file-preview-item" data-file-id="${file.id}">
                ${preview}
                <div class="file-preview-info">
                    <div class="file-preview-name">${file.originalFilename}</div>
                    <div class="file-preview-details">
                        <span class="file-preview-type">${file.fileType}</span>
                        <span class="file-preview-size">${file.fileSizeFormatted}</span>
                    </div>
                    <div class="file-preview-date">${new Date(file.createdDate).toLocaleString()}</div>
                </div>
                <div class="file-preview-actions">
                    ${file.isImage ? `<button class="file-action-btn view" data-action="view" data-file-id="${file.id}">
                        <i class="fas fa-eye"></i> View
                    </button>` : ''}
                    <button class="file-action-btn download" data-action="download" data-file-id="${file.id}">
                        <i class="fas fa-download"></i> Download
                    </button>
                    <button class="file-action-btn delete" data-action="delete" data-file-id="${file.id}">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Attach file action event listeners
     */
    attachFileActions(fileId) {
        const item = document.querySelector(`[data-file-id="${fileId}"]`);
        if (!item) return;

        const viewBtn = item.querySelector('[data-action="view"]');
        const downloadBtn = item.querySelector('[data-action="download"]');
        const deleteBtn = item.querySelector('[data-action="delete"]');

        if (viewBtn) {
            viewBtn.addEventListener('click', () => this.viewFile(fileId));
        }

        if (downloadBtn) {
            downloadBtn.addEventListener('click', () => this.downloadFile(fileId));
        }

        if (deleteBtn) {
            deleteBtn.addEventListener('click', () => this.deleteFile(fileId));
        }
    }

    /**
     * View file (open in modal for images)
     */
    async viewFile(fileId) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/${fileId}`, {
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to get file info');
            }

            const file = await response.json();

            if (file.isImage) {
                this.showImageModal(file.viewUrl);
            } else {
                window.open(file.viewUrl, '_blank');
            }

        } catch (error) {
            console.error('View file error:', error);
            this.showToast('Failed to view file', 'error');
        }
    }

    /**
     * Download file
     */
    async downloadFile(fileId) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/${fileId}`, {
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to get file info');
            }

            const file = await response.json();

            // Create temporary link and trigger download
            const link = document.createElement('a');
            link.href = file.downloadUrl;
            link.download = file.originalFilename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            this.showToast('Download started', 'success');

        } catch (error) {
            console.error('Download file error:', error);
            this.showToast('Failed to download file', 'error');
        }
    }

    /**
     * Delete file
     */
    async deleteFile(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) {
            return;
        }

        try {
            const response = await fetch(`${this.apiBaseUrl}/${fileId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to delete file');
            }

            this.showToast('File deleted successfully', 'success');
            this.loadUserFiles();
            this.loadStorageStats();

        } catch (error) {
            console.error('Delete file error:', error);
            this.showToast('Failed to delete file', 'error');
        }
    }

    /**
     * Load storage statistics
     */
    async loadStorageStats(userId = null) {
        try {
            if (!userId) {
                const user = UserManager.getCurrentUser();
                if (!user) return;
                userId = user.id;
            }

            const response = await fetch(`${this.apiBaseUrl}/storage-stats/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load storage stats');
            }

            const stats = await response.json();
            this.renderStorageStats(stats);

        } catch (error) {
            console.error('Load storage stats error:', error);
        }
    }

    /**
     * Render storage statistics
     */
    renderStorageStats(stats) {
        const container = document.getElementById('storage-stats');
        if (!container) return;

        const usedPercentage = stats.usedPercentage || 0;
        let barClass = '';
        if (usedPercentage > 90) barClass = 'danger';
        else if (usedPercentage > 70) barClass = 'warning';

        container.innerHTML = `
            <h3><i class="fas fa-database"></i> Storage Usage</h3>
            <div class="storage-bar">
                <div class="storage-bar-fill ${barClass}" style="width: ${usedPercentage}%"></div>
            </div>
            <div class="storage-info">
                <span>${stats.totalSizeFormatted} used of ${stats.maxSizeFormatted}</span>
                <span>${usedPercentage.toFixed(1)}% used</span>
            </div>
            <div class="storage-info" style="margin-top: 10px;">
                <span>${stats.fileCount} files</span>
            </div>
        `;
    }

    /**
     * Show image modal
     */
    showImageModal(imageUrl) {
        let modal = document.getElementById('image-modal');
        
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'image-modal';
            modal.className = 'image-modal';
            modal.innerHTML = `
                <span class="image-modal-close">&times;</span>
                <img src="" alt="Preview">
            `;
            document.body.appendChild(modal);

            // Close modal on click
            modal.querySelector('.image-modal-close').addEventListener('click', () => {
                modal.classList.remove('active');
            });
            
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.classList.remove('active');
                }
            });
        }

        modal.querySelector('img').src = imageUrl;
        modal.classList.add('active');
    }

    /**
     * Show upload progress
     */
    showUploadProgress() {
        this.uploadArea.classList.add('uploading');
        const progress = document.getElementById('upload-progress');
        if (progress) {
            progress.classList.add('active');
        }
    }

    /**
     * Hide upload progress
     */
    hideUploadProgress() {
        this.uploadArea.classList.remove('uploading');
        const progress = document.getElementById('upload-progress');
        if (progress) {
            progress.classList.remove('active');
        }
    }

    /**
     * Get file icon class
     */
    getFileIconClass(contentType) {
        if (contentType.startsWith('image/')) return 'image';
        if (contentType === 'application/pdf') return 'pdf';
        if (contentType.includes('word') || contentType.includes('document')) return 'doc';
        return 'default';
    }

    /**
     * Get file icon
     */
    getFileIcon(contentType) {
        if (contentType.startsWith('image/')) return 'fas fa-file-image';
        if (contentType === 'application/pdf') return 'fas fa-file-pdf';
        if (contentType.includes('word') || contentType.includes('document')) return 'fas fa-file-word';
        if (contentType.includes('excel') || contentType.includes('spreadsheet')) return 'fas fa-file-excel';
        if (contentType.includes('text')) return 'fas fa-file-alt';
        return 'fas fa-file';
    }

    /**
     * Format file size
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    /**
     * Show toast notification
     */
    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.remove();
        }, 3000);
    }
}

// Export for use in other scripts
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FileUploadManager;
}
