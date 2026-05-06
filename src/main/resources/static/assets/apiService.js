/**
 * API Service Module
 * 
 * Comprehensive JavaScript service for interacting with the Finance Tracker REST API.
 * 
 * Features:
 * - Centralized API configuration
 * - JWT token management (auto-attach to requests)
 * - Automatic token refresh on 401 errors
 * - Request/response interceptors
 * - Error handling utilities
 * - Authentication methods
 * - User management methods
 * - Support ticket methods
 * - Password reset methods
 * 
 * Usage:
 * import apiService from './apiService.js';
 * 
 * // Login
 * const response = await apiService.auth.login('username', 'password');
 * 
 * // Get profile
 * const profile = await apiService.user.getProfile();
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */

// ==================== Configuration ====================

const API_CONFIG = {
    // Base API URL - change for different environments
    BASE_URL: 'http://localhost:8080/api',
    
    // Timeout for API requests (milliseconds)
    TIMEOUT: 30000,
    
    // Token storage keys
    TOKEN_KEY: 'jwt_access_token',
    REFRESH_TOKEN_KEY: 'jwt_refresh_token',
    USER_KEY: 'user_data',
    
    // Token refresh settings
    ENABLE_AUTO_REFRESH: true,
    MAX_REFRESH_RETRIES: 1
};

// ==================== Token Management ====================

const TokenManager = {
    /**
     * Gets the access token from localStorage.
     * @returns {string|null} Access token or null
     */
    getAccessToken() {
        return localStorage.getItem(API_CONFIG.TOKEN_KEY);
    },

    /**
     * Gets the refresh token from localStorage.
     * @returns {string|null} Refresh token or null
     */
    getRefreshToken() {
        return localStorage.getItem(API_CONFIG.REFRESH_TOKEN_KEY);
    },

    /**
     * Saves tokens to localStorage.
     * @param {string} accessToken - Access token
     * @param {string} refreshToken - Refresh token (optional)
     */
    saveTokens(accessToken, refreshToken = null) {
        if (accessToken) {
            localStorage.setItem(API_CONFIG.TOKEN_KEY, accessToken);
        }
        if (refreshToken) {
            localStorage.setItem(API_CONFIG.REFRESH_TOKEN_KEY, refreshToken);
        }
    },

    /**
     * Saves user data to localStorage.
     * @param {Object} userData - User data object
     */
    saveUserData(userData) {
        localStorage.setItem(API_CONFIG.USER_KEY, JSON.stringify(userData));
    },

    /**
     * Gets user data from localStorage.
     * @returns {Object|null} User data or null
     */
    getUserData() {
        const userData = localStorage.getItem(API_CONFIG.USER_KEY);
        return userData ? JSON.parse(userData) : null;
    },

    /**
     * Clears all tokens and user data.
     */
    clearAll() {
        localStorage.removeItem(API_CONFIG.TOKEN_KEY);
        localStorage.removeItem(API_CONFIG.REFRESH_TOKEN_KEY);
        localStorage.removeItem(API_CONFIG.USER_KEY);
    },

    /**
     * Checks if user is authenticated (has access token).
     * @returns {boolean}
     */
    isAuthenticated() {
        return !!this.getAccessToken();
    }
};

// ==================== HTTP Client ====================

class HttpClient {
    constructor() {
        this.isRefreshing = false;
        this.refreshSubscribers = [];
    }

    /**
     * Makes an HTTP request using Fetch API.
     * @param {string} url - Request URL
     * @param {Object} options - Fetch options
     * @returns {Promise<any>} Response data
     */
    async request(url, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        // Add authorization header if token exists
        const token = TokenManager.getAccessToken();
        if (token && !config.skipAuth) {
            config.headers['Authorization'] = `Bearer ${token}`;
            console.log(`[HTTP] Request to ${url} with token: ${token.substring(0, 20)}...`);
        } else if (!config.skipAuth) {
            console.warn(`[HTTP] No token found for request to ${url}`);
            console.log('[HTTP] localStorage jwt_access_token:', localStorage.getItem('jwt_access_token'));
            console.log('[HTTP] localStorage jwtToken:', localStorage.getItem('jwtToken'));
        }

        try {
            const response = await fetch(`${API_CONFIG.BASE_URL}${url}`, config);
            console.log(`[HTTP] Response from ${url}: ${response.status} ${response.statusText}`);
            
            // Handle 401 Unauthorized - try to refresh token
            if (response.status === 401 && !config.skipAuth && API_CONFIG.ENABLE_AUTO_REFRESH) {
                return await this.handleUnauthorized(url, options);
            }

            // Parse response
            const data = await this.parseResponse(response);

            // Handle non-2xx responses
            if (!response.ok) {
                throw this.createError(response.status, data);
            }

            // Wrap response in success format for consistency
            return {
                success: true,
                data: data,
                status: response.status
            };

        } catch (error) {
            // Re-throw API errors with success flag
            if (error.status) {
                throw {
                    success: false,
                    ...error
                };
            }
            
            // Network or other errors
            throw {
                success: false,
                status: 0,
                message: 'Network error. Please check your connection.',
                error: 'NetworkError',
                originalError: error
            };
        }
    }

    /**
     * Parses response based on content type.
     * @param {Response} response - Fetch response
     * @returns {Promise<any>}
     */
    async parseResponse(response) {
        const contentType = response.headers.get('content-type');
        
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }
        
        const text = await response.text();
        return text || null;
    }

    /**
     * Creates a standardized error object.
     * @param {number} status - HTTP status code
     * @param {any} data - Error data
     * @returns {Object} Error object
     */
    createError(status, data) {
        return {
            status: status,
            message: data?.message || data?.error || 'An error occurred',
            error: data?.error || 'Error',
            timestamp: data?.timestamp,
            path: data?.path,
            fieldErrors: data?.fieldErrors,
            details: data?.details
        };
    }

    /**
     * Handles 401 Unauthorized by attempting token refresh.
     * @param {string} url - Original request URL
     * @param {Object} options - Original request options
     * @returns {Promise<any>}
     */
    async handleUnauthorized(url, options) {
        // If already refreshing, wait for it
        if (this.isRefreshing) {
            return new Promise((resolve, reject) => {
                this.refreshSubscribers.push({ resolve, reject, url, options });
            });
        }

        this.isRefreshing = true;

        try {
            // Attempt token refresh
            const refreshToken = TokenManager.getRefreshToken();
            
            if (!refreshToken) {
                throw new Error('No refresh token available');
            }

            const response = await fetch(`${API_CONFIG.BASE_URL}/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (!response.ok) {
                throw new Error('Token refresh failed');
            }

            const data = await response.json();
            
            // Save new tokens
            TokenManager.saveTokens(data.accessToken, data.refreshToken);
            
            // Retry all queued requests
            this.refreshSubscribers.forEach(subscriber => {
                subscriber.resolve(this.request(subscriber.url, subscriber.options));
            });
            this.refreshSubscribers = [];

            // Retry original request
            return await this.request(url, options);

        } catch (error) {
            // Refresh failed - clear tokens and redirect to login
            this.refreshSubscribers.forEach(subscriber => {
                subscriber.reject(error);
            });
            this.refreshSubscribers = [];
            
            TokenManager.clearAll();
            
            // Redirect to login page
            if (typeof window !== 'undefined') {
                window.location.href = '/login.html';
            }
            
            throw {
                status: 401,
                message: 'Session expired. Please log in again.',
                error: 'Unauthorized'
            };
        } finally {
            this.isRefreshing = false;
        }
    }

    // HTTP Methods

    async get(url, options = {}) {
        return this.request(url, { ...options, method: 'GET' });
    }

    async post(url, data, options = {}) {
        return this.request(url, {
            ...options,
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async put(url, data, options = {}) {
        return this.request(url, {
            ...options,
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async patch(url, data, options = {}) {
        return this.request(url, {
            ...options,
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }

    async delete(url, options = {}) {
        return this.request(url, { ...options, method: 'DELETE' });
    }
}

// Create HTTP client instance
const httpClient = new HttpClient();

// ==================== Authentication API ====================

const authAPI = {
    /**
     * Authenticates user with username and password.
     * @param {string} username - Username
     * @param {string} password - Password
     * @returns {Promise<Object>} User data with tokens
     */
    async login(username, password) {
        const response = await httpClient.post('/auth/login', 
            { username, password },
            { skipAuth: true }
        );
        
        // Extract data from wrapped response
        const data = response.data || response;
        
        // Save tokens and user data
        const accessToken = data.accessToken || data.token; // Handle both formats
        const refreshToken = data.refreshToken;
        
        TokenManager.saveTokens(accessToken, refreshToken);
        TokenManager.saveUserData({
            id: data.id,
            username: data.username,
            email: data.email,
            roles: data.roles
        });
        
        console.log('[AUTH] Login successful, tokens saved:', {
            hasAccessToken: !!accessToken,
            hasRefreshToken: !!refreshToken,
            username: data.username
        });
        
        return response;
    },

    /**
     * Registers a new user.
     * @param {Object} userData - User registration data
     * @returns {Promise<Object>} Registration response
     */
    async register(userData) {
        return await httpClient.post('/auth/register', userData, { skipAuth: true });
    },

    /**
     * Registers a new user (legacy signup endpoint).
     * @param {Object} userData - User registration data
     * @returns {Promise<Object>} Registration response
     */
    async signup(userData) {
        return await httpClient.post('/auth/signup', userData, { skipAuth: true });
    },

    /**
     * Refreshes the access token using refresh token.
     * @returns {Promise<Object>} New tokens
     */
    async refreshToken() {
        const refreshToken = TokenManager.getRefreshToken();
        
        if (!refreshToken) {
            throw new Error('No refresh token available');
        }

        const response = await httpClient.post('/auth/refresh', 
            { refreshToken },
            { skipAuth: true }
        );
        
        // Save new tokens
        TokenManager.saveTokens(response.accessToken, response.refreshToken);
        
        return response;
    },

    /**
     * Logs out the current user.
     * @returns {Promise<void>}
     */
    async logout() {
        try {
            await httpClient.post('/auth/logout', {});
        } finally {
            // Clear tokens even if request fails
            TokenManager.clearAll();
            
            // Redirect to login page
            if (typeof window !== 'undefined') {
                window.location.href = '/login.html';
            }
        }
    },

    /**
     * Gets current authenticated user details.
     * @returns {Promise<Object>} Current user data
     */
    async getCurrentUser() {
        return await httpClient.get('/auth/me');
    },

    /**
     * Checks if user is authenticated.
     * @returns {boolean}
     */
    isAuthenticated() {
        return TokenManager.isAuthenticated();
    },

    /**
     * Gets stored user data.
     * @returns {Object|null} User data or null
     */
    getUserData() {
        return TokenManager.getUserData();
    }
};

// ==================== User API ====================

const userAPI = {
    /**
     * Gets all users (admin only).
     * @returns {Promise<Array>} List of users
     */
    async getAllUsers() {
        return await httpClient.get('/users');
    },

    /**
     * Gets user by ID.
     * @param {number} userId - User ID
     * @returns {Promise<Object>} User data
     */
    async getUserById(userId) {
        return await httpClient.get(`/users/${userId}`);
    },

    /**
     * Gets current user's profile.
     * @returns {Promise<Object>} User profile
     */
    async getProfile() {
        return await httpClient.get('/users/profile');
    },

    /**
     * Updates current user's profile.
     * @param {Object} profileData - Profile data (firstName, lastName, mobileNumber)
     * @returns {Promise<Object>} Updated profile
     */
    async updateProfile(profileData) {
        return await httpClient.put('/users/profile', profileData);
    },

    /**
     * Changes current user's password.
     * @param {string} currentPassword - Current password
     * @param {string} newPassword - New password
     * @param {string} confirmPassword - Confirm new password
     * @returns {Promise<Object>} Success message
     */
    async changePassword(currentPassword, newPassword, confirmPassword) {
        return await httpClient.post('/users/change-password', {
            currentPassword,
            newPassword,
            confirmPassword
        });
    },

    /**
     * Activates a user account (admin only).
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Success message
     */
    async activateUser(userId) {
        return await httpClient.put(`/users/${userId}/activate`, {});
    },

    /**
     * Deactivates a user account (admin only).
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Success message
     */
    async deactivateUser(userId) {
        return await httpClient.put(`/users/${userId}/deactivate`, {});
    },

    /**
     * Locks a user account (admin only).
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Success message
     */
    async lockUser(userId) {
        return await httpClient.put(`/users/${userId}/lock`, {});
    },

    /**
     * Unlocks a user account (admin only).
     * @param {number} userId - User ID
     * @returns {Promise<Object>} Success message
     */
    async unlockUser(userId) {
        return await httpClient.put(`/users/${userId}/unlock`, {});
    },

    /**
     * Deletes a user account (admin only).
     * @param {number} userId - User ID
     * @returns {Promise<void>}
     */
    async deleteUser(userId) {
        return await httpClient.delete(`/users/${userId}`);
    }
};

// ==================== Support Ticket API ====================

const supportAPI = {
    /**
     * Creates a new support ticket (public endpoint).
     * @param {Object} ticketData - Ticket data
     * @returns {Promise<Object>} Created ticket
     */
    async createTicket(ticketData) {
        return await httpClient.post('/support/tickets', ticketData, { skipAuth: true });
    },

    /**
     * Creates a support ticket for authenticated user.
     * @param {Object} ticketData - Ticket data
     * @returns {Promise<Object>} Created ticket
     */
    async createTicketForUser(ticketData) {
        return await httpClient.post('/support/ticket/user', ticketData);
    },

    /**
     * Gets all support tickets (user's own or all if admin).
     * @returns {Promise<Array>} List of tickets
     */
    async getAllTickets() {
        return await httpClient.get('/support/tickets');
    },

    /**
     * Gets user's own support tickets.
     * @returns {Promise<Array>} List of user's tickets
     */
    async getMyTickets() {
        return await httpClient.get('/support/tickets');
    },

    /**
     * Gets support ticket by ID.
     * @param {number} ticketId - Ticket ID
     * @returns {Promise<Object>} Ticket details
     */
    async getTicketById(ticketId) {
        return await httpClient.get(`/support/tickets/${ticketId}`);
    },

    /**
     * Gets support ticket by ticket number (public endpoint).
     * @param {string} ticketNumber - Ticket number (e.g., TKT-1730628000000)
     * @returns {Promise<Object>} Ticket details
     */
    async getTicketByNumber(ticketNumber) {
        return await httpClient.get(`/support/ticket/${ticketNumber}`, { skipAuth: true });
    },

    /**
     * Updates a support ticket (admin only).
     * @param {number} ticketId - Ticket ID
     * @param {Object} updateData - Update data
     * @returns {Promise<Object>} Updated ticket
     */
    async updateTicket(ticketId, updateData) {
        return await httpClient.put(`/support/tickets/${ticketId}`, updateData);
    },

    /**
     * Assigns a ticket to an agent (admin only).
     * @param {number} ticketId - Ticket ID
     * @param {string} assignedTo - Agent username/ID
     * @returns {Promise<Object>} Updated ticket
     */
    async assignTicket(ticketId, assignedTo) {
        return await httpClient.put(`/support/ticket/${ticketId}/assign`, { assignedTo });
    },

    /**
     * Resolves a support ticket (admin only).
     * @param {number} ticketId - Ticket ID
     * @param {string} resolutionNotes - Resolution notes
     * @returns {Promise<Object>} Updated ticket
     */
    async resolveTicket(ticketId, resolutionNotes) {
        return await httpClient.put(`/support/ticket/${ticketId}/resolve`, { resolutionNotes });
    },

    /**
     * Closes a support ticket (admin only).
     * @param {number} ticketId - Ticket ID
     * @returns {Promise<Object>} Updated ticket
     */
    async closeTicket(ticketId) {
        return await httpClient.put(`/support/ticket/${ticketId}/close`, {});
    },

    /**
     * Gets tickets by status.
     * @param {string} status - Ticket status (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
     * @returns {Promise<Array>} List of tickets
     */
    async getTicketsByStatus(status) {
        return await httpClient.get(`/support/tickets/status/${status}`);
    },

    /**
     * Gets count of open tickets.
     * @returns {Promise<number>} Count of open tickets
     */
    async getOpenTicketsCount() {
        return await httpClient.get('/support/tickets/count/open');
    }
};

// ==================== Password Reset API ====================

const passwordResetAPI = {
    /**
     * Requests a password reset token.
     * @param {string} email - User's email address
     * @returns {Promise<Object>} Success message
     */
    async requestReset(email) {
        return await httpClient.post('/password-reset/request', 
            { email },
            { skipAuth: true }
        );
    },

    /**
     * Requests a password reset (legacy endpoint).
     * @param {string} email - User's email address
     * @returns {Promise<Object>} Success message
     */
    async forgotPassword(email) {
        return await httpClient.post('/password-reset/forgot-password',
            { email },
            { skipAuth: true }
        );
    },

    /**
     * Validates a password reset token.
     * @param {string} token - Reset token
     * @returns {Promise<Object>} Validation result
     */
    async validateToken(token) {
        return await httpClient.get(`/password-reset/validate?token=${token}`, { skipAuth: true });
    },

    /**
     * Resets password using token.
     * @param {string} token - Reset token
     * @param {string} newPassword - New password
     * @param {string} confirmPassword - Confirm new password
     * @returns {Promise<Object>} Success message
     */
    async resetPassword(token, newPassword, confirmPassword) {
        return await httpClient.post('/password-reset/reset',
            { token, newPassword, confirmPassword },
            { skipAuth: true }
        );
    }
};

// ==================== Transaction API ====================

const transactionAPI = {
    /**
     * Creates a new transaction for the authenticated user.
     * @param {Object} transactionData - Transaction data
     * @param {string} transactionData.description - Transaction description
     * @param {number} transactionData.amount - Transaction amount
     * @param {string} transactionData.type - Transaction type (INCOME or EXPENSE)
     * @param {string} transactionData.date - Transaction date (YYYY-MM-DD format)
     * @returns {Promise<Object>} Created transaction
     */
    async createTransaction(transactionData) {
        return await httpClient.post('/transactions', transactionData);
    },

    /**
     * Gets all transactions for the authenticated user.
     * @returns {Promise<Array>} List of user's transactions
     */
    async getAllTransactions() {
        return await httpClient.get('/transactions');
    },

    /**
     * Gets a specific transaction by ID.
     * @param {number} transactionId - Transaction ID
     * @returns {Promise<Object>} Transaction details
     */
    async getTransactionById(transactionId) {
        return await httpClient.get(`/transactions/${transactionId}`);
    },

    /**
     * Updates an existing transaction.
     * @param {number} transactionId - Transaction ID
     * @param {Object} transactionData - Updated transaction data
     * @returns {Promise<Object>} Updated transaction
     */
    async updateTransaction(transactionId, transactionData) {
        return await httpClient.put(`/transactions/${transactionId}`, transactionData);
    },

    /**
     * Deletes a transaction.
     * @param {number} transactionId - Transaction ID
     * @returns {Promise<void>} Success (no content)
     */
    async deleteTransaction(transactionId) {
        return await httpClient.delete(`/transactions/${transactionId}`);
    },

    /**
     * Gets user's transaction statistics (total income, expense, balance).
     * @returns {Promise<Object>} Statistics object with totalIncome, totalExpense, balance
     */
    async getStatistics() {
        return await httpClient.get('/transactions/statistics');
    }
};

// ==================== Error Handling Utilities ====================

const ErrorHandler = {
    /**
     * Extracts error message from error object.
     * @param {any} error - Error object
     * @returns {string} Error message
     */
    getMessage(error) {
        if (typeof error === 'string') {
            return error;
        }
        
        if (error?.message) {
            return error.message;
        }
        
        return 'An unexpected error occurred';
    },

    /**
     * Gets field-specific validation errors.
     * @param {Object} error - Error object
     * @returns {Object|null} Field errors map or null
     */
    getFieldErrors(error) {
        if (error?.fieldErrors) {
            // Convert array to object map
            const fieldMap = {};
            error.fieldErrors.forEach(fieldError => {
                fieldMap[fieldError.field] = fieldError.message;
            });
            return fieldMap;
        }
        return null;
    },

    /**
     * Checks if error is a validation error.
     * @param {Object} error - Error object
     * @returns {boolean}
     */
    isValidationError(error) {
        return error?.status === 400 && error?.fieldErrors;
    },

    /**
     * Checks if error is an authentication error.
     * @param {Object} error - Error object
     * @returns {boolean}
     */
    isAuthError(error) {
        return error?.status === 401;
    },

    /**
     * Checks if error is a forbidden error.
     * @param {Object} error - Error object
     * @returns {boolean}
     */
    isForbiddenError(error) {
        return error?.status === 403;
    },

    /**
     * Checks if error is a not found error.
     * @param {Object} error - Error object
     * @returns {boolean}
     */
    isNotFoundError(error) {
        return error?.status === 404;
    },

    /**
     * Checks if error is a conflict error.
     * @param {Object} error - Error object
     * @returns {boolean}
     */
    isConflictError(error) {
        return error?.status === 409;
    },

    /**
     * Displays error in console (development).
     * @param {any} error - Error object
     * @param {string} context - Context where error occurred
     */
    logError(error, context = '') {
        if (process?.env?.NODE_ENV === 'development') {
            console.error(`[API Error${context ? ` - ${context}` : ''}]:`, error);
        }
    },

    /**
     * Shows user-friendly error message (if DOM available).
     * @param {any} error - Error object
     * @param {string} elementId - ID of element to display error
     */
    displayError(error, elementId = 'error-message') {
        if (typeof document === 'undefined') return;
        
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = this.getMessage(error);
            element.style.display = 'block';
        }
    }
};

// ==================== API Service (Main Export) ====================

// ==================== Admin API ====================

const adminAPI = {
    /**
     * Get dashboard statistics.
     * @returns {Promise<Object>}
     */
    async getDashboardStats() {
        return await HttpClient.get('/admin/stats/dashboard');
    },

    /**
     * Get all users with pagination.
     * @param {number} page - Page number
     * @param {number} size - Page size
     * @param {string} sortBy - Field to sort by
     * @param {string} direction - Sort direction (ASC/DESC)
     * @returns {Promise<Object>}
     */
    async getAllUsers(page = 0, size = 10, sortBy = 'id', direction = 'ASC') {
        return await HttpClient.get(`/admin/users?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`);
    },

    /**
     * Search users.
     * @param {string} query - Search query
     * @param {number} page - Page number
     * @param {number} size - Page size
     * @returns {Promise<Object>}
     */
    async searchUsers(query, page = 0, size = 10) {
        return await HttpClient.get(`/admin/users/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    },

    /**
     * Get user by ID.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async getUserById(userId) {
        return await HttpClient.get(`/admin/users/${userId}`);
    },

    /**
     * Get user statistics.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async getUserStats(userId) {
        return await HttpClient.get(`/admin/users/${userId}/stats`);
    },

    /**
     * Update user roles.
     * @param {number} userId - User ID
     * @param {Array<number>} roles - Array of role IDs
     * @returns {Promise<Object>}
     */
    async updateUserRoles(userId, roles) {
        return await HttpClient.put(`/admin/users/${userId}/roles`, roles);
    },

    /**
     * Activate user.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async activateUser(userId) {
        return await HttpClient.put(`/admin/users/${userId}/activate`);
    },

    /**
     * Deactivate user.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async deactivateUser(userId) {
        return await HttpClient.put(`/admin/users/${userId}/deactivate`);
    },

    /**
     * Lock user account.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async lockUser(userId) {
        return await HttpClient.put(`/admin/users/${userId}/lock`);
    },

    /**
     * Unlock user account.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async unlockUser(userId) {
        return await HttpClient.put(`/admin/users/${userId}/unlock`);
    },

    /**
     * Delete user.
     * @param {number} userId - User ID
     * @returns {Promise<Object>}
     */
    async deleteUser(userId) {
        return await HttpClient.delete(`/admin/users/${userId}`);
    },

    /**
     * Get all tickets with filtering.
     * @param {number} page - Page number
     * @param {number} size - Page size
     * @param {string} sortBy - Field to sort by
     * @param {string} direction - Sort direction
     * @param {string} status - Filter by status
     * @param {string} priority - Filter by priority
     * @returns {Promise<Object>}
     */
    async getAllTickets(page = 0, size = 10, sortBy = 'createdDate', direction = 'DESC', status = null, priority = null) {
        let url = `/admin/tickets?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`;
        if (status) url += `&status=${status}`;
        if (priority) url += `&priority=${priority}`;
        return await HttpClient.get(url);
    },

    /**
     * Search tickets.
     * @param {string} query - Search query
     * @param {number} page - Page number
     * @param {number} size - Page size
     * @returns {Promise<Object>}
     */
    async searchTickets(query, page = 0, size = 10) {
        return await HttpClient.get(`/admin/tickets/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    },

    /**
     * Get ticket by ID.
     * @param {number} ticketId - Ticket ID
     * @returns {Promise<Object>}
     */
    async getTicketById(ticketId) {
        return await HttpClient.get(`/admin/tickets/${ticketId}`);
    },

    /**
     * Update ticket status.
     * @param {number} ticketId - Ticket ID
     * @param {string} status - New status
     * @returns {Promise<Object>}
     */
    async updateTicketStatus(ticketId, status) {
        return await HttpClient.put(`/admin/tickets/${ticketId}/status?status=${status}`);
    },

    /**
     * Update ticket priority.
     * @param {number} ticketId - Ticket ID
     * @param {string} priority - New priority
     * @returns {Promise<Object>}
     */
    async updateTicketPriority(ticketId, priority) {
        return await HttpClient.put(`/admin/tickets/${ticketId}/priority?priority=${priority}`);
    },

    /**
     * Assign ticket.
     * @param {number} ticketId - Ticket ID
     * @param {string} assignee - Assignee name
     * @returns {Promise<Object>}
     */
    async assignTicket(ticketId, assignee) {
        return await HttpClient.put(`/admin/tickets/${ticketId}/assign?assignee=${encodeURIComponent(assignee)}`);
    },

    /**
     * Resolve ticket.
     * @param {number} ticketId - Ticket ID
     * @param {string} resolutionNotes - Resolution notes
     * @returns {Promise<Object>}
     */
    async resolveTicket(ticketId, resolutionNotes) {
        return await HttpClient.put(`/admin/tickets/${ticketId}/resolve`, resolutionNotes);
    },

    /**
     * Close ticket.
     * @param {number} ticketId - Ticket ID
     * @returns {Promise<Object>}
     */
    async closeTicket(ticketId) {
        return await HttpClient.put(`/admin/tickets/${ticketId}/close`);
    },

    /**
     * Delete ticket.
     * @param {number} ticketId - Ticket ID
     * @returns {Promise<Object>}
     */
    async deleteTicket(ticketId) {
        return await HttpClient.delete(`/admin/tickets/${ticketId}`);
    },

    /**
     * Get user statistics.
     * @returns {Promise<Object>}
     */
    async getUserStatistics() {
        return await HttpClient.get('/admin/stats/users');
    },

    /**
     * Get ticket statistics.
     * @returns {Promise<Object>}
     */
    async getTicketStatistics() {
        return await HttpClient.get('/admin/stats/tickets');
    },

    /**
     * Get tickets by status.
     * @returns {Promise<Object>}
     */
    async getTicketsByStatus() {
        return await HttpClient.get('/admin/stats/tickets/status');
    },

    /**
     * Get tickets by priority.
     * @returns {Promise<Object>}
     */
    async getTicketsByPriority() {
        return await HttpClient.get('/admin/stats/tickets/priority');
    },

    /**
     * Get recent activity.
     * @param {number} limit - Number of items to fetch
     * @returns {Promise<Object>}
     */
    async getRecentActivity(limit = 10) {
        return await HttpClient.get(`/admin/stats/activity?limit=${limit}`);
    },

    /**
     * Get all roles.
     * @returns {Promise<Object>}
     */
    async getAllRoles() {
        return await HttpClient.get('/admin/roles');
    }
};

// ==================== API Service (Main Export) ====================

const apiService = {
    // API modules
    auth: authAPI,
    user: userAPI,
    support: supportAPI,
    passwordReset: passwordResetAPI,
    admin: adminAPI,
    transactions: transactionAPI,
    
    // Utilities
    token: TokenManager,
    error: ErrorHandler,
    
    // Configuration
    config: API_CONFIG,
    
    /**
     * Updates base URL (for different environments).
     * @param {string} baseUrl - New base URL
     */
    setBaseUrl(baseUrl) {
        API_CONFIG.BASE_URL = baseUrl;
    },
    
    /**
     * Updates timeout.
     * @param {number} timeout - Timeout in milliseconds
     */
    setTimeout(timeout) {
        API_CONFIG.TIMEOUT = timeout;
    },
    
    /**
     * Enables or disables automatic token refresh.
     * @param {boolean} enable - Enable auto refresh
     */
    setAutoRefresh(enable) {
        API_CONFIG.ENABLE_AUTO_REFRESH = enable;
    }
};

// Export for different module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = apiService;
}

// Always expose to window for browser usage
if (typeof window !== 'undefined') {
    window.apiService = apiService;
}

// ES6 export (if supported) - commented out to prevent module conflicts in browser
// export default apiService;
