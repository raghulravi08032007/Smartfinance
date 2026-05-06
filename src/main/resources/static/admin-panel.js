/* ============================================================================
   ADMIN PANEL JAVASCRIPT
   Handles admin dashboard functionality, user management, ticket management,
   and analytics visualization.
   ============================================================================ */

// Admin Manager - Main controller
const AdminManager = {
    currentTab: 'users',
    users: [],
    tickets: [],
    stats: {},
    pagination: {
        users: { page: 0, size: 10, total: 0 },
        tickets: { page: 0, size: 10, total: 0 }
    },
    sorting: {
        users: { field: 'id', direction: 'ASC' },
        tickets: { field: 'createdDate', direction: 'DESC' }
    },
    charts: {
        users: null,
        ticketStatus: null,
        ticketPriority: null
    },

    // Initialize admin panel
    async init() {
        console.log('AdminManager: Initializing admin panel...');
        
        try {
            // Check if user is admin
            if (!this.checkAdminAccess()) {
                showToast('Access denied. Admin privileges required.', 'error');
                setTimeout(() => {
                    window.location.href = 'index.html';
                }, 2000);
                return;
            }

            // Load initial data
            await this.loadDashboardStats();
            await this.loadUsers();
            await this.switchTab('users');
            
            console.log('AdminManager: Initialization complete');
        } catch (error) {
            console.error('AdminManager: Initialization error:', error);
            showToast('Failed to load admin panel', 'error');
        }
    },

    // Check if current user has admin access
    checkAdminAccess() {
        if (!AuthManager.isAuthenticated()) {
            return false;
        }

        const user = UserManager.getUser();
        // Check if user has ADMIN role
        return user && user.roles && user.roles.some(role => 
            role === 'ROLE_ADMIN' || role.name === 'ROLE_ADMIN'
        );
    },

    // Load dashboard statistics
    async loadDashboardStats() {
        try {
            const response = await apiService.admin.getDashboardStats();
            
            if (response.success && response.data) {
                this.stats = response.data;
                this.renderDashboardStats();
            }
        } catch (error) {
            console.error('AdminManager: Error loading dashboard stats:', error);
        }
    },

    // Render dashboard statistics
    renderDashboardStats() {
        const stats = this.stats;
        
        document.getElementById('total-users-count').textContent = stats.totalUsers || 0;
        document.getElementById('active-users-count').textContent = stats.activeUsers || 0;
        document.getElementById('total-tickets-count').textContent = stats.totalTickets || 0;
        document.getElementById('open-tickets-count').textContent = stats.openTickets || 0;
        document.getElementById('resolved-tickets-count').textContent = stats.resolvedTickets || 0;
        document.getElementById('pending-tickets-count').textContent = stats.pendingTickets || 0;
        
        // Calculate resolution rate
        const totalTickets = stats.totalTickets || 1;
        const resolvedTickets = stats.resolvedTickets || 0;
        const resolutionRate = Math.round((resolvedTickets / totalTickets) * 100);
        document.getElementById('resolution-rate').textContent = resolutionRate;
    },

    // Load users with pagination
    async loadUsers() {
        try {
            const { page, size } = this.pagination.users;
            const { field, direction } = this.sorting.users;
            
            const response = await apiService.admin.getAllUsers(page, size, field, direction);
            
            if (response.success && response.data) {
                this.users = response.data.content || response.data;
                this.pagination.users.total = response.data.totalPages || 1;
                this.renderUsersTable();
                this.renderUsersPagination();
            }
        } catch (error) {
            console.error('AdminManager: Error loading users:', error);
            showToast('Failed to load users', 'error');
        }
    },

    // Render users table
    renderUsersTable() {
        const tbody = document.getElementById('users-table-body');
        
        if (this.users.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 40px;">
                        <i class="fas fa-users" style="font-size: 48px; color: #ddd; margin-bottom: 10px;"></i>
                        <p style="color: #999;">No users found</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = this.users.map(user => `
            <tr>
                <td>${user.id}</td>
                <td><strong>${user.username}</strong></td>
                <td>${user.email}</td>
                <td>
                    ${this.formatRoles(user.roles)}
                </td>
                <td>
                    ${this.getUserStatusBadges(user)}
                </td>
                <td>${this.formatDate(user.createdDate)}</td>
                <td>
                    <div style="display: flex; gap: 5px; flex-wrap: wrap;">
                        <button class="btn-action-small" onclick="AdminManager.viewUserDetails(${user.id})">
                            <i class="fas fa-eye"></i> View
                        </button>
                        ${user.active ? 
                            `<button class="btn-action-small btn-warning" onclick="AdminManager.deactivateUser(${user.id})">
                                <i class="fas fa-ban"></i> Deactivate
                            </button>` :
                            `<button class="btn-action-small btn-success" onclick="AdminManager.activateUser(${user.id})">
                                <i class="fas fa-check"></i> Activate
                            </button>`
                        }
                        ${user.accountLocked ?
                            `<button class="btn-action-small btn-success" onclick="AdminManager.unlockUser(${user.id})">
                                <i class="fas fa-unlock"></i> Unlock
                            </button>` :
                            `<button class="btn-action-small btn-warning" onclick="AdminManager.lockUser(${user.id})">
                                <i class="fas fa-lock"></i> Lock
                            </button>`
                        }
                        <button class="btn-action-small btn-danger" onclick="AdminManager.deleteUser(${user.id})">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    // Format roles for display
    formatRoles(roles) {
        if (!roles || roles.length === 0) return '<span class="badge badge-secondary">USER</span>';
        
        return roles.map(role => {
            const roleName = role.name || role;
            const displayName = roleName.replace('ROLE_', '');
            const badgeClass = roleName.includes('ADMIN') ? 'badge-danger' : 'badge-primary';
            return `<span class="badge ${badgeClass}">${displayName}</span>`;
        }).join(' ');
    },

    // Get user status badges
    getUserStatusBadges(user) {
        let badges = [];
        
        if (user.active) {
            badges.push('<span class="badge badge-success">Active</span>');
        } else {
            badges.push('<span class="badge badge-danger">Inactive</span>');
        }
        
        if (user.accountLocked) {
            badges.push('<span class="badge badge-warning">Locked</span>');
        }
        
        return badges.join(' ');
    },

    // Load tickets with pagination and filtering
    async loadTickets(status = null, priority = null) {
        try {
            const { page, size } = this.pagination.tickets;
            const { field, direction } = this.sorting.tickets;
            
            const response = await apiService.admin.getAllTickets(page, size, field, direction, status, priority);
            
            if (response.success && response.data) {
                this.tickets = response.data.content || response.data;
                this.pagination.tickets.total = response.data.totalPages || 1;
                this.renderTicketsTable();
                this.renderTicketsPagination();
            }
        } catch (error) {
            console.error('AdminManager: Error loading tickets:', error);
            showToast('Failed to load tickets', 'error');
        }
    },

    // Render tickets table
    renderTicketsTable() {
        const tbody = document.getElementById('tickets-table-body');
        
        if (this.tickets.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; padding: 40px;">
                        <i class="fas fa-ticket-alt" style="font-size: 48px; color: #ddd; margin-bottom: 10px;"></i>
                        <p style="color: #999;">No tickets found</p>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = this.tickets.map(ticket => `
            <tr>
                <td><strong>${ticket.ticketNumber}</strong></td>
                <td>
                    <div style="max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        ${ticket.subject}
                    </div>
                </td>
                <td>${this.getStatusBadge(ticket.status)}</td>
                <td>${this.getPriorityBadge(ticket.priority)}</td>
                <td>${ticket.assignedTo || '<span style="color: #999;">Unassigned</span>'}</td>
                <td>${this.formatDate(ticket.createdDate)}</td>
                <td>
                    <div style="display: flex; gap: 5px; flex-wrap: wrap;">
                        <button class="btn-action-small" onclick="AdminManager.viewTicketDetails(${ticket.id})">
                            <i class="fas fa-eye"></i> View
                        </button>
                        ${ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' ?
                            `<button class="btn-action-small btn-success" onclick="AdminManager.resolveTicket(${ticket.id})">
                                <i class="fas fa-check"></i> Resolve
                            </button>` : ''
                        }
                        <button class="btn-action-small btn-danger" onclick="AdminManager.deleteTicket(${ticket.id})">
                            <i class="fas fa-trash"></i> Delete
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');
    },

    // Get status badge
    getStatusBadge(status) {
        const statusMap = {
            'OPEN': 'badge-info',
            'IN_PROGRESS': 'badge-primary',
            'PENDING_USER': 'badge-warning',
            'RESOLVED': 'badge-success',
            'CLOSED': 'badge-secondary'
        };
        const badgeClass = statusMap[status] || 'badge-secondary';
        const displayName = status.replace('_', ' ');
        return `<span class="badge ${badgeClass}">${displayName}</span>`;
    },

    // Get priority badge
    getPriorityBadge(priority) {
        const priorityMap = {
            'LOW': 'badge-success',
            'MEDIUM': 'badge-warning',
            'HIGH': 'badge-danger',
            'CRITICAL': 'badge-danger'
        };
        const badgeClass = priorityMap[priority] || 'badge-secondary';
        return `<span class="badge ${badgeClass}">${priority}</span>`;
    },

    // Load analytics
    async loadAnalytics() {
        try {
            const [userStats, ticketStatusStats, ticketPriorityStats, activityData] = await Promise.all([
                apiService.admin.getUserStatistics(),
                apiService.admin.getTicketsByStatus(),
                apiService.admin.getTicketsByPriority(),
                apiService.admin.getRecentActivity()
            ]);

            if (userStats.success) {
                this.renderUsersChart(userStats.data);
            }

            if (ticketStatusStats.success) {
                this.renderTicketStatusChart(ticketStatusStats.data);
            }

            if (ticketPriorityStats.success) {
                this.renderTicketPriorityChart(ticketPriorityStats.data);
            }

            if (activityData.success) {
                this.renderRecentActivity(activityData.data);
            }
        } catch (error) {
            console.error('AdminManager: Error loading analytics:', error);
        }
    },

    // Render users chart
    renderUsersChart(data) {
        const canvas = document.getElementById('usersChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');

        if (this.charts.users) {
            this.charts.users.destroy();
        }

        this.charts.users = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Active Users', 'Inactive Users', 'Locked Users'],
                datasets: [{
                    data: [data.activeUsers || 0, data.inactiveUsers || 0, data.lockedUsers || 0],
                    backgroundColor: ['#28a745', '#ffc107', '#dc3545'],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    },

    // Render ticket status chart
    renderTicketStatusChart(data) {
        const canvas = document.getElementById('ticketStatusChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');

        if (this.charts.ticketStatus) {
            this.charts.ticketStatus.destroy();
        }

        const labels = Object.keys(data).map(k => k.replace('_', ' '));
        const values = Object.values(data);

        this.charts.ticketStatus = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: ['#17a2b8', '#007bff', '#ffc107', '#28a745', '#6c757d'],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    },

    // Render ticket priority chart
    renderTicketPriorityChart(data) {
        const canvas = document.getElementById('ticketPriorityChart');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');

        if (this.charts.ticketPriority) {
            this.charts.ticketPriority.destroy();
        }

        const labels = Object.keys(data);
        const values = Object.values(data);

        this.charts.ticketPriority = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Tickets',
                    data: values,
                    backgroundColor: ['#28a745', '#ffc107', '#dc3545', '#dc3545'],
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
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    },

    // Render recent activity
    renderRecentActivity(data) {
        const container = document.getElementById('recent-activity-list');
        if (!container) return;

        const activities = [];

        // Add recent users
        if (data.recentUsers) {
            data.recentUsers.slice(0, 5).forEach(user => {
                activities.push({
                    icon: 'fa-user-plus',
                    title: `New user: ${user.username}`,
                    time: this.formatRelativeTime(user.createdDate)
                });
            });
        }

        // Add recent tickets
        if (data.recentTickets) {
            data.recentTickets.slice(0, 5).forEach(ticket => {
                activities.push({
                    icon: 'fa-ticket-alt',
                    title: `Ticket created: ${ticket.ticketNumber}`,
                    time: this.formatRelativeTime(ticket.createdDate)
                });
            });
        }

        // Sort by most recent
        container.innerHTML = activities.map(activity => `
            <div class="activity-item">
                <div class="activity-icon">
                    <i class="fas ${activity.icon}"></i>
                </div>
                <div class="activity-details">
                    <h4>${activity.title}</h4>
                    <p>${activity.time}</p>
                </div>
            </div>
        `).join('');
    },

    // Format date
    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
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

    // Switch tab
    async switchTab(tab) {
        this.currentTab = tab;

        // Update tab buttons
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.remove('active');
            if (btn.getAttribute('data-tab') === tab) {
                btn.classList.add('active');
            }
        });

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tab}-tab`).classList.add('active');

        // Load tab data
        if (tab === 'users') {
            await this.loadUsers();
        } else if (tab === 'tickets') {
            await this.loadTickets();
        } else if (tab === 'analytics') {
            await this.loadAnalytics();
        }
    },

    // Render users pagination
    renderUsersPagination() {
        const container = document.getElementById('users-pagination');
        const { page, total } = this.pagination.users;

        container.innerHTML = `
            <button onclick="AdminManager.changeUsersPage(${page - 1})" ${page === 0 ? 'disabled' : ''}>
                <i class="fas fa-chevron-left"></i> Previous
            </button>
            <span class="page-info">Page ${page + 1} of ${total || 1}</span>
            <button onclick="AdminManager.changeUsersPage(${page + 1})" ${page >= total - 1 ? 'disabled' : ''}>
                Next <i class="fas fa-chevron-right"></i>
            </button>
        `;
    },

    // Render tickets pagination
    renderTicketsPagination() {
        const container = document.getElementById('tickets-pagination');
        const { page, total } = this.pagination.tickets;

        container.innerHTML = `
            <button onclick="AdminManager.changeTicketsPage(${page - 1})" ${page === 0 ? 'disabled' : ''}>
                <i class="fas fa-chevron-left"></i> Previous
            </button>
            <span class="page-info">Page ${page + 1} of ${total || 1}</span>
            <button onclick="AdminManager.changeTicketsPage(${page + 1})" ${page >= total - 1 ? 'disabled' : ''}>
                Next <i class="fas fa-chevron-right"></i>
            </button>
        `;
    },

    // Change users page
    async changeUsersPage(newPage) {
        if (newPage < 0 || newPage >= this.pagination.users.total) return;
        this.pagination.users.page = newPage;
        await this.loadUsers();
    },

    // Change tickets page
    async changeTicketsPage(newPage) {
        if (newPage < 0 || newPage >= this.pagination.tickets.total) return;
        this.pagination.tickets.page = newPage;
        await this.loadTickets();
    },

    // User actions
    async viewUserDetails(userId) {
        try {
            const response = await apiService.admin.getUserStats(userId);
            
            if (response.success && response.data) {
                this.showUserModal(response.data);
            }
        } catch (error) {
            console.error('AdminManager: Error fetching user details:', error);
            showToast('Failed to load user details', 'error');
        }
    },

    showUserModal(userStats) {
        const modal = document.getElementById('user-detail-modal');
        const content = document.getElementById('user-detail-content');

        content.innerHTML = `
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                <div>
                    <h3>User Information</h3>
                    <p><strong>ID:</strong> ${userStats.userId}</p>
                    <p><strong>Username:</strong> ${userStats.username}</p>
                    <p><strong>Email:</strong> ${userStats.email}</p>
                    <p><strong>Full Name:</strong> ${userStats.fullName}</p>
                    <p><strong>Roles:</strong> ${userStats.roles}</p>
                </div>
                <div>
                    <h3>Account Status</h3>
                    <p><strong>Active:</strong> ${userStats.active ? '✅ Yes' : '❌ No'}</p>
                    <p><strong>Locked:</strong> ${userStats.accountLocked ? '🔒 Yes' : '🔓 No'}</p>
                    <p><strong>Failed Attempts:</strong> ${userStats.failedLoginAttempts}</p>
                    <p><strong>Last Login:</strong> ${userStats.lastLogin}</p>
                    <p><strong>Created:</strong> ${userStats.createdDate}</p>
                </div>
            </div>
            <div style="margin-top: 20px;">
                <h3>Ticket Statistics</h3>
                <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 10px;">
                    <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; text-align: center;">
                        <h4 style="font-size: 24px; color: #0b0081;">${userStats.totalTickets}</h4>
                        <p style="color: #666; font-size: 12px;">Total</p>
                    </div>
                    <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; text-align: center;">
                        <h4 style="font-size: 24px; color: #17a2b8;">${userStats.openTickets}</h4>
                        <p style="color: #666; font-size: 12px;">Open</p>
                    </div>
                    <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; text-align: center;">
                        <h4 style="font-size: 24px; color: #28a745;">${userStats.resolvedTickets}</h4>
                        <p style="color: #666; font-size: 12px;">Resolved</p>
                    </div>
                    <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; text-align: center;">
                        <h4 style="font-size: 24px; color: #ffc107;">${userStats.pendingTickets}</h4>
                        <p style="color: #666; font-size: 12px;">Pending</p>
                    </div>
                </div>
            </div>
        `;

        modal.classList.add('active');
    },

    async activateUser(userId) {
        if (!confirm('Are you sure you want to activate this user?')) return;
        
        try {
            const response = await apiService.admin.activateUser(userId);
            if (response.success) {
                showToast('User activated successfully', 'success');
                await this.loadUsers();
            }
        } catch (error) {
            showToast('Failed to activate user', 'error');
        }
    },

    async deactivateUser(userId) {
        if (!confirm('Are you sure you want to deactivate this user?')) return;
        
        try {
            const response = await apiService.admin.deactivateUser(userId);
            if (response.success) {
                showToast('User deactivated successfully', 'success');
                await this.loadUsers();
            }
        } catch (error) {
            showToast('Failed to deactivate user', 'error');
        }
    },

    async lockUser(userId) {
        if (!confirm('Are you sure you want to lock this user account?')) return;
        
        try {
            const response = await apiService.admin.lockUser(userId);
            if (response.success) {
                showToast('User locked successfully', 'success');
                await this.loadUsers();
            }
        } catch (error) {
            showToast('Failed to lock user', 'error');
        }
    },

    async unlockUser(userId) {
        if (!confirm('Are you sure you want to unlock this user account?')) return;
        
        try {
            const response = await apiService.admin.unlockUser(userId);
            if (response.success) {
                showToast('User unlocked successfully', 'success');
                await this.loadUsers();
            }
        } catch (error) {
            showToast('Failed to unlock user', 'error');
        }
    },

    async deleteUser(userId) {
        if (!confirm('Are you sure you want to delete this user? This action cannot be undone!')) return;
        
        try {
            const response = await apiService.admin.deleteUser(userId);
            if (response.success) {
                showToast('User deleted successfully', 'success');
                await this.loadUsers();
                await this.loadDashboardStats();
            }
        } catch (error) {
            showToast('Failed to delete user', 'error');
        }
    },

    // Ticket actions
    async viewTicketDetails(ticketId) {
        try {
            const response = await apiService.admin.getTicketById(ticketId);
            
            if (response.success && response.data) {
                this.showTicketModal(response.data);
            }
        } catch (error) {
            showToast('Failed to load ticket details', 'error');
        }
    },

    showTicketModal(ticket) {
        const modal = document.getElementById('ticket-detail-modal');
        const content = document.getElementById('ticket-detail-content');

        content.innerHTML = `
            <div>
                <h3>Ticket #${ticket.ticketNumber}</h3>
                <p><strong>Subject:</strong> ${ticket.subject}</p>
                <p><strong>Status:</strong> ${this.getStatusBadge(ticket.status)}</p>
                <p><strong>Priority:</strong> ${this.getPriorityBadge(ticket.priority)}</p>
                <p><strong>Created:</strong> ${this.formatDate(ticket.createdDate)}</p>
                <p><strong>Assigned To:</strong> ${ticket.assignedTo || 'Unassigned'}</p>
                
                <h4 style="margin-top: 20px;">Message</h4>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 10px; white-space: pre-wrap;">
                    ${ticket.message}
                </div>

                ${ticket.resolutionNotes ? `
                    <h4 style="margin-top: 20px;">Resolution Notes</h4>
                    <div style="background: #d4edda; padding: 15px; border-radius: 10px; white-space: pre-wrap;">
                        ${ticket.resolutionNotes}
                    </div>
                ` : ''}

                <div style="margin-top: 20px; display: flex; gap: 10px;">
                    ${ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED' ? `
                        <button class="btn-action btn-success" onclick="AdminManager.resolveTicketWithModal(${ticket.id})">
                            <i class="fas fa-check"></i> Resolve Ticket
                        </button>
                    ` : ''}
                    <button class="btn-action" onclick="AdminManager.closeTicketModal()">
                        Close
                    </button>
                </div>
            </div>
        `;

        modal.classList.add('active');
    },

    async resolveTicket(ticketId) {
        const notes = prompt('Enter resolution notes:');
        if (!notes) return;

        try {
            const response = await apiService.admin.resolveTicket(ticketId, notes);
            if (response.success) {
                showToast('Ticket resolved successfully', 'success');
                await this.loadTickets();
                await this.loadDashboardStats();
            }
        } catch (error) {
            showToast('Failed to resolve ticket', 'error');
        }
    },

    async resolveTicketWithModal(ticketId) {
        this.closeTicketModal();
        await this.resolveTicket(ticketId);
    },

    async deleteTicket(ticketId) {
        if (!confirm('Are you sure you want to delete this ticket? This action cannot be undone!')) return;
        
        try {
            const response = await apiService.admin.deleteTicket(ticketId);
            if (response.success) {
                showToast('Ticket deleted successfully', 'success');
                await this.loadTickets();
                await this.loadDashboardStats();
            }
        } catch (error) {
            showToast('Failed to delete ticket', 'error');
        }
    },

    closeUserModal() {
        document.getElementById('user-detail-modal').classList.remove('active');
    },

    closeTicketModal() {
        document.getElementById('ticket-detail-modal').classList.remove('active');
    }
};

// ============================================================================
// GLOBAL FUNCTIONS
// ============================================================================

function switchTab(tab) {
    AdminManager.switchTab(tab);
}

function searchUsers() {
    const query = document.getElementById('user-search').value;
    // Implement search functionality
    console.log('Searching users:', query);
}

function searchTickets() {
    const query = document.getElementById('ticket-search').value;
    // Implement search functionality
    console.log('Searching tickets:', query);
}

function filterTickets() {
    const status = document.getElementById('ticket-status-filter').value;
    const priority = document.getElementById('ticket-priority-filter').value;
    AdminManager.loadTickets(status || null, priority || null);
}

function sortUsersTable(field) {
    if (AdminManager.sorting.users.field === field) {
        AdminManager.sorting.users.direction = AdminManager.sorting.users.direction === 'ASC' ? 'DESC' : 'ASC';
    } else {
        AdminManager.sorting.users.field = field;
        AdminManager.sorting.users.direction = 'ASC';
    }
    AdminManager.loadUsers();
}

function sortTicketsTable(field) {
    if (AdminManager.sorting.tickets.field === field) {
        AdminManager.sorting.tickets.direction = AdminManager.sorting.tickets.direction === 'ASC' ? 'DESC' : 'ASC';
    } else {
        AdminManager.sorting.tickets.field = field;
        AdminManager.sorting.tickets.direction = 'DESC';
    }
    AdminManager.loadTickets();
}

function exportUsers() {
    showToast('Export feature coming soon', 'info');
}

function refreshAllData() {
    location.reload();
}

function closeUserModal() {
    AdminManager.closeUserModal();
}

function closeTicketModal() {
    AdminManager.closeTicketModal();
}

// ============================================================================
// INITIALIZATION
// ============================================================================

document.addEventListener('DOMContentLoaded', () => {
    console.log('Admin Panel: DOM loaded, initializing...');
    AdminManager.init();
});

// Export for use in other modules
window.AdminManager = AdminManager;

console.log('admin-panel.js loaded successfully');
