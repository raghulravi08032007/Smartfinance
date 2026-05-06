/**
 * Authentication State Management Module
 * 
 * Manages user authentication state, token storage, session management,
 * and route protection for the Finance Tracker application.
 * 
 * Features:
 * - JWT token storage (localStorage/sessionStorage)
 * - User information persistence
 * - Authentication status checking
 * - Auto-logout on token expiration
 * - Unauthorized access handling
 * - Route protection
 * - Session timeout management
 * - Remember me functionality
 * 
 * Usage:
 * import AuthManager from './auth.js';
 * 
 * // Initialize on page load
 * AuthManager.init();
 * 
 * // Check authentication
 * if (AuthManager.isAuthenticated()) {
 *     // User is logged in
 * }
 * 
 * // Protect page
 * AuthManager.requireAuth();
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */

// ==================== Configuration ====================

const AUTH_CONFIG = {
    // Storage keys
    ACCESS_TOKEN_KEY: 'jwt_access_token',
    REFRESH_TOKEN_KEY: 'jwt_refresh_token',
    USER_DATA_KEY: 'user_data',
    REMEMBER_ME_KEY: 'remember_me',
    TOKEN_EXPIRY_KEY: 'token_expiry',
    LAST_ACTIVITY_KEY: 'last_activity',
    
    // Pages
    LOGIN_PAGE: '/login.html',
    HOME_PAGE: '/index.html',
    DASHBOARD_PAGE: '/index.html',
    
    // Public pages that don't require authentication
    PUBLIC_PAGES: [
        '/login.html',
        '/sign-up.html',
        '/forgot-password.html',
        '/support.html',
        '/index.html'
    ],
    
    // Session settings
    SESSION_TIMEOUT: 30 * 60 * 1000, // 30 minutes in milliseconds
    TOKEN_CHECK_INTERVAL: 60 * 1000, // Check token every 1 minute
    ACTIVITY_EVENTS: ['mousedown', 'keydown', 'scroll', 'touchstart'],
    
    // Redirect settings
    REDIRECT_PARAM: 'redirect',
    ENABLE_AUTO_REDIRECT: true
};

// ==================== Storage Manager ====================

const StorageManager = {
    /**
     * Gets the appropriate storage based on remember me setting.
     * @returns {Storage} localStorage or sessionStorage
     */
    getStorage() {
        const rememberMe = localStorage.getItem(AUTH_CONFIG.REMEMBER_ME_KEY);
        return rememberMe === 'true' ? localStorage : sessionStorage;
    },

    /**
     * Saves data to appropriate storage.
     * @param {string} key - Storage key
     * @param {string} value - Value to store
     */
    set(key, value) {
        try {
            this.getStorage().setItem(key, value);
        } catch (error) {
            console.error('Error saving to storage:', error);
        }
    },

    /**
     * Gets data from storage (checks both localStorage and sessionStorage).
     * @param {string} key - Storage key
     * @returns {string|null} Stored value or null
     */
    get(key) {
        try {
            return this.getStorage().getItem(key) || 
                   localStorage.getItem(key) || 
                   sessionStorage.getItem(key);
        } catch (error) {
            console.error('Error reading from storage:', error);
            return null;
        }
    },

    /**
     * Removes data from both storage types.
     * @param {string} key - Storage key
     */
    remove(key) {
        try {
            localStorage.removeItem(key);
            sessionStorage.removeItem(key);
        } catch (error) {
            console.error('Error removing from storage:', error);
        }
    },

    /**
     * Clears all authentication data.
     */
    clearAll() {
        const keys = [
            AUTH_CONFIG.ACCESS_TOKEN_KEY,
            AUTH_CONFIG.REFRESH_TOKEN_KEY,
            AUTH_CONFIG.USER_DATA_KEY,
            AUTH_CONFIG.TOKEN_EXPIRY_KEY,
            AUTH_CONFIG.LAST_ACTIVITY_KEY
        ];
        
        keys.forEach(key => this.remove(key));
    },

    /**
     * Sets remember me preference.
     * @param {boolean} remember - Remember me flag
     */
    setRememberMe(remember) {
        localStorage.setItem(AUTH_CONFIG.REMEMBER_ME_KEY, remember.toString());
    },

    /**
     * Gets remember me preference.
     * @returns {boolean}
     */
    getRememberMe() {
        return localStorage.getItem(AUTH_CONFIG.REMEMBER_ME_KEY) === 'true';
    }
};

// ==================== Token Manager ====================

const TokenManager = {
    /**
     * Saves authentication tokens.
     * @param {string} accessToken - Access token
     * @param {string} refreshToken - Refresh token (optional)
     * @param {boolean} rememberMe - Remember me flag
     */
    saveTokens(accessToken, refreshToken = null, rememberMe = false) {
        StorageManager.setRememberMe(rememberMe);
        
        if (accessToken) {
            StorageManager.set(AUTH_CONFIG.ACCESS_TOKEN_KEY, accessToken);
            
            // Calculate and store token expiry
            const expiry = this.calculateTokenExpiry(accessToken);
            if (expiry) {
                StorageManager.set(AUTH_CONFIG.TOKEN_EXPIRY_KEY, expiry.toString());
            }
        }
        
        if (refreshToken) {
            StorageManager.set(AUTH_CONFIG.REFRESH_TOKEN_KEY, refreshToken);
        }
        
        // Update last activity
        this.updateLastActivity();
    },

    /**
     * Gets the access token.
     * @returns {string|null} Access token or null
     */
    getAccessToken() {
        return StorageManager.get(AUTH_CONFIG.ACCESS_TOKEN_KEY);
    },

    /**
     * Gets the refresh token.
     * @returns {string|null} Refresh token or null
     */
    getRefreshToken() {
        return StorageManager.get(AUTH_CONFIG.REFRESH_TOKEN_KEY);
    },

    /**
     * Calculates token expiry time from JWT.
     * @param {string} token - JWT token
     * @returns {number|null} Expiry timestamp or null
     */
    calculateTokenExpiry(token) {
        try {
            const payload = this.parseJWT(token);
            if (payload && payload.exp) {
                return payload.exp * 1000; // Convert to milliseconds
            }
        } catch (error) {
            console.error('Error parsing token:', error);
        }
        return null;
    },

    /**
     * Parses JWT token to extract payload.
     * @param {string} token - JWT token
     * @returns {Object|null} Token payload or null
     */
    parseJWT(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error('Error parsing JWT:', error);
            return null;
        }
    },

    /**
     * Checks if token is expired.
     * @returns {boolean} True if expired
     */
    isTokenExpired() {
        const expiry = StorageManager.get(AUTH_CONFIG.TOKEN_EXPIRY_KEY);
        
        if (!expiry) {
            return true;
        }
        
        const expiryTime = parseInt(expiry, 10);
        const currentTime = Date.now();
        
        // Add 1 minute buffer
        return currentTime >= (expiryTime - 60000);
    },

    /**
     * Gets token expiry time.
     * @returns {Date|null} Expiry date or null
     */
    getTokenExpiry() {
        const expiry = StorageManager.get(AUTH_CONFIG.TOKEN_EXPIRY_KEY);
        return expiry ? new Date(parseInt(expiry, 10)) : null;
    },

    /**
     * Gets time remaining until token expires.
     * @returns {number} Time in milliseconds
     */
    getTimeUntilExpiry() {
        const expiry = this.getTokenExpiry();
        if (!expiry) return 0;
        
        const remaining = expiry.getTime() - Date.now();
        return Math.max(0, remaining);
    },

    /**
     * Updates last activity timestamp.
     */
    updateLastActivity() {
        StorageManager.set(AUTH_CONFIG.LAST_ACTIVITY_KEY, Date.now().toString());
    },

    /**
     * Checks if session has timed out due to inactivity.
     * @returns {boolean} True if timed out
     */
    isSessionTimedOut() {
        const lastActivity = StorageManager.get(AUTH_CONFIG.LAST_ACTIVITY_KEY);
        
        if (!lastActivity) {
            return false;
        }
        
        const lastActivityTime = parseInt(lastActivity, 10);
        const currentTime = Date.now();
        const timeSinceActivity = currentTime - lastActivityTime;
        
        return timeSinceActivity > AUTH_CONFIG.SESSION_TIMEOUT;
    },

    /**
     * Clears all tokens.
     */
    clearTokens() {
        StorageManager.clearAll();
    }
};

// ==================== User Manager ====================

const UserManager = {
    /**
     * Saves user information.
     * @param {Object} userData - User data object
     */
    saveUser(userData) {
        try {
            const userInfo = {
                id: userData.id,
                username: userData.username,
                email: userData.email,
                firstName: userData.firstName || '',
                lastName: userData.lastName || '',
                roles: userData.roles || [],
                savedAt: Date.now()
            };
            
            StorageManager.set(
                AUTH_CONFIG.USER_DATA_KEY, 
                JSON.stringify(userInfo)
            );
        } catch (error) {
            console.error('Error saving user data:', error);
        }
    },

    /**
     * Gets stored user information.
     * @returns {Object|null} User data or null
     */
    getUser() {
        try {
            const userData = StorageManager.get(AUTH_CONFIG.USER_DATA_KEY);
            return userData ? JSON.parse(userData) : null;
        } catch (error) {
            console.error('Error retrieving user data:', error);
            return null;
        }
    },

    /**
     * Updates user information.
     * @param {Object} updates - User data updates
     */
    updateUser(updates) {
        const currentUser = this.getUser();
        if (currentUser) {
            const updatedUser = { ...currentUser, ...updates };
            this.saveUser(updatedUser);
        }
    },

    /**
     * Gets user ID.
     * @returns {number|null} User ID or null
     */
    getUserId() {
        const user = this.getUser();
        return user ? user.id : null;
    },

    /**
     * Gets username.
     * @returns {string|null} Username or null
     */
    getUsername() {
        const user = this.getUser();
        return user ? user.username : null;
    },

    /**
     * Gets user email.
     * @returns {string|null} Email or null
     */
    getEmail() {
        const user = this.getUser();
        return user ? user.email : null;
    },

    /**
     * Gets user's full name.
     * @returns {string} Full name or username
     */
    getFullName() {
        const user = this.getUser();
        if (!user) return '';
        
        if (user.firstName && user.lastName) {
            return `${user.firstName} ${user.lastName}`;
        }
        
        return user.username || '';
    },

    /**
     * Gets user roles.
     * @returns {Array<string>} User roles
     */
    getRoles() {
        const user = this.getUser();
        return user && user.roles ? user.roles : [];
    },

    /**
     * Checks if user has a specific role.
     * @param {string} role - Role name
     * @returns {boolean}
     */
    hasRole(role) {
        const roles = this.getRoles();
        return roles.includes(role);
    },

    /**
     * Checks if user is admin.
     * @returns {boolean}
     */
    isAdmin() {
        return this.hasRole('ROLE_ADMIN') || this.hasRole('ADMIN');
    },

    /**
     * Clears user data.
     */
    clearUser() {
        StorageManager.remove(AUTH_CONFIG.USER_DATA_KEY);
    }
};

// ==================== Authentication Manager ====================

const AuthManager = {
    // State
    initialized: false,
    tokenCheckInterval: null,
    activityListeners: [],

    /**
     * Initializes the authentication manager.
     * Sets up token checking and activity monitoring.
     */
    init() {
        if (this.initialized) {
            return;
        }

        console.log('[Auth] Initializing authentication manager...');

        // Check initial authentication state
        this.checkAuthState();

        // Start token expiry checking
        this.startTokenExpiryCheck();

        // Setup activity monitoring
        this.setupActivityMonitoring();

        this.initialized = true;
        console.log('[Auth] Authentication manager initialized');
    },

    /**
     * Checks current authentication state.
     */
    checkAuthState() {
        if (this.isAuthenticated()) {
            console.log('[Auth] User is authenticated:', UserManager.getUsername());
            
            // Check if token is expired
            if (TokenManager.isTokenExpired()) {
                console.warn('[Auth] Token is expired');
                this.handleTokenExpiry();
            }
            
            // Check if session timed out
            if (TokenManager.isSessionTimedOut()) {
                console.warn('[Auth] Session timed out due to inactivity');
                this.handleSessionTimeout();
            }
        } else {
            console.log('[Auth] User is not authenticated');
        }
    },

    /**
     * Checks if user is authenticated.
     * @returns {boolean} True if authenticated
     */
    isAuthenticated() {
        const token = TokenManager.getAccessToken();
        const user = UserManager.getUser();
        
        return !!(token && user);
    },

    /**
     * Logs in user with credentials.
     * @param {string} username - Username
     * @param {string} password - Password
     * @param {boolean} rememberMe - Remember me flag
     * @returns {Promise<Object>} Login response
     */
    async login(username, password, rememberMe = false) {
        try {
            // Use apiService if available
            if (window.apiService) {
                const response = await window.apiService.auth.login(username, password);
                
                // Save tokens and user data
                TokenManager.saveTokens(
                    response.accessToken, 
                    response.refreshToken, 
                    rememberMe
                );
                
                UserManager.saveUser({
                    id: response.id,
                    username: response.username,
                    email: response.email,
                    roles: response.roles
                });
                
                console.log('[Auth] Login successful:', username);
                return response;
            } else {
                throw new Error('API service not available');
            }
        } catch (error) {
            console.error('[Auth] Login failed:', error);
            throw error;
        }
    },

    /**
     * Logs out user and clears all data.
     * @param {boolean} redirect - Whether to redirect to login page
     */
    async logout(redirect = true) {
        console.log('[Auth] Logging out user...');

        try {
            // Call logout API if available
            if (window.apiService && TokenManager.getAccessToken()) {
                await window.apiService.auth.logout().catch(err => {
                    console.warn('[Auth] Logout API call failed:', err);
                });
            }
        } finally {
            // Clear all authentication data
            TokenManager.clearTokens();
            UserManager.clearUser();
            
            // Stop monitoring
            this.cleanup();
            
            console.log('[Auth] Logout complete');
            
            // Redirect to login page
            if (redirect && AUTH_CONFIG.ENABLE_AUTO_REDIRECT) {
                this.redirectToLogin();
            }
        }
    },

    /**
     * Handles token expiry.
     */
    handleTokenExpiry() {
        console.warn('[Auth] Handling token expiry...');
        
        // Try to refresh token if refresh token exists
        const refreshToken = TokenManager.getRefreshToken();
        
        if (refreshToken && window.apiService) {
            console.log('[Auth] Attempting token refresh...');
            // Token refresh is handled automatically by apiService interceptors
            return;
        }
        
        // No refresh token, logout
        this.logout(true);
        this.showMessage('Your session has expired. Please log in again.', 'warning');
    },

    /**
     * Handles session timeout due to inactivity.
     */
    handleSessionTimeout() {
        console.warn('[Auth] Session timed out due to inactivity');
        this.logout(true);
        this.showMessage('Your session has expired due to inactivity.', 'warning');
    },

    /**
     * Handles unauthorized access (401 response).
     */
    handleUnauthorized() {
        console.warn('[Auth] Unauthorized access detected');
        this.logout(true);
        this.showMessage('Authentication required. Please log in.', 'error');
    },

    /**
     * Protects a page by requiring authentication.
     * Redirects to login if not authenticated.
     */
    requireAuth() {
        if (!this.isAuthenticated()) {
            console.warn('[Auth] Authentication required, redirecting to login...');
            this.redirectToLogin();
            return false;
        }
        
        // Check token expiry
        if (TokenManager.isTokenExpired()) {
            console.warn('[Auth] Token expired, redirecting to login...');
            this.handleTokenExpiry();
            return false;
        }
        
        // Check session timeout
        if (TokenManager.isSessionTimedOut()) {
            console.warn('[Auth] Session timeout, redirecting to login...');
            this.handleSessionTimeout();
            return false;
        }
        
        return true;
    },

    /**
     * Protects admin pages by requiring admin role.
     */
    requireAdmin() {
        if (!this.requireAuth()) {
            return false;
        }
        
        if (!UserManager.isAdmin()) {
            console.warn('[Auth] Admin access required');
            this.showMessage('You do not have permission to access this page.', 'error');
            this.redirectToHome();
            return false;
        }
        
        return true;
    },

    /**
     * Checks if current page is public.
     * @returns {boolean}
     */
    isPublicPage() {
        const currentPath = window.location.pathname;
        return AUTH_CONFIG.PUBLIC_PAGES.some(page => 
            currentPath.endsWith(page) || currentPath === '/'
        );
    },

    /**
     * Redirects to login page.
     * @param {boolean} saveRedirect - Save current page for redirect after login
     */
    redirectToLogin(saveRedirect = true) {
        if (typeof window === 'undefined') return;
        
        let loginUrl = AUTH_CONFIG.LOGIN_PAGE;
        
        // Save current page for redirect after login
        if (saveRedirect && !this.isPublicPage()) {
            const currentPath = window.location.pathname + window.location.search;
            loginUrl += `?${AUTH_CONFIG.REDIRECT_PARAM}=${encodeURIComponent(currentPath)}`;
        }
        
        window.location.href = loginUrl;
    },

    /**
     * Redirects to home page.
     */
    redirectToHome() {
        if (typeof window === 'undefined') return;
        window.location.href = AUTH_CONFIG.HOME_PAGE;
    },

    /**
     * Redirects to saved redirect URL or dashboard.
     */
    redirectAfterLogin() {
        if (typeof window === 'undefined') return;
        
        // Check for redirect parameter
        const urlParams = new URLSearchParams(window.location.search);
        const redirectUrl = urlParams.get(AUTH_CONFIG.REDIRECT_PARAM);
        
        if (redirectUrl) {
            window.location.href = decodeURIComponent(redirectUrl);
        } else {
            window.location.href = AUTH_CONFIG.DASHBOARD_PAGE;
        }
    },

    /**
     * Starts periodic token expiry checking.
     */
    startTokenExpiryCheck() {
        // Clear existing interval
        if (this.tokenCheckInterval) {
            clearInterval(this.tokenCheckInterval);
        }

        // Check token every minute
        this.tokenCheckInterval = setInterval(() => {
            if (this.isAuthenticated()) {
                if (TokenManager.isTokenExpired()) {
                    this.handleTokenExpiry();
                } else if (TokenManager.isSessionTimedOut()) {
                    this.handleSessionTimeout();
                }
            }
        }, AUTH_CONFIG.TOKEN_CHECK_INTERVAL);

        console.log('[Auth] Token expiry checking started');
    },

    /**
     * Sets up activity monitoring for session timeout.
     */
    setupActivityMonitoring() {
        // Remove existing listeners
        this.removeActivityListeners();

        // Add activity listeners
        AUTH_CONFIG.ACTIVITY_EVENTS.forEach(event => {
            const listener = () => {
                if (this.isAuthenticated()) {
                    TokenManager.updateLastActivity();
                }
            };
            
            window.addEventListener(event, listener, { passive: true });
            this.activityListeners.push({ event, listener });
        });

        console.log('[Auth] Activity monitoring started');
    },

    /**
     * Removes activity listeners.
     */
    removeActivityListeners() {
        this.activityListeners.forEach(({ event, listener }) => {
            window.removeEventListener(event, listener);
        });
        this.activityListeners = [];
    },

    /**
     * Cleans up intervals and listeners.
     */
    cleanup() {
        if (this.tokenCheckInterval) {
            clearInterval(this.tokenCheckInterval);
            this.tokenCheckInterval = null;
        }
        
        this.removeActivityListeners();
        this.initialized = false;
        
        console.log('[Auth] Cleanup complete');
    },

    /**
     * Shows a message to the user.
     * @param {string} message - Message text
     * @param {string} type - Message type (info, success, warning, error)
     */
    showMessage(message, type = 'info') {
        console.log(`[Auth] ${type.toUpperCase()}: ${message}`);
        
        // Try to show in UI if available
        if (typeof window !== 'undefined' && window.showNotification) {
            window.showNotification(message, type);
        } else {
            // Fallback to alert for errors and warnings
            if (type === 'error' || type === 'warning') {
                alert(message);
            }
        }
    },

    // Expose sub-managers for direct access
    token: TokenManager,
    user: UserManager,
    storage: StorageManager,
    config: AUTH_CONFIG
};

// ==================== Auto-initialization ====================

// Auto-initialize when DOM is ready
if (typeof window !== 'undefined') {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            AuthManager.init();
        });
    } else {
        AuthManager.init();
    }
}

// ==================== Export ====================

// CommonJS
if (typeof module !== 'undefined' && module.exports) {
    module.exports = AuthManager;
}

// Global
if (typeof window !== 'undefined') {
    window.AuthManager = AuthManager;
}

// ES6 export
export default AuthManager;
