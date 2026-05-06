/* ============================================================================
   DASHBOARD.JS - Dashboard Functionality and Data Visualization
   ============================================================================
   
   This file handles:
   - User data fetching and display
   - Ticket statistics calculation
   - Chart.js visualization (pie and bar charts)
   - Activity timeline generation
   - Ticket summary cards creation
   - Profile summary display
   - Quick action handlers
   - Loading states and error handling
   ============================================================================ */

// Dashboard Manager - Main controller
const DashboardManager = {
    user: null,
    tickets: [],
    stats: {
        total: 0,
        open: 0,
        resolved: 0,
        pending: 0
    },
    charts: {
        statusChart: null,
        priorityChart: null
    },

    // Initialize dashboard
    async init() {
        console.log('DashboardManager: Initializing dashboard...');
        
        try {
            // Check authentication
            if (!AuthManager.isAuthenticated()) {
                console.warn('DashboardManager: User not authenticated, redirecting to login');
                showToast('Please login to access the dashboard', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 1500);
                return;
            }

            // Get user data
            this.user = UserManager.getUser();
            console.log('DashboardManager: User data retrieved', this.user);

            // Display user greeting
            this.displayGreeting();

            // Fetch tickets
            await this.fetchTickets();

            // Calculate statistics
            this.calculateStats();

            // Render all components
            this.renderStats();
            this.renderActivityTimeline();
            this.renderTicketCards();
            this.renderCharts();
            this.renderProfileSummary();

            console.log('DashboardManager: Dashboard initialized successfully');
        } catch (error) {
            console.error('DashboardManager: Error initializing dashboard:', error);
            showToast('Failed to load dashboard data', 'error');
            this.showErrorState();
        }
    },

    // Display user greeting
    displayGreeting() {
        const greetingElement = document.getElementById('dynamic-greeting-dashboard');
        if (!greetingElement) return;

        const username = this.user?.name || this.user?.username || 'User';
        const hour = new Date().getHours();
        let greeting = 'Hello';

        if (hour < 12) greeting = 'Good Morning';
        else if (hour < 18) greeting = 'Good Afternoon';
        else greeting = 'Good Evening';

        greetingElement.innerHTML = `
            <h1>${greeting}, <span class="username">${username}</span><span class="emoji">👋</span></h1>
            <p>Welcome to your Finance Tracker dashboard</p>
        `;
    },

    // Fetch tickets from API
    async fetchTickets() {
        try {
            console.log('DashboardManager: Fetching tickets...');
            const response = await apiService.support.getMyTickets();
            
            if (response.success && response.data) {
                this.tickets = response.data;
                console.log(`DashboardManager: Fetched ${this.tickets.length} tickets`);
            } else {
                throw new Error('Failed to fetch tickets');
            }
        } catch (error) {
            console.error('DashboardManager: Error fetching tickets:', error);
            this.tickets = [];
            throw error;
        }
    },

    // Calculate ticket statistics
    calculateStats() {
        this.stats = {
            total: this.tickets.length,
            open: this.tickets.filter(t => t.status === 'open').length,
            resolved: this.tickets.filter(t => t.status === 'resolved' || t.status === 'closed').length,
            pending: this.tickets.filter(t => t.status === 'pending').length
        };
        console.log('DashboardManager: Statistics calculated', this.stats);
    },

    // Render statistics cards
    renderStats() {
        const elements = {
            total: document.getElementById('total-tickets'),
            open: document.getElementById('open-tickets'),
            resolved: document.getElementById('resolved-tickets'),
            pending: document.getElementById('pending-tickets')
        };

        Object.keys(elements).forEach(key => {
            if (elements[key]) {
                elements[key].textContent = this.stats[key];
                elements[key].classList.add('animate-count');
            }
        });
    },

    // Render activity timeline
    renderActivityTimeline() {
        const timelineContainer = document.getElementById('activity-timeline');
        if (!timelineContainer) return;

        if (this.tickets.length === 0) {
            timelineContainer.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <h3>No Activity Yet</h3>
                    <p>Your recent activity will appear here</p>
                </div>
            `;
            return;
        }

        // Create activity items from tickets (sorted by date, most recent first)
        const activities = this.createActivitiesFromTickets();
        
        timelineContainer.innerHTML = activities.map(activity => `
            <div class="activity-item activity-${activity.type}">
                <div class="activity-header">
                    <div class="activity-icon">
                        <i class="${activity.icon}"></i>
                    </div>
                    <div class="activity-title">${activity.title}</div>
                </div>
                <div class="activity-description">${activity.description}</div>
                <div class="activity-time">${activity.time}</div>
            </div>
        `).join('');
    },

    // Create activities from tickets
    createActivitiesFromTickets() {
        const activities = [];

        // Sort tickets by date (most recent first)
        const sortedTickets = [...this.tickets].sort((a, b) => {
            return new Date(b.createdAt || b.created_at || 0) - new Date(a.createdAt || a.created_at || 0);
        });

        // Take the 10 most recent tickets
        sortedTickets.slice(0, 10).forEach(ticket => {
            const status = ticket.status?.toLowerCase();
            let type = 'created';
            let icon = 'fas fa-plus';
            let title = 'Ticket Created';

            if (status === 'resolved' || status === 'closed') {
                type = 'resolved';
                icon = 'fas fa-check';
                title = 'Ticket Resolved';
            } else if (status === 'pending') {
                type = 'updated';
                icon = 'fas fa-clock';
                title = 'Ticket Pending';
            } else if (status === 'in progress') {
                type = 'updated';
                icon = 'fas fa-sync';
                title = 'Ticket In Progress';
            }

            activities.push({
                type,
                icon,
                title,
                description: `${ticket.subject || 'Support Request'} - Ticket #${ticket.id || ticket.ticketNumber || 'N/A'}`,
                time: this.formatRelativeTime(ticket.createdAt || ticket.created_at)
            });
        });

        return activities;
    },

    // Format relative time
    formatRelativeTime(dateString) {
        if (!dateString) return 'Recently';

        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        
        return date.toLocaleDateString();
    },

    // Render ticket summary cards
    renderTicketCards() {
        const cardsContainer = document.getElementById('tickets-summary-cards');
        if (!cardsContainer) return;

        if (this.tickets.length === 0) {
            cardsContainer.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-ticket-alt"></i>
                    <h3>No Tickets Yet</h3>
                    <p>Create your first support ticket to get started</p>
                    <button class="btn" onclick="createNewTicket()">
                        <i class="fas fa-plus"></i> Create Ticket
                    </button>
                </div>
            `;
            return;
        }

        // Show the 5 most recent tickets
        const recentTickets = [...this.tickets]
            .sort((a, b) => new Date(b.createdAt || b.created_at || 0) - new Date(a.createdAt || a.created_at || 0))
            .slice(0, 5);

        cardsContainer.innerHTML = recentTickets.map(ticket => {
            const priority = (ticket.priority || 'medium').toLowerCase();
            const status = (ticket.status || 'open').toLowerCase();
            const statusBadge = this.getStatusBadge(status);
            const priorityBadge = this.getPriorityBadge(priority);

            return `
                <div class="ticket-card priority-${priority}" onclick="viewTicketDetails(${ticket.id || ticket.ticketNumber})">
                    <div class="ticket-card-header">
                        <span class="ticket-number">#${ticket.id || ticket.ticketNumber || 'N/A'}</span>
                        <div class="ticket-badges">
                            ${statusBadge}
                            ${priorityBadge}
                        </div>
                    </div>
                    <div class="ticket-subject">${ticket.subject || 'Support Request'}</div>
                    <div class="ticket-meta">
                        <span class="ticket-date">
                            <i class="fas fa-calendar"></i>
                            ${this.formatDate(ticket.createdAt || ticket.created_at)}
                        </span>
                    </div>
                </div>
            `;
        }).join('');
    },

    // Get status badge HTML
    getStatusBadge(status) {
        const statusColors = {
            'open': 'info',
            'pending': 'warning',
            'in progress': 'info',
            'resolved': 'success',
            'closed': 'secondary'
        };
        const color = statusColors[status] || 'secondary';
        return `<span class="badge badge-${color}">${status}</span>`;
    },

    // Get priority badge HTML
    getPriorityBadge(priority) {
        const priorityColors = {
            'high': 'danger',
            'medium': 'warning',
            'low': 'success'
        };
        const color = priorityColors[priority] || 'secondary';
        return `<span class="badge badge-${color}">${priority}</span>`;
    },

    // Format date
    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    },

    // Render charts
    renderCharts() {
        this.renderStatusChart();
        this.renderPriorityChart();
    },

    // Render status pie chart
    renderStatusChart() {
        const canvas = document.getElementById('ticketStatusChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');

        // Destroy existing chart if it exists
        if (this.charts.statusChart) {
            this.charts.statusChart.destroy();
        }

        // Calculate status distribution
        const statusData = {
            'Open': this.tickets.filter(t => t.status === 'open').length,
            'Pending': this.tickets.filter(t => t.status === 'pending').length,
            'Resolved': this.tickets.filter(t => (t.status === 'resolved' || t.status === 'closed')).length
        };

        this.charts.statusChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: Object.keys(statusData),
                datasets: [{
                    data: Object.values(statusData),
                    backgroundColor: [
                        '#17a2b8', // Open - info
                        '#ffc107', // Pending - warning
                        '#28a745'  // Resolved - success
                    ],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 15,
                            font: {
                                size: 12,
                                family: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif"
                            }
                        }
                    },
                    title: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                                return `${label}: ${value} (${percentage}%)`;
                            }
                        }
                    }
                }
            }
        });
    },

    // Render priority bar chart
    renderPriorityChart() {
        const canvas = document.getElementById('ticketPriorityChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');

        // Destroy existing chart if it exists
        if (this.charts.priorityChart) {
            this.charts.priorityChart.destroy();
        }

        // Calculate priority distribution
        const priorityData = {
            'High': this.tickets.filter(t => (t.priority || '').toLowerCase() === 'high').length,
            'Medium': this.tickets.filter(t => (t.priority || '').toLowerCase() === 'medium').length,
            'Low': this.tickets.filter(t => (t.priority || '').toLowerCase() === 'low').length
        };

        this.charts.priorityChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: Object.keys(priorityData),
                datasets: [{
                    label: 'Tickets',
                    data: Object.values(priorityData),
                    backgroundColor: [
                        '#dc3545', // High - danger
                        '#ffc107', // Medium - warning
                        '#28a745'  // Low - success
                    ],
                    borderWidth: 0,
                    borderRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    title: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1,
                            font: {
                                size: 11
                            }
                        },
                        grid: {
                            color: 'rgba(0, 0, 0, 0.05)'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                size: 12,
                                weight: 'bold'
                            }
                        }
                    }
                }
            }
        });
    },

    // Render profile summary
    renderProfileSummary() {
        const profileCard = document.getElementById('profile-summary-card');
        if (!profileCard) return;

        const username = this.user?.name || this.user?.username || 'User';
        const email = this.user?.email || 'user@example.com';
        const initials = this.getInitials(username);
        const memberSince = this.user?.createdAt || this.user?.created_at || new Date().toISOString();

        profileCard.innerHTML = `
            <div class="profile-avatar">
                ${initials}
            </div>
            <div class="profile-name">${username}</div>
            <div class="profile-email">${email}</div>
            <div class="profile-stats">
                <div class="profile-stat">
                    <div class="profile-stat-value">${this.stats.total}</div>
                    <div class="profile-stat-label">Tickets</div>
                </div>
                <div class="profile-stat">
                    <div class="profile-stat-value">${this.stats.resolved}</div>
                    <div class="profile-stat-label">Resolved</div>
                </div>
            </div>
            <div class="profile-actions">
                <button class="profile-btn profile-btn-primary" onclick="viewProfile()">
                    <i class="fas fa-user"></i> View Profile
                </button>
                <button class="profile-btn profile-btn-secondary" onclick="changePassword()">
                    <i class="fas fa-key"></i> Change Password
                </button>
            </div>
        `;
    },

    // Get user initials
    getInitials(name) {
        if (!name) return 'U';
        const parts = name.trim().split(' ');
        if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    },

    // Show error state
    showErrorState() {
        const containers = [
            document.getElementById('activity-timeline'),
            document.getElementById('tickets-summary-cards'),
            document.getElementById('profile-summary-card')
        ];

        containers.forEach(container => {
            if (container) {
                container.innerHTML = `
                    <div class="empty-state">
                        <i class="fas fa-exclamation-triangle"></i>
                        <h3>Error Loading Data</h3>
                        <p>Please try refreshing the page</p>
                        <button class="btn" onclick="location.reload()">
                            <i class="fas fa-sync"></i> Refresh
                        </button>
                    </div>
                `;
            }
        });
    },

    // Refresh dashboard data
    async refresh() {
        console.log('DashboardManager: Refreshing dashboard...');
        showToast('Refreshing dashboard...', 'info');
        await this.init();
        showToast('Dashboard refreshed successfully', 'success');
    }
};

// ============================================================================
// QUICK ACTION HANDLERS
// ============================================================================

// Create new ticket
function createNewTicket() {
    console.log('createNewTicket: Redirecting to support page');
    window.location.href = 'support.html';
}

// View profile
function viewProfile() {
    console.log('viewProfile: Opening profile modal');
    if (typeof openEditProfile === 'function') {
        openEditProfile();
    } else {
        showToast('Profile feature coming soon', 'info');
    }
}

// Change password
function changePassword() {
    console.log('changePassword: Opening change password modal');
    if (typeof openChangePassword === 'function') {
        openChangePassword();
    } else {
        showToast('Password change feature coming soon', 'info');
    }
}

// View all tickets
function viewAllTickets() {
    console.log('viewAllTickets: Opening all tickets view');
    showToast('All tickets feature coming soon', 'info');
    // TODO: Implement all tickets page or modal
}

// View ticket details
function viewTicketDetails(ticketId) {
    console.log('viewTicketDetails: Opening ticket details for ID:', ticketId);
    showToast(`Viewing ticket #${ticketId}`, 'info');
    // TODO: Implement ticket details modal
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

// Show toast notification (fallback if not defined in script.js)
if (typeof showToast !== 'function') {
    function showToast(message, type = 'info') {
        console.log(`Toast [${type}]:`, message);
        alert(message);
    }
}

// ============================================================================
// PAGE INITIALIZATION
// ============================================================================

// Initialize dashboard when DOM is ready
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Dashboard: DOM loaded, initializing...');
    
    try {
        // Initialize dashboard
        await DashboardManager.init();
        
        // Add refresh button listener if exists
        const refreshBtn = document.getElementById('refresh-dashboard');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', () => DashboardManager.refresh());
        }
        
        console.log('Dashboard: Initialization complete');
    } catch (error) {
        console.error('Dashboard: Initialization error:', error);
    }
});

// Refresh dashboard when window gains focus
window.addEventListener('focus', () => {
    console.log('Dashboard: Window focused, checking for updates');
    // Optional: Auto-refresh when user returns to tab
    // DashboardManager.refresh();
});

// Handle visibility change
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
        console.log('Dashboard: Page visible again');
        // Optional: Auto-refresh when tab becomes visible
        // DashboardManager.refresh();
    }
});

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { DashboardManager };
}

// Also expose to window for global access
window.DashboardManager = DashboardManager;

console.log('dashboard.js loaded successfully');
