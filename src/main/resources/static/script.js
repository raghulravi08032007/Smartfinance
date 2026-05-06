// ============================================================================
// AUTHENTICATION AND USER MANAGEMENT
// ============================================================================

/**
 * Simple auth check using localStorage
 */
function isUserLoggedIn() {
  // Check both old and new token keys for backward compatibility
  const token = localStorage.getItem('jwt_access_token') || localStorage.getItem('jwtToken');
  return token !== null && token !== undefined && token !== '';
}

function getLoggedInUsername() {
  // Try multiple sources for username
  let username = localStorage.getItem('username');
  
  // If not found, try to get from user object
  if (!username) {
    try {
      const userData = localStorage.getItem('user');
      if (userData) {
        const user = JSON.parse(userData);
        username = user.username;
      }
    } catch (e) {
      console.error('Error parsing user data:', e);
    }
  }
  
  // If still not found, try to get from apiService user data
  if (!username && window.apiService) {
    try {
      const userData = window.apiService.auth.getUserData();
      if (userData && userData.username) {
        username = userData.username;
        // Save it to localStorage for future use
        localStorage.setItem('username', userData.username);
      }
    } catch (e) {
      console.error('Error getting username from apiService:', e);
    }
  }
  
  // Last resort: check if there's a JWT token and decode it
  if (!username) {
    try {
      // Check both old and new token keys
      const token = localStorage.getItem('jwt_access_token') || localStorage.getItem('jwtToken');
      if (token) {
        // Basic JWT decode (without validation)
        const base64Url = token.split('.')[1];
        if (base64Url) {
          const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
          const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          }).join(''));
          const payload = JSON.parse(jsonPayload);
          username = payload.sub || payload.username;
          if (username) {
            // Save it to localStorage for future use
            localStorage.setItem('username', username);
          }
        }
      }
    } catch (e) {
      console.error('Error decoding JWT token:', e);
    }
  }
  
  // If STILL not found, check if we're on a page that requires authentication
  if (!username) {
    // Check if we're on a public page (pages that don't require authentication)
    const currentPage = window.location.pathname;
    const isPublicPage = currentPage.includes('index.html') ||
                         currentPage.includes('login.html') || 
                         currentPage.includes('sign-up.html') || 
                         currentPage.includes('forgot-password.html') ||
                         currentPage.includes('support.html') ||
                         currentPage.includes('transaction-recovery.html') ||
                         currentPage === '/' ||
                         currentPage === '';
    
    if (!isPublicPage) {
      console.error('CRITICAL: No username found in any storage location!');
      console.error('localStorage keys:', Object.keys(localStorage));
      console.error('Redirecting to login page...');
      // Only redirect if not already on a public page
      setTimeout(() => {
        window.location.href = './login.html';
      }, 100);
      return null;
    }
    
    // If on public page, return null without redirecting
    console.log('On public page, username not required');
    return null;
  }
  
  return username;
}

/**
 * Update navigation based on authentication status
 */
function updateNavigationForAuth() {
  const isAuthenticated = isUserLoggedIn();
  const username = getLoggedInUsername();
  
  // Get navigation elements
  const buttonsDiv = document.querySelector('.buttons');
  if (!buttonsDiv) return;
  
  if (isAuthenticated) {
    // Replace login/signup buttons with user menu
    buttonsDiv.innerHTML = `
      <span style="color: #e74c3c; margin-right: 1rem; display: inline-flex; align-items: center; gap: 0.5rem; background-color: rgba(255, 140, 0, 0.2); padding: 0.5rem 1rem; border-radius: 6px; border: 2px solid #ff8c00;">
        <i class="fas fa-user-circle" style="font-size: 1.2rem; color: #ff8c00;"></i>
        <strong style="color: #e74c3c;">${username}</strong>
      </span>
      <a href="#" id="logout-btn" class="btn-head" style="background-color: #e74c3c; color: #fff;">
        <i class="fas fa-sign-out-alt"></i> Logout
      </a>
    `;
    
    // Add logout handler
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', handleLogout);
    }
    
    // Update hero section - change title and button for logged in users
    const heroTitle = document.querySelector('.under-welcome-h1');
    if (heroTitle) {
      heroTitle.textContent = `Welcome Back, ${username}!`;
    }
    
    const heroDesc = document.querySelector('.under-welcome-p');
    if (heroDesc) {
      heroDesc.textContent = 'Continue managing your finances and tracking your progress. Access your dashboard to view your latest transactions and reports.';
    }
    
    const heroSignupBtn = document.querySelector('.under-welcome-btn');
    if (heroSignupBtn) {
      heroSignupBtn.href = '#tracker';
      heroSignupBtn.innerHTML = '<i class="fas fa-tachometer-alt"></i> Go to Dashboard';
    }
    
    // Update other signup buttons on the page
    const signupButtons = document.querySelectorAll('a[href="./sign-up.html"]');
    signupButtons.forEach(btn => {
      if (btn.classList.contains('sign-up-button-btn')) {
        btn.href = '#tracker';
        btn.textContent = 'Go to Dashboard';
      }
    });
    
  } else {
    // Show login/signup buttons
    buttonsDiv.innerHTML = `
      <a href="./login.html" class="btn-head"><i class="fas fa-sign-in-alt"></i> Log In</a>
      <a href="./sign-up.html" class="btn-head"><i class="fas fa-user-plus"></i> Sign Up</a>
    `;
    
    // Restore original hero section content for non-logged in users
    const heroTitle = document.querySelector('.under-welcome-h1');
    if (heroTitle && heroTitle.textContent.includes('Welcome Back')) {
      heroTitle.textContent = 'Take Control of Your Finances';
    }
    
    const heroDesc = document.querySelector('.under-welcome-p');
    if (heroDesc && heroDesc.textContent.includes('Continue managing')) {
      heroDesc.textContent = 'Track your income and expenses effortlessly. Manage your budget, set financial goals, and make smarter decisions for more secure future.';
    }
    
    const heroSignupBtn = document.querySelector('.under-welcome-btn');
    if (heroSignupBtn && heroSignupBtn.textContent.includes('Dashboard')) {
      heroSignupBtn.href = './sign-up.html';
      heroSignupBtn.textContent = 'Sign Up Now';
    }
  }
}

/**
 * Handle user logout
 */
async function handleLogout(e) {
  if (e) e.preventDefault();
  
  console.log('Logging out...');
  
  // Clear all auth data from localStorage
  // New token keys (used by apiService)
  localStorage.removeItem('jwt_access_token');
  localStorage.removeItem('jwt_refresh_token');
  localStorage.removeItem('user_data');
  
  // Legacy token keys (for backward compatibility)
  localStorage.removeItem('jwtToken');
  localStorage.removeItem('username');
  localStorage.removeItem('user');
  
  // Show success message
  alert('Logged out successfully!');
  
  // Redirect to login page
  window.location.href = './login.html';
}

/**
 * Display user information on the page
 */
function displayUserInfo(user) {
  // Update user info section if it exists
  const userInfoSection = document.getElementById('user-info-section');
  if (userInfoSection) {
    userInfoSection.innerHTML = `
      <div class="user-info-card">
        <div class="user-avatar">
          <i class="fas fa-user-circle"></i>
        </div>
        <div class="user-details">
          <h3>${user.fullName || user.name || 'User'}</h3>
          <p><i class="fas fa-envelope"></i> ${user.email || 'N/A'}</p>
          <p><i class="fas fa-user"></i> @${user.username || 'N/A'}</p>
          ${user.mobile ? `<p><i class="fas fa-phone"></i> ${user.mobile}</p>` : ''}
          <p><i class="fas fa-calendar"></i> Member since ${formatDate(new Date(user.createdAt || Date.now()))}</p>
        </div>
        <div class="user-actions">
          <button onclick="openEditProfile()" class="btn btn-primary">
            <i class="fas fa-edit"></i> Edit Profile
          </button>
          <button onclick="openChangePassword()" class="btn btn-secondary">
            <i class="fas fa-key"></i> Change Password
          </button>
        </div>
      </div>
    `;
  }
  
  // Update dynamic greeting message
  updateGreetingMessage(user);
}

/**
 * Update greeting message based on time of day
 */
function updateGreetingMessage(user) {
  const greetingElement = document.getElementById('dynamic-greeting');
  if (!greetingElement) return;
  
  const hour = new Date().getHours();
  let greeting = 'Hello';
  
  if (hour < 12) {
    greeting = 'Good morning';
  } else if (hour < 18) {
    greeting = 'Good afternoon';
  } else {
    greeting = 'Good evening';
  }
  
  const username = user.username || user.name || 'User';
  greetingElement.innerHTML = `
    <h2>${greeting}, <span class="username-highlight">${username}</span>! 👋</h2>
    <p>Welcome back to your Finance Tracker</p>
  `;
}

// ============================================================================
// PROFILE MANAGEMENT
// ============================================================================

/**
 * Open edit profile modal
 */
function openEditProfile() {
  const user = UserManager ? UserManager.getUser() : null;
  if (!user) {
    showToast('Unable to load user data', 'error');
    return;
  }
  
  // Create modal HTML
  const modalHTML = `
    <div id="edit-profile-modal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3><i class="fas fa-user-edit"></i> Edit Profile</h3>
          <button onclick="closeEditProfile()" class="close-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <form id="edit-profile-form">
            <div class="form-group">
              <label for="edit-fullname">Full Name</label>
              <input type="text" id="edit-fullname" name="fullName" value="${user.fullName || ''}" required>
            </div>
            <div class="form-group">
              <label for="edit-email">Email</label>
              <input type="email" id="edit-email" name="email" value="${user.email || ''}" required>
            </div>
            <div class="form-group">
              <label for="edit-mobile">Mobile</label>
              <input type="tel" id="edit-mobile" name="mobile" value="${user.mobile || ''}" pattern="[0-9]{10}">
            </div>
            <div class="form-actions">
              <button type="button" onclick="closeEditProfile()" class="btn btn-secondary">
                Cancel
              </button>
              <button type="submit" class="btn btn-primary">
                <i class="fas fa-save"></i> Save Changes
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `;
  
  // Add modal to page
  document.body.insertAdjacentHTML('beforeend', modalHTML);
  
  // Add form submit handler
  document.getElementById('edit-profile-form').addEventListener('submit', handleProfileUpdate);
}

/**
 * Close edit profile modal
 */
function closeEditProfile() {
  const modal = document.getElementById('edit-profile-modal');
  if (modal) modal.remove();
}

/**
 * Handle profile update submission
 */
async function handleProfileUpdate(e) {
  e.preventDefault();
  
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');
  const originalBtnText = submitBtn.innerHTML;
  
  try {
    // Show loading state
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';
    submitBtn.disabled = true;
    
    // Get form data
    const formData = new FormData(form);
    const updateData = {
      fullName: formData.get('fullName'),
      email: formData.get('email'),
      mobile: formData.get('mobile')
    };
    
    // Call API to update profile
    if (apiService && apiService.user && apiService.user.updateProfile) {
      const response = await apiService.user.updateProfile(updateData);
      
      // Update local user data
      if (UserManager) {
        UserManager.updateUser(response.data || updateData);
      }
      
      // Show success message
      showToast('Profile updated successfully!', 'success');
      
      // Refresh user info display
      displayUserInfo(UserManager.getUser());
      
      // Close modal
      closeEditProfile();
    } else {
      throw new Error('API service not available');
    }
    
  } catch (error) {
    console.error('Profile update error:', error);
    showToast(error.message || 'Failed to update profile', 'error');
    
    // Reset button state
    submitBtn.innerHTML = originalBtnText;
    submitBtn.disabled = false;
  }
}

/**
 * Open change password modal
 */
function openChangePassword() {
  const modalHTML = `
    <div id="change-password-modal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3><i class="fas fa-key"></i> Change Password</h3>
          <button onclick="closeChangePassword()" class="close-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <form id="change-password-form">
            <div class="form-group">
              <label for="current-password">Current Password</label>
              <input type="password" id="current-password" name="currentPassword" required>
            </div>
            <div class="form-group">
              <label for="new-password">New Password</label>
              <input type="password" id="new-password" name="newPassword" required minlength="8">
              <small>Minimum 8 characters with uppercase, lowercase, number, and special character</small>
            </div>
            <div class="form-group">
              <label for="confirm-password">Confirm New Password</label>
              <input type="password" id="confirm-password" name="confirmPassword" required>
            </div>
            <div class="form-actions">
              <button type="button" onclick="closeChangePassword()" class="btn btn-secondary">
                Cancel
              </button>
              <button type="submit" class="btn btn-primary">
                <i class="fas fa-check"></i> Change Password
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `;
  
  document.body.insertAdjacentHTML('beforeend', modalHTML);
  document.getElementById('change-password-form').addEventListener('submit', handlePasswordChange);
}

/**
 * Close change password modal
 */
function closeChangePassword() {
  const modal = document.getElementById('change-password-modal');
  if (modal) modal.remove();
}

/**
 * Handle password change submission
 */
async function handlePasswordChange(e) {
  e.preventDefault();
  
  const form = e.target;
  const submitBtn = form.querySelector('button[type="submit"]');
  const originalBtnText = submitBtn.innerHTML;
  
  // Get form values
  const currentPassword = form.currentPassword.value;
  const newPassword = form.newPassword.value;
  const confirmPassword = form.confirmPassword.value;
  
  // Validate passwords match
  if (newPassword !== confirmPassword) {
    showToast('New passwords do not match!', 'error');
    return;
  }
  
  try {
    // Show loading state
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Changing...';
    submitBtn.disabled = true;
    
    // Call API to change password
    if (apiService && apiService.user && apiService.user.changePassword) {
      await apiService.user.changePassword({
        currentPassword,
        newPassword
      });
      
      showToast('Password changed successfully!', 'success');
      closeChangePassword();
      
      // Optionally logout user to re-login with new password
      setTimeout(() => {
        handleLogout();
      }, 2000);
    } else {
      throw new Error('API service not available');
    }
    
  } catch (error) {
    console.error('Password change error:', error);
    showToast(error.message || 'Failed to change password', 'error');
    
    submitBtn.innerHTML = originalBtnText;
    submitBtn.disabled = false;
  }
}

// ============================================================================
// SUPPORT TICKET HISTORY
// ============================================================================

/**
 * Load and display support ticket history
 */
async function loadSupportTickets() {
  const ticketsContainer = document.getElementById('support-tickets-container');
  if (!ticketsContainer) return;
  
  try {
    // Show loading state
    ticketsContainer.innerHTML = '<div class="loading-spinner"><i class="fas fa-spinner fa-spin"></i> Loading tickets...</div>';
    
    // Fetch tickets from API
    if (apiService && apiService.support && apiService.support.getMyTickets) {
      const response = await apiService.support.getMyTickets();
      const tickets = response.data || response;
      
      if (!tickets || tickets.length === 0) {
        ticketsContainer.innerHTML = `
          <div class="no-tickets">
            <i class="fas fa-inbox"></i>
            <p>No support tickets found</p>
            <a href="./support.html" class="btn btn-primary">Create New Ticket</a>
          </div>
        `;
        return;
      }
      
      // Display tickets table
      displayTicketsTable(tickets, ticketsContainer);
    } else {
      throw new Error('Support API not available');
    }
    
  } catch (error) {
    console.error('Error loading tickets:', error);
    ticketsContainer.innerHTML = `
      <div class="error-message">
        <i class="fas fa-exclamation-circle"></i>
        <p>Failed to load support tickets</p>
        <button onclick="loadSupportTickets()" class="btn btn-secondary">Retry</button>
      </div>
    `;
  }
}

/**
 * Display tickets in a table format
 */
function displayTicketsTable(tickets, container) {
  const tableHTML = `
    <div class="tickets-table-wrapper">
      <table class="tickets-table">
        <thead>
          <tr>
            <th>Ticket #</th>
            <th>Subject</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${tickets.map(ticket => `
            <tr>
              <td><strong>#${ticket.ticketNumber || ticket.id}</strong></td>
              <td>${ticket.subject || 'N/A'}</td>
              <td>
                <span class="priority-badge priority-${(ticket.priority || 'low').toLowerCase()}">
                  ${ticket.priority || 'Low'}
                </span>
              </td>
              <td>
                <span class="status-badge status-${(ticket.status || 'open').toLowerCase()}">
                  <i class="fas fa-${getStatusIcon(ticket.status)}"></i>
                  ${ticket.status || 'Open'}
                </span>
              </td>
              <td>${formatDate(new Date(ticket.createdAt || Date.now()))}</td>
              <td>
                <button onclick="viewTicketDetails('${ticket.id || ticket.ticketNumber}')" class="btn-icon" title="View Details">
                  <i class="fas fa-eye"></i>
                </button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;
  
  container.innerHTML = tableHTML;
}

/**
 * Get icon for ticket status
 */
function getStatusIcon(status) {
  const icons = {
    'open': 'folder-open',
    'in-progress': 'spinner',
    'resolved': 'check-circle',
    'closed': 'times-circle',
    'pending': 'clock'
  };
  return icons[(status || 'open').toLowerCase()] || 'question-circle';
}

/**
 * View ticket details
 */
async function viewTicketDetails(ticketId) {
  try {
    // Show loading toast
    showToast('Loading ticket details...', 'info');
    
    // Fetch ticket details
    if (apiService && apiService.support && apiService.support.getTicketById) {
      const response = await apiService.support.getTicketById(ticketId);
      const ticket = response.data || response;
      
      // Create modal with ticket details
      const modalHTML = `
        <div id="ticket-details-modal" class="modal-overlay">
          <div class="modal-content modal-large">
            <div class="modal-header">
              <h3><i class="fas fa-ticket-alt"></i> Ticket #${ticket.ticketNumber || ticketId}</h3>
              <button onclick="closeTicketDetails()" class="close-btn">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="modal-body">
              <div class="ticket-details">
                <div class="ticket-info-grid">
                  <div class="info-item">
                    <label>Subject:</label>
                    <p>${ticket.subject || 'N/A'}</p>
                  </div>
                  <div class="info-item">
                    <label>Priority:</label>
                    <span class="priority-badge priority-${(ticket.priority || 'low').toLowerCase()}">
                      ${ticket.priority || 'Low'}
                    </span>
                  </div>
                  <div class="info-item">
                    <label>Status:</label>
                    <span class="status-badge status-${(ticket.status || 'open').toLowerCase()}">
                      <i class="fas fa-${getStatusIcon(ticket.status)}"></i>
                      ${ticket.status || 'Open'}
                    </span>
                  </div>
                  <div class="info-item">
                    <label>Created:</label>
                    <p>${formatDate(new Date(ticket.createdAt || Date.now()))}</p>
                  </div>
                </div>
                <div class="ticket-message">
                  <label>Message:</label>
                  <div class="message-content">${ticket.message || 'No message provided'}</div>
                </div>
                ${ticket.response ? `
                  <div class="ticket-response">
                    <label>Response from Support:</label>
                    <div class="response-content">${ticket.response}</div>
                  </div>
                ` : ''}
              </div>
            </div>
            <div class="modal-footer">
              <button onclick="closeTicketDetails()" class="btn btn-secondary">Close</button>
            </div>
          </div>
        </div>
      `;
      
      document.body.insertAdjacentHTML('beforeend', modalHTML);
    } else {
      throw new Error('Unable to fetch ticket details');
    }
    
  } catch (error) {
    console.error('Error viewing ticket:', error);
    showToast('Failed to load ticket details', 'error');
  }
}

/**
 * Close ticket details modal
 */
function closeTicketDetails() {
  const modal = document.getElementById('ticket-details-modal');
  if (modal) modal.remove();
}

// ============================================================================
// ENHANCED TOAST NOTIFICATIONS
// ============================================================================

/**
 * Show enhanced toast notification with API response handling
 */
function showToast(message, type = 'info', duration = 5000) {
  // Remove existing toasts
  const existingToasts = document.querySelectorAll('.toast-notification');
  existingToasts.forEach(toast => toast.remove());
  
  // Create toast element
  const toast = document.createElement('div');
  toast.className = `toast-notification toast-${type}`;
  
  // Set icon based on type
  const icons = {
    success: 'fa-check-circle',
    error: 'fa-exclamation-circle',
    warning: 'fa-exclamation-triangle',
    info: 'fa-info-circle'
  };
  
  const colors = {
    success: 'linear-gradient(135deg, #28a745 0%, #20c997 100%)',
    error: 'linear-gradient(135deg, #dc3545 0%, #c82333 100%)',
    warning: 'linear-gradient(135deg, #ffc107 0%, #ff9800 100%)',
    info: 'linear-gradient(135deg, #17a2b8 0%, #138496 100%)'
  };
  
  toast.innerHTML = `
    <div class="toast-icon">
      <i class="fas ${icons[type]}"></i>
    </div>
    <div class="toast-content">
      <div class="toast-message">${message}</div>
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">
      <i class="fas fa-times"></i>
    </button>
  `;
  
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    min-width: 300px;
    max-width: 500px;
    background: ${colors[type]};
    color: white;
    padding: 16px 20px;
    border-radius: 12px;
    box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    z-index: 100000;
    display: flex;
    align-items: center;
    gap: 12px;
    animation: slideInRight 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255,255,255,0.2);
  `;
  
  // Add styles for toast components
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideInRight {
      from { transform: translateX(400px); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOutRight {
      from { transform: translateX(0); opacity: 1; }
      to { transform: translateX(400px); opacity: 0; }
    }
    .toast-notification .toast-icon {
      font-size: 24px;
      flex-shrink: 0;
    }
    .toast-notification .toast-content {
      flex: 1;
    }
    .toast-notification .toast-message {
      font-weight: 500;
      line-height: 1.4;
    }
    .toast-notification .toast-close {
      background: transparent;
      border: none;
      color: white;
      cursor: pointer;
      font-size: 18px;
      padding: 4px;
      opacity: 0.8;
      transition: opacity 0.2s;
      flex-shrink: 0;
    }
    .toast-notification .toast-close:hover {
      opacity: 1;
    }
  `;
  
  if (!document.getElementById('toast-styles')) {
    style.id = 'toast-styles';
    document.head.appendChild(style);
  }
  
  document.body.appendChild(toast);
  
  // Auto-remove after duration
  setTimeout(() => {
    toast.style.animation = 'slideOutRight 0.4s ease-in';
    setTimeout(() => toast.remove(), 400);
  }, duration);
}

// ============================================================================
// INITIALIZE AUTHENTICATION UI
// ============================================================================

// Active Navigation Highlighting
document.addEventListener('DOMContentLoaded', function() {
  // Initialize authentication UI
  updateNavigationForAuth();
  
  // Load support tickets if on dashboard/profile page
  const ticketsContainer = document.getElementById('support-tickets-container');
  if (ticketsContainer && AuthManager && AuthManager.isAuthenticated()) {
    loadSupportTickets();
  }
  
  // Get current page path
  const currentPath = window.location.pathname;
  const currentPage = currentPath.split('/').pop() || 'index.html';
  
  // Remove active class from all nav links
  const navLinks = document.querySelectorAll('.nav-links a');
  navLinks.forEach(link => link.classList.remove('active'));
  
  // Add active class based on current page
  if (currentPage === 'index.html' || currentPage === '') {
    // For index page, highlight Home
    const homeLink = document.querySelector('.nav-links a[href="#"], .nav-links a[href="./index.html"]');
    if (homeLink) homeLink.classList.add('active');
  } else if (currentPage === 'support.html') {
    // For support page, highlight Support
    const supportLink = document.querySelector('.nav-links a[href="./support.html"]');
    if (supportLink) supportLink.classList.add('active');
  } else if (currentPage === 'login.html') {
    // For login page, highlight Home (since it's part of main site)
    const homeLink = document.querySelector('.nav-links a[href="./index.html"]');
    if (homeLink) homeLink.classList.add('active');
  } else if (currentPage === 'sign-up.html') {
    // For sign-up page, highlight Home (since it's part of main site)
    const homeLink = document.querySelector('.nav-links a[href="./index.html"]');
    if (homeLink) homeLink.classList.add('active');
  }
  
  // Initialize keyboard navigation
  initKeyboardNavigation();
  initKeyboardShortcuts();
  
  // Mobile Menu Toggle
  const menuToggle = document.getElementById('menu-toggle');
  const navLinksContainer = document.querySelector('.nav-links');
  
  if (menuToggle && navLinksContainer) {
    menuToggle.addEventListener('click', function() {
      navLinksContainer.classList.toggle('active');
      const icon = menuToggle.querySelector('i');
      if (navLinksContainer.classList.contains('active')) {
        icon.classList.remove('fa-bars');
        icon.classList.add('fa-times');
      } else {
        icon.classList.remove('fa-times');
        icon.classList.add('fa-bars');
      }
    });
    
    // Close menu when clicking on a link
    navLinks.forEach(link => {
      link.addEventListener('click', function() {
        navLinksContainer.classList.remove('active');
        const icon = menuToggle.querySelector('i');
        icon.classList.remove('fa-times');
        icon.classList.add('fa-bars');
      });
    });
    
    // Close menu when clicking outside
    document.addEventListener('click', function(event) {
      if (!menuToggle.contains(event.target) && !navLinksContainer.contains(event.target)) {
        navLinksContainer.classList.remove('active');
        const icon = menuToggle.querySelector('i');
        icon.classList.remove('fa-times');
        icon.classList.add('fa-bars');
      }
    });
  }
});

// const arrow = document.querySelector('.arrow');

// arrow.addEventListener('click', (event) => {
//   event.preventDefault();
//   const targetElement = document.querySelector(event.target.getAttribute('#tracker'));
//   targetElement.scrollIntoView({ behavior: 'smooth' });
// });

// Get the table-part element and the transaction-table
const tablePart = document.querySelector(".table-part");
const transactionTable = document.getElementById("transaction-table");

// Function to check the number of entries and apply scrollbar if needed
function checkTableScroll() {
  const rowCount = transactionTable.rows.length - 1; // Exclude the header row
  const maxRowCount = 10; // Set the desired maximum number of entries

  if (rowCount > maxRowCount) {
    tablePart.classList.add("scrollable");
  } else {
    tablePart.classList.remove("scrollable");
  }
}

// Call the function initially and whenever there is a change in the table
checkTableScroll();

// Add an event listener for changes in the table
const observer = new MutationObserver(checkTableScroll);
observer.observe(transactionTable, {
  childList: true,
  subtree: true,
});

// Initialize an empty array to store the transactions
let transactions = [];

// Load transactions from backend API
async function loadTransactions() {
  // Get current logged-in username
  const currentUser = getLoggedInUsername();
  
  // Critical check: if username is null, don't proceed
  if (!currentUser) {
    console.error('Cannot load transactions: No username available');
    transactions = [];
    return;
  }
  
  console.log(`Loading transactions for user: ${currentUser} from backend API`);
  
  // Check if apiService is available
  if (typeof apiService === 'undefined' || !apiService || !apiService.transactions) {
    console.error('apiService is not available! Falling back to legacy localStorage.');
    loadTransactionsLegacy();
    return;
  }
  
  try {
    // Fetch transactions from backend
    const response = await apiService.transactions.getAllTransactions();
    
    if (response.success && response.data) {
      // Transform backend data to frontend format
      transactions = response.data.map(t => ({
        primeId: t.id, // Use backend ID as primeId
        description: t.description,
        amount: t.amount,
        type: t.type.toLowerCase(), // Backend uses UPPERCASE, frontend uses lowercase
        date: t.date // Backend sends date in YYYY-MM-DD format
      }));
      
      console.log(`Loaded ${transactions.length} transactions from backend for user: ${currentUser}`);
    } else {
      console.warn('Failed to load transactions:', response);
      transactions = [];
    }
  } catch (error) {
    console.error('Error loading transactions from backend:', error);
    transactions = [];
    
    // Try to migrate from localStorage if this is the first time
    migrateFromLocalStorage(currentUser);
  }
}

// Helper function to migrate old localStorage data to backend
async function migrateFromLocalStorage(currentUser) {
  const oldKey = `financeTracker.transactions.${currentUser}`;
  const legacyKey = 'financeTracker.transactions';
  
  let oldData = localStorage.getItem(oldKey) || localStorage.getItem(legacyKey);
  
  if (oldData) {
    try {
      const oldTransactions = JSON.parse(oldData);
      if (Array.isArray(oldTransactions) && oldTransactions.length > 0) {
        console.log(`Migrating ${oldTransactions.length} transactions from localStorage to backend...`);
        
        // Upload each transaction to backend
        for (const transaction of oldTransactions) {
          try {
            const transactionData = {
              description: transaction.description,
              amount: transaction.amount,
              type: transaction.type.toUpperCase(), // Convert to backend format
              date: transaction.primeId ? new Date(transaction.primeId).toISOString().split('T')[0] : new Date().toISOString().split('T')[0]
            };
            
            await apiService.transactions.createTransaction(transactionData);
          } catch (err) {
            console.error('Error migrating transaction:', err);
          }
        }
        
        console.log('Migration completed! Reloading transactions...');
        
        // Remove old localStorage data
        localStorage.removeItem(oldKey);
        localStorage.removeItem(legacyKey);
        localStorage.setItem('financeTracker.migrationDone', 'true');
        
        // Reload transactions from backend
        await loadTransactions();
      }
    } catch (e) {
      console.error('Error during migration:', e);
    }
  }
}

// Legacy support - keep minimal localStorage code for backwards compatibility
function loadTransactionsLegacy() {
  // This function is kept for reference but should no longer be used
  // All transaction storage is now handled by the backend
  const currentUser = getLoggedInUsername();
  
  if (!currentUser) {
    console.error('Cannot load transactions: No username available');
    transactions = [];
    return;
  }
  
  const storageKey = `financeTracker.transactions.${currentUser}`;
  console.log(`[LEGACY] Loading transactions for user: ${currentUser} from key: ${storageKey}`);
  
  let stored = localStorage.getItem(storageKey);
  
  if (!stored) {
    const oldKey = 'financeTracker.transactions';
    const migrationKey = 'financeTracker.migrationDone';
    const migrationDone = localStorage.getItem(migrationKey);
    
    if (!migrationDone) {
      const oldStored = localStorage.getItem(oldKey);
      
      if (oldStored && currentUser === 'AjayRocks') {
        console.log('One-time migration: Moving old transactions to AjayRocks');
        localStorage.setItem(storageKey, oldStored);
        stored = oldStored;
        
        // Mark migration as complete
        localStorage.setItem(migrationKey, 'true');
        
        // Remove old key
        localStorage.removeItem(oldKey);
      } else {
        // Mark migration as done even if no old data exists
        localStorage.setItem(migrationKey, 'true');
      }
    }
  }
  
  if (stored) {
    try {
      transactions = JSON.parse(stored);
      console.log(`Loaded ${transactions.length} transactions from localStorage`);
    } catch (e) {
      console.error('Failed to parse transactions:', e);
      transactions = [];
    }
  } else {
    console.log('No existing transactions found, starting with empty array');
    transactions = [];
  }
}

// Save transactions is now handled by the backend API
// This function is kept for legacy compatibility but does nothing
function saveTransactions() {
  console.log('[DEPRECATED] saveTransactions called - transactions are now saved automatically to backend');
}

// Variable to store the current transaction being edited
let editedTransaction = null;

// Function to add a new transaction (updated to use backend API)
async function addTransaction() {
  const descriptionInput = document.getElementById("description");
  const amountInput = document.getElementById("amount");
  const typeInput = document.getElementById("type");
  const dateInput = document.getElementById("date");

  if (!descriptionInput || !amountInput || !typeInput || !dateInput) {
    console.error('Required form elements not found');
    return;
  }

  const description = descriptionInput.value;
  const amount = parseFloat(amountInput.value);
  const type = typeInput.value;
  const chosenDate = new Date(dateInput.value);

  // Validate the input - fixed date validation
  if (description.trim() === "" || isNaN(amount) || isNaN(chosenDate.getTime())) {
    alert('Please fill in all fields with valid data');
    return;
  }

  // Check if apiService is available
  if (typeof apiService === 'undefined' || !apiService || !apiService.transactions) {
    console.error('apiService is not available!');
    console.error('window.apiService:', window.apiService);
    alert('Error: API service is not loaded. Please refresh the page.');
    return;
  }

  try {
    // Create transaction data for backend
    const transactionData = {
      description: description,
      amount: amount,
      type: type.toUpperCase(), // Backend expects UPPERCASE
      date: dateInput.value // Already in YYYY-MM-DD format
    };

    // Send to backend
    const response = await apiService.transactions.createTransaction(transactionData);

    if (response.success && response.data) {
      console.log('Transaction created successfully:', response.data);
      
      // Clear the input fields
      descriptionInput.value = "";
      amountInput.value = "";
      dateInput.value = "";

      // Reload transactions from backend
      await loadTransactions();

      // Update the balance
      updateBalance();

      // Update the transaction table
      updateTransactionTable();

      // Show success message
      if (typeof showToast === 'function') {
        showToast('Transaction added successfully!', 'success');
      }
    } else {
      throw new Error(response.message || 'Failed to create transaction');
    }
  } catch (error) {
    console.error('Error adding transaction:', error);
    alert('Error: Failed to add transaction. ' + (error.message || 'Please try again.'));
  }
}

// Function to delete a transaction (updated to use backend API)
async function deleteTransaction(primeId) {
  // Confirm deletion
  if (!confirm('Are you sure you want to delete this transaction?')) {
    return;
  }

  // Check if apiService is available
  if (typeof apiService === 'undefined' || !apiService || !apiService.transactions) {
    console.error('apiService is not available!');
    alert('Error: API service is not loaded. Please refresh the page.');
    return;
  }

  try {
    // Delete from backend (primeId is the backend transaction ID)
    const response = await apiService.transactions.deleteTransaction(primeId);

    console.log('Transaction deleted successfully');

    // Reload transactions from backend
    await loadTransactions();

    // Update the balance
    updateBalance();

    // Update the transaction table
    updateTransactionTable();

    // Show success message
    if (typeof showToast === 'function') {
      showToast('Transaction deleted successfully!', 'success');
    }
  } catch (error) {
    console.error('Error deleting transaction:', error);
    alert('Error: Failed to delete transaction. ' + (error.message || 'Please try again.'));
  }
}

// Function to edit a transaction
function editTransaction(primeId) {
  // Find the transaction with the given primeId
  const transaction = transactions.find(
    (transaction) => transaction.primeId === primeId
  );

  if (!transaction) {
    console.error('Transaction not found:', primeId);
    return;
  }

  // Populate the input fields with the transaction details for editing
  document.getElementById("description").value = transaction.description;
  document.getElementById("amount").value = transaction.amount;
  document.getElementById("type").value = transaction.type;

  // Store the current transaction being edited
  editedTransaction = transaction;

  // Show the Save button and hide the Add Transaction button (only if they exist)
  const addBtn = document.getElementById("add-transaction-btn");
  const saveBtn = document.getElementById("save-transaction-btn");
  if (addBtn) addBtn.style.display = "none";
  if (saveBtn) saveBtn.style.display = "inline-block";

  // Set the date input value
  const dateInput = document.getElementById("date");
  if (dateInput && transaction.date) {
    dateInput.value = transaction.date; // Backend date is already in YYYY-MM-DD format
  }
}

// Function to save the edited transaction (updated to use backend API)
async function saveTransaction() {
  if (!editedTransaction) {
    return;
  }
  const descriptionInput = document.getElementById("description");
  const amountInput = document.getElementById("amount");
  const typeInput = document.getElementById("type");
  const dateInput = document.getElementById("date");

  const description = descriptionInput.value;
  const amount = parseFloat(amountInput.value);
  const type = typeInput.value;
  const chosenDate = new Date(dateInput.value);

  // Validate the input - fixed date validation
  if (description.trim() === "" || isNaN(amount) || isNaN(chosenDate.getTime())) {
    alert('Please fill in all fields with valid data');
    return;
  }

  // Check if apiService is available
  if (typeof apiService === 'undefined' || !apiService || !apiService.transactions) {
    console.error('apiService is not available!');
    alert('Error: API service is not loaded. Please refresh the page.');
    return;
  }

  try {
    // Create transaction data for backend
    const transactionData = {
      description: description,
      amount: amount,
      type: type.toUpperCase(), // Backend expects UPPERCASE
      date: dateInput.value // Already in YYYY-MM-DD format
    };

    // Update transaction on backend (use primeId which is the backend ID)
    const response = await apiService.transactions.updateTransaction(editedTransaction.primeId, transactionData);

    if (response.success && response.data) {
      console.log('Transaction updated successfully:', response.data);

      // Clear the input fields
      descriptionInput.value = "";
      amountInput.value = "";
      dateInput.value = "";

      // Clear the edited transaction
      editedTransaction = null;

      // Reload transactions from backend
      await loadTransactions();

      // Update the balance
      updateBalance();

      // Update the transaction table
      updateTransactionTable();

      // Show the Add Transaction button and hide the Save button (only if they exist)
      const addBtn = document.getElementById("add-transaction-btn");
      const saveBtn = document.getElementById("save-transaction-btn");
      if (addBtn) addBtn.style.display = "inline-block";
      if (saveBtn) saveBtn.style.display = "none";

      // Show success message
      if (typeof showToast === 'function') {
        showToast('Transaction updated successfully!', 'success');
      }
    } else {
      throw new Error(response.message || 'Failed to update transaction');
    }
  } catch (error) {
    console.error('Error updating transaction:', error);
    alert('Error: Failed to update transaction. ' + (error.message || 'Please try again.'));
  }
}

// Function to update the balance
function updateBalance() {
  const balanceElement = document.getElementById("balance");
  let balance = 0.0;

  // Calculate the total balance
  transactions.forEach((transaction) => {
    if (transaction.type === "income") {
      balance += transaction.amount;
    } else if (transaction.type === "expense") {
      balance -= transaction.amount;
    }
  });

  // Format the balance with currency symbol
  const currencySelect = document.getElementById("currency");
  const currencyCode = currencySelect.value;
  const formattedBalance = formatCurrency(balance, currencyCode);

  // Update the balance display
  balanceElement.textContent = formattedBalance;

  // Check if the balance is negative or positive
  if (balance < 0) {
    balanceElement.classList.remove("positive-balance");
    balanceElement.classList.add("negative-balance");
  } else {
    balanceElement.classList.remove("negative-balance");
    balanceElement.classList.add("positive-balance");
  }
}

// Function to format currency based on the selected currency code
function formatCurrency(amount, currencyCode) {
  // Define currency symbols and decimal separators for different currency codes
  const currencySymbols = {
    USD: "$",
    EUR: "€",
    INR: "₹",
  };

  const decimalSeparators = {
    USD: ".",
    EUR: ",",
    INR: ".",
  };

  // Get the currency symbol and decimal separator based on the currency code
  const symbol = currencySymbols[currencyCode] || "";
  const decimalSeparator = decimalSeparators[currencyCode] || ".";

  // Format the amount with currency symbol and decimal separator
  const formattedAmount =
    symbol + amount.toFixed(2).replace(".", decimalSeparator);
  return formattedAmount;
}

// Function to format date as DD/MM/YYYY
function formatDate(date) {
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

// Function to update the transaction table
function updateTransactionTable() {
  const transactionTable = document.getElementById("transaction-table");

  // Clear the existing table rows
  while (transactionTable.rows.length > 1) {
    transactionTable.deleteRow(1);
  }

  // Add new rows to the table
  transactions.forEach((transaction) => {
    const newRow = transactionTable.insertRow();

    const dateCell = newRow.insertCell();
    const date = new Date(transaction.primeId);
    dateCell.textContent = formatDate(date);

    const descriptionCell = newRow.insertCell();
    descriptionCell.textContent = transaction.description;

    const amountCell = newRow.insertCell();
    const currencySelect = document.getElementById("currency");
    const currencyCode = currencySelect.value;
    const formattedAmount = formatCurrency(transaction.amount, currencyCode);
    amountCell.textContent = formattedAmount;

    const typeCell = newRow.insertCell();
    typeCell.textContent = transaction.type;

    const actionCell = newRow.insertCell();
    const editButton = document.createElement("button");
    editButton.textContent = "Edit";
    editButton.classList.add("edit-button");
    editButton.addEventListener("click", () =>
      editTransaction(transaction.primeId)
    );
    actionCell.appendChild(editButton);

    const deleteButton = document.createElement("button");
    deleteButton.textContent = "Delete";
    deleteButton.classList.add("delete-button");
    deleteButton.addEventListener("click", () =>
      deleteTransaction(transaction.primeId)
    );
    actionCell.appendChild(deleteButton);
  });
}

// Event listener for the Add Transaction button (guarded)
const addTransactionBtn = document.getElementById("add-transaction-btn");
if (addTransactionBtn) {
  addTransactionBtn.addEventListener("click", addTransaction);
}

// Event listener for the Save Transaction button (guarded)
const saveTransactionBtn = document.getElementById("save-transaction-btn");
if (saveTransactionBtn) {
  saveTransactionBtn.addEventListener("click", saveTransaction);
}

// Initialize on page load (only for dashboard page)
document.addEventListener('DOMContentLoaded', function() {
  // Check if we're on the dashboard page by checking the URL
  const currentPage = window.location.pathname;
  const isDashboardPage = currentPage.includes('dashboard.html') || currentPage.includes('index.html') || currentPage.endsWith('/');
  
  console.log('=== PAGE LOAD DEBUG ===');
  console.log('Current page:', currentPage);
  console.log('Is dashboard page?', isDashboardPage);
  console.log('========================');
  
  // Only run authentication checks if we're on the dashboard page
  if (!isDashboardPage) {
    console.log('Not on dashboard page, skipping authentication checks');
    return;
  }
  
  console.log('=== Finance Tracker Initialization ===');
  console.log('Checking authentication status...');
  
  // Verify user is logged in
  if (!isUserLoggedIn()) {
    console.warn('User not logged in, redirecting to login page...');
    window.location.href = './login.html';
    return;
  }
  
  // Get username and verify it exists
  const username = getLoggedInUsername();
  if (!username) {
    console.error('Username not found, redirecting to login page...');
    window.location.href = './login.html';
    return;
  }
  
  console.log(`Authenticated user: ${username}`);
  console.log('Loading user-specific transactions from backend...');
  
  // Load transactions from backend API (user-specific) - ASYNC operation
  loadTransactions().then(() => {
    // Initial update of the balance and transaction table
    updateBalance();
    updateTransactionTable();
    
    console.log(`Successfully loaded ${transactions.length} transactions for user: ${username}`);
    console.log('=== Initialization Complete ===');
  }).catch(error => {
    console.error('Error initializing transactions:', error);
    alert('Warning: Could not load your transactions. Please refresh the page.');
  });
});

// Function to handle the download of data in PDF and CSV formats
function handleDownload() {
  // Prompt the user to select the export format
  const exportFormat = prompt("Select export format: PDF or CSV");
  
  if (!exportFormat) {
    return; // User cancelled
  }
  
  const format = exportFormat.toLowerCase().trim();

  if (format === "pdf") {
    // Call a function to export data to PDF
    exportToPDF();
  } else if (format === "csv") {
    // Call a function to export data to CSV
    exportToCSV();
  } else {
    showNotification('Invalid export format. Please enter either "PDF" or "CSV".', 'error');
  }
}

// Function to export data to PDF
function exportToPDF() {
  // Define the document content using pdfMake syntax
  const docDefinition = {
    content: [
      {
        table: {
          headerRows: 1,
          widths: ["auto", "*", "auto", "auto"],
          body: [
            [
              { text: "Date", style: "header" },
              { text: "Description", style: "header" },
              { text: "Amount", style: "header" },
              { text: "Type", style: "header" },
            ],
            // Add transaction data to the table body
            ...transactions.map((transaction) => {
              const date = formatDate(new Date(transaction.primeId));
              const description = transaction.description;
              const amount = transaction.amount;
              const type = transaction.type;

              return [date, description, amount.toString(), type];
            }),
          ],
        },
      },
    ],
    styles: {
      header: {
        fontSize: 12,
        bold: true,
        margin: [0, 5],
      },
    },
  };

  // Create the PDF document
  pdfMake.createPdf(docDefinition).download("transactions.pdf");
}

// Function to export data to CSV
function exportToCSV() {
  // Generate CSV content
  const csvContent =
    "Date,Description,Amount,Type\n" +
    transactions
      .map((transaction) => {
        const date = formatDate(new Date(transaction.primeId));
        const description = transaction.description;
        const amount = transaction.amount;
        const type = transaction.type;

        return `${date},${description},${amount},${type}`;
      })
      .join("\n");
  // Create a Blob with the CSV content
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });

  // Create a link element and trigger a click to download the CSV file
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "transactions.csv";
  link.click();
}

// Invite Section Functions
function sendEmailInvite() {
  const emailInput = document.getElementById('invite-email');
  const email = emailInput.value.trim();
  
  if (!email) {
    showNotification('Please enter an email address', 'error');
    return;
  }
  
  if (!isValidEmail(email)) {
    showNotification('Please enter a valid email address', 'error');
    return;
  }
  
  // Create mailto link with pre-filled subject and body
  const subject = 'Join our Personal Finance Tracker community!';
  const body = `Hi! I've been using this amazing Personal Finance Tracker app to manage my finances and I thought you might find it useful too. It helps you track income, expenses, and manage your budget effectively. Check it out: ${window.location.href}`;
  
  const mailtoLink = `mailto:${email}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  
  // Open the default email client
  window.location.href = mailtoLink;
  
  // Clear the input
  emailInput.value = '';
  
  // Show success message
  showNotification('Community invite sent!', 'success');
}

function shareOnLinkedIn() {
  const url = encodeURIComponent(window.location.href);
  const text = encodeURIComponent('Join our Personal Finance Tracker community! Track your finances effortlessly.');
  const linkedinUrl = `https://www.linkedin.com/sharing/share-offsite/?url=${url}`;
  
  // Open LinkedIn share dialog in a new window
  window.open(linkedinUrl, 'linkedin-share', 'width=600,height=400,scrollbars=yes,resizable=yes');
  
  showNotification('LinkedIn share opened!', 'success');
}

function shareOnTwitter() {
  const url = encodeURIComponent(window.location.href);
  const text = encodeURIComponent('Join our Personal Finance Tracker community! Track your finances effortlessly.');
  const twitterUrl = `https://twitter.com/intent/tweet?url=${url}&text=${text}`;
  
  // Open Twitter share dialog in a new window
  window.open(twitterUrl, 'twitter-share', 'width=600,height=400,scrollbars=yes,resizable=yes');
  
  showNotification('Community share opened!', 'success');
}

function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

function showNotification(message, type = 'info') {
  // Use the enhanced showToast function
  showToast(message, type);
}

// Add event listener for Enter key on email input
document.addEventListener('DOMContentLoaded', function() {
  const emailInput = document.getElementById('invite-email');
  if (emailInput) {
    emailInput.addEventListener('keypress', function(e) {
      if (e.key === 'Enter') {
        sendEmailInvite();
      }
    });
  }
});

// Search Bar Functionality
document.addEventListener('DOMContentLoaded', function() {
  const searchInput = document.getElementById('search-input');
  
  if (searchInput) {
    // Focus search bar on Cmd+K, Ctrl+K, or Ctrl+Shift+K
    document.addEventListener('keydown', function(e) {
      // Check for Cmd+K (Mac), Ctrl+K, or Ctrl+Shift+K (Windows/Linux)
      if ((e.metaKey || e.ctrlKey) && e.key === 'k' && !e.shiftKey) {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        
        // Focus the search input
        searchInput.focus();
        searchInput.select(); // Select all text for easy replacement
        
        // Show a brief visual feedback
        searchInput.style.borderColor = '#0b0081';
        setTimeout(() => {
          searchInput.style.borderColor = '';
        }, 1000);
        
        return false;
      }
      
      // Alternative: Ctrl+Shift+K (won't conflict with browser)
      if (e.ctrlKey && e.shiftKey && e.key === 'K') {
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        
        // Focus the search input
        searchInput.focus();
        searchInput.select();
        
        // Show a brief visual feedback
        searchInput.style.borderColor = '#0b0081';
        setTimeout(() => {
          searchInput.style.borderColor = '';
        }, 1000);
        
        return false;
      }
    }, true); // Use capture phase to intercept before browser default
    
    // Handle search functionality
    searchInput.addEventListener('input', function(e) {
      const query = e.target.value.toLowerCase().trim();
      
      if (query.length > 2) { // Only search if at least 3 characters
        performSearch(query);
      } else if (query.length === 0) {
        // Clear highlights when search is empty
        const previousHighlights = document.querySelectorAll('.search-highlight');
        previousHighlights.forEach(el => {
          el.classList.remove('search-highlight');
        });
      }
    });
    
    // Handle Enter key in search
    searchInput.addEventListener('keypress', function(e) {
      if (e.key === 'Enter') {
        const query = e.target.value.toLowerCase().trim();
        if (query.length > 0) {
          performSearch(query);
        }
      }
    });
  }
});

// Search functionality
function performSearch(query) {
  
  // Clear previous highlights
  const previousHighlights = document.querySelectorAll('.search-highlight');
  previousHighlights.forEach(el => {
    el.classList.remove('search-highlight');
  });
  
  // Get all text elements that can be searched
  const searchableElements = document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div, a, li, td, th, button, input[placeholder]');
  let foundResults = false;
  let resultCount = 0;
  let firstResult = null;
  const searchResults = [];
  
  searchableElements.forEach(element => {
    const text = element.textContent.toLowerCase();
    const placeholder = element.placeholder ? element.placeholder.toLowerCase() : '';
    
    if (text.includes(query) || placeholder.includes(query)) {
      // Highlight the element
      element.classList.add('search-highlight');
      foundResults = true;
      resultCount++;
      
      // Store result info for enhanced display
      searchResults.push({
        element: element,
        text: element.textContent.trim(),
        tagName: element.tagName.toLowerCase(),
        type: element.placeholder ? 'input' : element.tagName.toLowerCase()
      });
      
      // Store the first result for scrolling
      if (!firstResult) {
        firstResult = element;
      }
    }
  });
  
  if (foundResults) {
    showEnhancedSearchNotification(searchResults, query, 'success');
    
    // Scroll to first result with enhanced animation
    if (firstResult) {
      setTimeout(() => {
        firstResult.scrollIntoView({ 
          behavior: 'smooth', 
          block: 'center',
          inline: 'nearest'
        });
        // Add focus effect
        firstResult.classList.add('search-focus');
        setTimeout(() => firstResult.classList.remove('search-focus'), 2000);
      }, 300);
    }
  } else {
    showEnhancedSearchNotification([], query, 'error');
  }
}

// Enhanced search notification function
function showEnhancedSearchNotification(results, query, type) {
  // Remove existing notification
  const existingNotification = document.getElementById('search-notification');
  if (existingNotification) {
    existingNotification.remove();
  }
  
  const notification = document.createElement('div');
  notification.id = 'search-notification';
  notification.style.cssText = `
    position: fixed;
    top: 80px;
    right: 20px;
    background: ${type === 'success' ? 'linear-gradient(135deg, #0b0081 0%, #1a1a8a 100%)' : 'linear-gradient(135deg, #dc3545 0%, #c82333 100%)'};
    color: white;
    padding: 20px;
    border-radius: 12px;
    font-size: 14px;
    z-index: 1000;
    box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    max-width: 350px;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255,255,255,0.1);
    animation: slideInRight 0.3s ease-out;
  `;
  
  if (type === 'success' && results.length > 0) {
    notification.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 15px;">
        <i class="fas fa-search" style="color: #f4ff61; font-size: 16px;"></i>
        <strong>${results.length} result${results.length > 1 ? 's' : ''} found</strong>
      </div>
      <div style="max-height: 200px; overflow-y: auto;">
        ${results.slice(0, 5).map(result => `
          <div style="padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.1);">
            <div style="color: #f4ff61; font-weight: bold; text-transform: uppercase; font-size: 11px; margin-bottom: 4px;">
              ${result.type === 'input' ? 'INPUT FIELD' : result.tagName}
            </div>
            <div style="font-size: 13px; opacity: 0.9; line-height: 1.4;">
              ${result.text.substring(0, 80)}${result.text.length > 80 ? '...' : ''}
            </div>
          </div>
        `).join('')}
        ${results.length > 5 ? `
          <div style="text-align: center; margin-top: 10px; font-size: 12px; opacity: 0.7;">
            +${results.length - 5} more results
          </div>
        ` : ''}
      </div>
    `;
  } else {
    notification.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <i class="fas fa-exclamation-triangle" style="color: #ffc107; font-size: 16px;"></i>
        <div>
          <strong>No results found</strong>
          <div style="font-size: 12px; margin-top: 4px; opacity: 0.8;">
            Try different keywords or check spelling
          </div>
        </div>
      </div>
    `;
  }
  
  document.body.appendChild(notification);
  
  // Auto-hide after 5 seconds
  setTimeout(() => {
    if (notification.parentNode) {
      notification.style.animation = 'slideOutRight 0.3s ease-in';
      setTimeout(() => notification.remove(), 300);
    }
  }, 5000);
}

// Loading States and Animations
function showLoadingState(element, type = 'button') {
  if (type === 'button') {
    element.classList.add('btn-loading');
    element.disabled = true;
  } else if (type === 'form') {
    element.classList.add('form-loading');
  } else if (type === 'page') {
    element.classList.add('loading');
  }
}

function hideLoadingState(element, type = 'button') {
  if (type === 'button') {
    element.classList.remove('btn-loading');
    element.disabled = false;
  } else if (type === 'form') {
    element.classList.remove('form-loading');
  } else if (type === 'page') {
    element.classList.remove('loading');
  }
}

// Enhanced form submission with loading states
function handleFormSubmission(form, callback) {
  const submitButton = form.querySelector('button[type="submit"]');
  
  if (submitButton) {
    showLoadingState(submitButton, 'button');
    showLoadingState(form, 'form');
    
    // Simulate processing time
    setTimeout(() => {
      if (callback) callback();
      hideLoadingState(submitButton, 'button');
      hideLoadingState(form, 'form');
    }, 2000);
  }
}

// Enhanced Form Validation and User Feedback
function validateForm(form) {
  const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
  let isValid = true;
  const errors = [];
  
  inputs.forEach(input => {
    const value = input.value.trim();
    const fieldName = input.name || input.id || 'field';
    
    // Clear previous errors
    clearFieldError(input);
    
    // Required field validation
    if (!value) {
      showFieldError(input, `${fieldName} is required`);
      errors.push(`${fieldName} is required`);
      isValid = false;
      return;
    }
    
    // Email validation
    if (input.type === 'email' && value) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(value)) {
        showFieldError(input, 'Please enter a valid email address');
        errors.push('Invalid email format');
        isValid = false;
      }
    }
    
    // Password validation
    if (input.type === 'password' && value) {
      if (value.length < 8) {
        showFieldError(input, 'Password must be at least 8 characters long');
        errors.push('Password too short');
        isValid = false;
      }
    }
    
    // Phone validation
    if (input.type === 'tel' && value) {
      const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
      if (!phoneRegex.test(value.replace(/[\s\-\(\)]/g, ''))) {
        showFieldError(input, 'Please enter a valid phone number');
        errors.push('Invalid phone number');
        isValid = false;
      }
    }
  });
  
  return { isValid, errors };
}

function showFieldError(input, message) {
  const errorDiv = document.createElement('div');
  errorDiv.className = 'field-error';
  errorDiv.textContent = message;
  errorDiv.style.cssText = `
    color: #dc3545;
    font-size: 12px;
    margin-top: 4px;
    animation: fadeInUp 0.3s ease-out;
  `;
  
  input.classList.add('error');
  input.style.borderColor = '#dc3545';
  
  // Insert error after input
  input.parentNode.insertBefore(errorDiv, input.nextSibling);
}

function clearFieldError(input) {
  input.classList.remove('error');
  input.style.borderColor = '';
  
  const existingError = input.parentNode.querySelector('.field-error');
  if (existingError) {
    existingError.remove();
  }
}

function showSuccessMessage(message) {
  const notification = document.createElement('div');
  notification.style.cssText = `
    position: fixed;
    top: 80px;
    right: 20px;
    background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
    color: white;
    padding: 15px 20px;
    border-radius: 12px;
    font-size: 14px;
    z-index: 1000;
    box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255,255,255,0.1);
    animation: slideInRight 0.3s ease-out;
  `;
  
  notification.innerHTML = `
    <div style="display: flex; align-items: center; gap: 10px;">
      <i class="fas fa-check-circle" style="color: #ffffff; font-size: 16px;"></i>
      <strong>${message}</strong>
    </div>
  `;
  
  document.body.appendChild(notification);
  
  setTimeout(() => {
    notification.style.animation = 'slideOutRight 0.3s ease-in';
    setTimeout(() => notification.remove(), 300);
  }, 4000);
}

function showErrorMessage(message) {
  const notification = document.createElement('div');
  notification.style.cssText = `
    position: fixed;
    top: 80px;
    right: 20px;
    background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
    color: white;
    padding: 15px 20px;
    border-radius: 12px;
    font-size: 14px;
    z-index: 1000;
    box-shadow: 0 8px 25px rgba(0,0,0,0.15);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255,255,255,0.1);
    animation: slideInRight 0.3s ease-out;
  `;
  
  notification.innerHTML = `
    <div style="display: flex; align-items: center; gap: 10px;">
      <i class="fas fa-exclamation-circle" style="color: #ffc107; font-size: 16px;"></i>
      <strong>${message}</strong>
    </div>
  `;
  
  document.body.appendChild(notification);
  
  setTimeout(() => {
    notification.style.animation = 'slideOutRight 0.3s ease-in';
    setTimeout(() => notification.remove(), 300);
  }, 5000);
}

// Keyboard Navigation Support
function initKeyboardNavigation() {
  // Tab navigation enhancement
  document.addEventListener('keydown', function(e) {
    // Skip to main content with Tab
    if (e.key === 'Tab' && !e.shiftKey && document.activeElement === document.body) {
      const mainContent = document.querySelector('main, .main-content, .section-box');
      if (mainContent) {
        mainContent.focus();
        e.preventDefault();
      }
    }
    
    // Escape key to close modals/menus
    if (e.key === 'Escape') {
      // Close mobile menu
      const navLinks = document.querySelector('.nav-links');
      const menuToggle = document.getElementById('menu-toggle');
      if (navLinks && navLinks.classList.contains('active')) {
        navLinks.classList.remove('active');
        const icon = menuToggle.querySelector('i');
        icon.classList.remove('fa-times');
        icon.classList.add('fa-bars');
      }
      
      // Clear search
      const searchInput = document.getElementById('search-input');
      if (searchInput && document.activeElement === searchInput) {
        searchInput.value = '';
        clearSearchHighlights();
        hideSearchResults();
      }
    }
    
    // Arrow key navigation for lists
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      const activeElement = document.activeElement;
      const listItems = Array.from(document.querySelectorAll('li, .nav-links a, .benefit-item'));
      const currentIndex = listItems.indexOf(activeElement);
      
      if (currentIndex !== -1) {
        e.preventDefault();
        const nextIndex = e.key === 'ArrowDown' 
          ? (currentIndex + 1) % listItems.length
          : (currentIndex - 1 + listItems.length) % listItems.length;
        
        listItems[nextIndex].focus();
      }
    }
    
    // Enter key to activate focused elements
    if (e.key === 'Enter') {
      const activeElement = document.activeElement;
      if (activeElement.tagName === 'A' || activeElement.tagName === 'BUTTON') {
        activeElement.click();
      }
    }
  });
  
  // Focus management for better accessibility
  const focusableElements = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
  
  // Trap focus in modals
  function trapFocus(element) {
    const focusableContent = element.querySelectorAll(focusableElements);
    const firstFocusableElement = focusableContent[0];
    const lastFocusableElement = focusableContent[focusableContent.length - 1];
    
    element.addEventListener('keydown', function(e) {
      if (e.key === 'Tab') {
        if (e.shiftKey) {
          if (document.activeElement === firstFocusableElement) {
            lastFocusableElement.focus();
            e.preventDefault();
          }
        } else {
          if (document.activeElement === lastFocusableElement) {
            firstFocusableElement.focus();
            e.preventDefault();
          }
        }
      }
    });
  }
  
  // Apply focus trap to mobile menu
  const navLinks = document.querySelector('.nav-links');
  if (navLinks) {
    trapFocus(navLinks);
  }
}

// Enhanced keyboard shortcuts
function initKeyboardShortcuts() {
  document.addEventListener('keydown', function(e) {
    // Alt + H for Home
    if (e.altKey && e.key === 'h') {
      e.preventDefault();
      const homeLink = document.querySelector('a[href="#"], a[href="./index.html"]');
      if (homeLink) homeLink.click();
    }
    
    // Alt + S for Support
    if (e.altKey && e.key === 's') {
      e.preventDefault();
      const supportLink = document.querySelector('a[href="./support.html"]');
      if (supportLink) supportLink.click();
    }
    
    // Alt + L for Login
    if (e.altKey && e.key === 'l') {
      e.preventDefault();
      const loginLink = document.querySelector('a[href="./login.html"]');
      if (loginLink) loginLink.click();
    }
    
    // Alt + U for Sign Up
    if (e.altKey && e.key === 'u') {
      e.preventDefault();
      const signupLink = document.querySelector('a[href="./sign-up.html"]');
      if (signupLink) signupLink.click();
    }
  });
}

// Page transition effects
function addPageTransition() {
  document.body.classList.add('page-enter');
  
  // Remove class after animation
  setTimeout(() => {
    document.body.classList.remove('page-enter');
  }, 500);
}

// Scroll to first search result
function scrollToFirstResult() {
  const firstResult = document.querySelector('.search-highlight');
  if (firstResult) {
    firstResult.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
}

// Show search notification
function showSearchNotification(message, type = 'info') {
  // Create notification element
  const notification = document.createElement('div');
  notification.className = `search-notification search-notification-${type}`;
  notification.textContent = message;
  
  // Style the notification
  notification.style.cssText = `
    position: fixed;
    top: 80px;
    right: 20px;
    background: ${type === 'success' ? '#28a745' : '#17a2b8'};
    color: white;
    padding: 12px 20px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    z-index: 10000;
    font-weight: 500;
    animation: slideInSearch 0.3s ease-out;
    max-width: 300px;
  `;
  
  // Add animation keyframes for search notifications
  if (!document.getElementById('search-notification-styles')) {
    const style = document.createElement('style');
    style.id = 'search-notification-styles';
    style.textContent = `
      @keyframes slideInSearch {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
      }
      @keyframes slideOutSearch {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
      }
      .search-highlight {
        background-color: #f4ff61 !important;
        color: #0b0081 !important;
        padding: 2px 4px;
        border-radius: 3px;
      }
    `;
    document.head.appendChild(style);
  }
  
  // Add to page
  document.body.appendChild(notification);
  
  // Remove after 3 seconds
  setTimeout(() => {
    notification.style.animation = 'slideOutSearch 0.3s ease-in';
    setTimeout(() => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    }, 300);
  }, 3000);
}