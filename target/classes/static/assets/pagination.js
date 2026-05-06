/**
 * Pagination and Search Component
 * Handles pagination, search, filtering, and sorting for data tables
 */

class PaginationManager {
    /**
     * Create a new PaginationManager instance
     * @param {Object} options - Configuration options
     * @param {string} options.apiEndpoint - Base API endpoint for fetching data
     * @param {string} options.containerId - ID of the container for results
     * @param {string} options.paginationId - ID of the pagination controls container
     * @param {Function} options.renderFunction - Function to render each item
     * @param {Object} options.defaultFilters - Default filter values
     * @param {number} options.defaultPageSize - Default page size (default: 10)
     */
    constructor(options) {
        this.apiEndpoint = options.apiEndpoint;
        this.containerId = options.containerId;
        this.paginationId = options.paginationId;
        this.renderFunction = options.renderFunction;
        this.defaultFilters = options.defaultFilters || {};
        this.defaultPageSize = options.defaultPageSize || 10;

        // Current state
        this.currentPage = 0;
        this.pageSize = this.defaultPageSize;
        this.sortBy = 'createdDate';
        this.sortDir = 'desc';
        this.filters = { ...this.defaultFilters };
        this.searchTerm = '';
        this.searchTimeout = null;

        // Pagination data
        this.totalPages = 0;
        this.totalElements = 0;
        this.isLoading = false;
    }

    /**
     * Initialize the pagination manager
     */
    async init() {
        this.setupEventListeners();
        await this.loadData();
    }

    /**
     * Setup event listeners for search and filter controls
     */
    setupEventListeners() {
        // Search input with debounce
        const searchInput = document.getElementById('search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                clearTimeout(this.searchTimeout);
                this.searchTimeout = setTimeout(() => {
                    this.searchTerm = e.target.value;
                    this.currentPage = 0; // Reset to first page
                    this.loadData();
                }, 500); // 500ms debounce
            });
        }

        // Clear search button
        const clearSearchBtn = document.getElementById('clear-search');
        if (clearSearchBtn) {
            clearSearchBtn.addEventListener('click', () => {
                const searchInput = document.getElementById('search-input');
                if (searchInput) {
                    searchInput.value = '';
                    this.searchTerm = '';
                    this.currentPage = 0;
                    this.loadData();
                }
            });
        }

        // Status filter
        const statusFilter = document.getElementById('status-filter');
        if (statusFilter) {
            statusFilter.addEventListener('change', (e) => {
                this.filters.status = e.target.value || null;
                this.currentPage = 0;
                this.loadData();
                this.updateFilterTags();
            });
        }

        // Priority filter
        const priorityFilter = document.getElementById('priority-filter');
        if (priorityFilter) {
            priorityFilter.addEventListener('change', (e) => {
                this.filters.priority = e.target.value || null;
                this.currentPage = 0;
                this.loadData();
                this.updateFilterTags();
            });
        }

        // Sort field
        const sortField = document.getElementById('sort-field');
        if (sortField) {
            sortField.addEventListener('change', (e) => {
                this.sortBy = e.target.value;
                this.loadData();
            });
        }

        // Sort direction button
        const sortDirectionBtn = document.getElementById('sort-direction');
        if (sortDirectionBtn) {
            sortDirectionBtn.addEventListener('click', () => {
                this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
                const icon = sortDirectionBtn.querySelector('i');
                if (icon) {
                    icon.className = this.sortDir === 'asc' ? 'fas fa-sort-up' : 'fas fa-sort-down';
                }
                this.loadData();
            });
        }

        // Page size selector
        const pageSizeSelect = document.getElementById('page-size');
        if (pageSizeSelect) {
            pageSizeSelect.addEventListener('change', (e) => {
                this.pageSize = parseInt(e.target.value);
                this.currentPage = 0; // Reset to first page
                this.loadData();
            });
        }
    }

    /**
     * Load data from API
     */
    async loadData() {
        if (this.isLoading) return;

        this.isLoading = true;
        this.showLoading();

        try {
            // Build query parameters
            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                sortBy: this.sortBy,
                sortDir: this.sortDir
            });

            // Add filters
            Object.keys(this.filters).forEach(key => {
                if (this.filters[key] !== null && this.filters[key] !== undefined && this.filters[key] !== '') {
                    params.append(key, this.filters[key]);
                }
            });

            // Add search term
            if (this.searchTerm && this.searchTerm.trim() !== '') {
                params.append('search', this.searchTerm.trim());
            }

            // Make API request
            const response = await fetch(`${this.apiEndpoint}?${params.toString()}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${TokenManager.getAccessToken()}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to load data');
            }

            const result = await response.json();

            if (result.success && result.data) {
                this.renderResults(result.data);
                this.renderPagination(result.data);
            } else {
                this.showError('Failed to load data');
            }

        } catch (error) {
            console.error('Error loading data:', error);
            this.showError('Error loading data. Please try again.');
        } finally {
            this.isLoading = false;
            this.hideLoading();
        }
    }

    /**
     * Render the results
     */
    renderResults(data) {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        // Update pagination metadata
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;

        if (data.empty || !data.content || data.content.length === 0) {
            container.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-search"></i>
                    <h3>No Results Found</h3>
                    <p>Try adjusting your search or filter criteria</p>
                </div>
            `;
            return;
        }

        // Render items using provided render function
        container.innerHTML = data.content.map(item => this.renderFunction(item)).join('');
    }

    /**
     * Render pagination controls
     */
    renderPagination(data) {
        const container = document.getElementById(this.paginationId);
        if (!container) return;

        const startItem = data.empty ? 0 : (data.page * data.size) + 1;
        const endItem = data.empty ? 0 : Math.min((data.page + 1) * data.size, data.totalElements);

        let html = `
            <div class="pagination-info">
                Showing <strong>${startItem}</strong> to <strong>${endItem}</strong> of <strong>${data.totalElements}</strong> results
            </div>
            <div class="pagination-controls">
        `;

        // Previous button
        html += `
            <button onclick="paginationManager.goToPage(${data.page - 1})" 
                    ${!data.hasPrevious ? 'disabled' : ''}>
                <i class="fas fa-chevron-left"></i> Previous
            </button>
        `;

        // Page numbers
        const pageNumbers = this.getPageNumbers(data.page, data.totalPages);
        pageNumbers.forEach(pageNum => {
            if (pageNum === '...') {
                html += `<span class="page-ellipsis">...</span>`;
            } else {
                html += `
                    <button class="page-number ${pageNum === data.page ? 'active' : ''}" 
                            onclick="paginationManager.goToPage(${pageNum})">
                        ${pageNum + 1}
                    </button>
                `;
            }
        });

        // Next button
        html += `
            <button onclick="paginationManager.goToPage(${data.page + 1})" 
                    ${!data.hasNext ? 'disabled' : ''}>
                Next <i class="fas fa-chevron-right"></i>
            </button>
        `;

        html += `</div>`;

        container.innerHTML = html;
    }

    /**
     * Calculate page numbers to display
     */
    getPageNumbers(currentPage, totalPages) {
        const delta = 2; // Number of pages to show on each side of current page
        const range = [];
        const rangeWithDots = [];

        for (let i = 0; i < totalPages; i++) {
            if (i === 0 || i === totalPages - 1 || 
                (i >= currentPage - delta && i <= currentPage + delta)) {
                range.push(i);
            }
        }

        let prev = -2;
        for (const i of range) {
            if (i - prev === 2) {
                rangeWithDots.push(prev + 1);
            } else if (i - prev !== 1) {
                rangeWithDots.push('...');
            }
            rangeWithDots.push(i);
            prev = i;
        }

        return rangeWithDots;
    }

    /**
     * Go to specific page
     */
    goToPage(page) {
        if (page < 0 || page >= this.totalPages) return;
        this.currentPage = page;
        this.loadData();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    /**
     * Update filter tags display
     */
    updateFilterTags() {
        const tagsContainer = document.getElementById('filter-tags');
        if (!tagsContainer) return;

        const tags = [];

        // Add filter tags
        Object.keys(this.filters).forEach(key => {
            if (this.filters[key] !== null && this.filters[key] !== undefined && this.filters[key] !== '') {
                tags.push({
                    key: key,
                    label: this.formatFilterLabel(key),
                    value: this.formatFilterValue(this.filters[key])
                });
            }
        });

        // Add search tag
        if (this.searchTerm && this.searchTerm.trim() !== '') {
            tags.push({
                key: 'search',
                label: 'Search',
                value: this.searchTerm
            });
        }

        if (tags.length === 0) {
            tagsContainer.innerHTML = '';
            tagsContainer.style.display = 'none';
            return;
        }

        tagsContainer.style.display = 'flex';
        tagsContainer.innerHTML = tags.map(tag => `
            <div class="filter-tag">
                <span class="tag-label">${tag.label}:</span>
                <span class="tag-value">${tag.value}</span>
                <button onclick="paginationManager.removeFilter('${tag.key}')">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `).join('') + `
            <button class="clear-filters-btn" onclick="paginationManager.clearAllFilters()">
                Clear All
            </button>
        `;
    }

    /**
     * Remove a specific filter
     */
    removeFilter(key) {
        if (key === 'search') {
            this.searchTerm = '';
            const searchInput = document.getElementById('search-input');
            if (searchInput) searchInput.value = '';
        } else {
            this.filters[key] = null;
            const filterElement = document.getElementById(`${key}-filter`);
            if (filterElement) filterElement.value = '';
        }
        this.currentPage = 0;
        this.loadData();
        this.updateFilterTags();
    }

    /**
     * Clear all filters
     */
    clearAllFilters() {
        this.searchTerm = '';
        this.filters = {};
        this.currentPage = 0;

        // Clear UI elements
        const searchInput = document.getElementById('search-input');
        if (searchInput) searchInput.value = '';

        const statusFilter = document.getElementById('status-filter');
        if (statusFilter) statusFilter.value = '';

        const priorityFilter = document.getElementById('priority-filter');
        if (priorityFilter) priorityFilter.value = '';

        this.loadData();
        this.updateFilterTags();
    }

    /**
     * Format filter label for display
     */
    formatFilterLabel(key) {
        return key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1');
    }

    /**
     * Format filter value for display
     */
    formatFilterValue(value) {
        if (typeof value === 'string') {
            return value.split('_').map(word => 
                word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
            ).join(' ');
        }
        return value;
    }

    /**
     * Show loading indicator
     */
    showLoading() {
        const container = document.getElementById(this.containerId);
        if (container) {
            container.innerHTML = `
                <div class="search-loading">
                    <div class="spinner"></div>
                </div>
            `;
        }
    }

    /**
     * Hide loading indicator
     */
    hideLoading() {
        // Loading is replaced by content, so no action needed
    }

    /**
     * Show error message
     */
    showError(message) {
        const container = document.getElementById(this.containerId);
        if (container) {
            container.innerHTML = `
                <div class="no-results">
                    <i class="fas fa-exclamation-circle"></i>
                    <h3>Error</h3>
                    <p>${message}</p>
                </div>
            `;
        }
    }
}

/**
 * Load filter options from API
 */
async function loadFilterOptions() {
    try {
        const response = await fetch('/api/tickets/filters', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${TokenManager.getAccessToken()}`
            }
        });

        if (response.ok) {
            const result = await response.json();
            if (result.success && result.filters) {
                populateFilterDropdowns(result.filters);
            }
        }
    } catch (error) {
        console.error('Error loading filter options:', error);
    }
}

/**
 * Populate filter dropdowns with options
 */
function populateFilterDropdowns(filters) {
    // Status filter
    const statusFilter = document.getElementById('status-filter');
    if (statusFilter && filters.statuses) {
        statusFilter.innerHTML = '<option value="">All Statuses</option>' +
            filters.statuses.map(status => 
                `<option value="${status.value}">${status.label}</option>`
            ).join('');
    }

    // Priority filter
    const priorityFilter = document.getElementById('priority-filter');
    if (priorityFilter && filters.priorities) {
        priorityFilter.innerHTML = '<option value="">All Priorities</option>' +
            filters.priorities.map(priority => 
                `<option value="${priority.value}">${priority.label}</option>`
            ).join('');
    }

    // Sort field
    const sortField = document.getElementById('sort-field');
    if (sortField && filters.sortableFields) {
        sortField.innerHTML = filters.sortableFields.map(field => 
            `<option value="${field.value}">${field.label}</option>`
        ).join('');
    }
}
