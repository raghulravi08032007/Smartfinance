# Search and Pagination System Documentation

## Overview

This document provides comprehensive documentation for the search, pagination, filtering, and sorting system implemented in the Finance Tracker application. The system provides a powerful and flexible way to search and navigate through large datasets with dynamic filters.

## Table of Contents

1. [Architecture](#architecture)
2. [Backend Components](#backend-components)
3. [Frontend Components](#frontend-components)
4. [API Endpoints](#api-endpoints)
5. [Usage Examples](#usage-examples)
6. [Configuration](#configuration)
7. [Customization](#customization)
8. [Best Practices](#best-practices)

---

## Architecture

The search and pagination system follows a layered architecture:

```
Frontend (pagination.js)
    ↓
REST API (TicketSearchController)
    ↓
Specification Builder (Dynamic Query)
    ↓
JPA Repository (JpaSpecificationExecutor)
    ↓
Database
```

### Key Features

- **Dynamic Filtering**: Build complex queries using multiple criteria
- **Pagination**: Navigate through large datasets efficiently
- **Sorting**: Sort by any field in ascending or descending order
- **Search**: Full-text search across multiple fields
- **Debouncing**: Prevent excessive API calls during typing
- **Responsive Design**: Works on all device sizes

---

## Backend Components

### 1. SearchCriteria Class

Located: `src/main/java/com/rawgul/dto/SearchCriteria.java`

Represents a single search condition with key, operation, and value.

**Supported Operations:**

| Operation | Symbol | Description | Example |
|-----------|--------|-------------|---------|
| EQUALS | `:` | Exact match | `status : "OPEN"` |
| NOT_EQUAL | `!:` | Not equal | `priority !: "LOW"` |
| GREATER_THAN | `>` | Greater than | `createdDate > "2024-01-01"` |
| LESS_THAN | `<` | Less than | `createdDate < "2024-12-31"` |
| GREATER_THAN_OR_EQUAL | `>=` | Greater than or equal | `priority >= "MEDIUM"` |
| LESS_THAN_OR_EQUAL | `<=` | Less than or equal | `priority <= "HIGH"` |
| LIKE | `~` | Contains (case-insensitive) | `subject ~ "payment"` |
| STARTS_WITH | `^` | Starts with | `ticketNumber ^ "TKT"` |
| ENDS_WITH | `$` | Ends with | `email $ "@gmail.com"` |
| IN | `in` | Value in list | `status in ["OPEN", "IN_PROGRESS"]` |
| NOT_IN | `!in` | Value not in list | `priority !in ["LOW"]` |

**Constructor:**
```java
// Simple criteria
SearchCriteria criteria = new SearchCriteria("status", ":", "OPEN");

// With join table
SearchCriteria criteria = new SearchCriteria("id", ":", userId, "user");
```

### 2. GenericSpecification Class

Located: `src/main/java/com/rawgul/specification/GenericSpecification.java`

Implements Spring Data JPA `Specification` interface for type-safe query building.

**Features:**
- Handles different data types (String, Number, Date, LocalDate, LocalDateTime)
- Supports join operations for related entities
- Case-insensitive string matching
- Error handling for invalid predicates

**Example Usage:**
```java
SearchCriteria criteria = new SearchCriteria("status", ":", TicketStatus.OPEN);
Specification<SupportTicket> spec = new GenericSpecification<>(criteria);
List<SupportTicket> tickets = repository.findAll(spec);
```

### 3. SpecificationBuilder Class

Located: `src/main/java/com/rawgul/specification/SpecificationBuilder.java`

Builder pattern for combining multiple search criteria with AND/OR operations.

**Methods:**

```java
// Add criteria with AND operation (default)
builder.with("status", ":", "OPEN");
builder.with("priority", ":", "HIGH");

// Add criteria with join
builder.with("id", ":", userId, "user");

// Switch to OR operation
builder.withOr();

// Build final specification
Specification<T> spec = builder.build();

// Clear all criteria
builder.clear();

// Get criteria count
int count = builder.size();
```

**Example:**
```java
// Find tickets that are OPEN and have HIGH priority
SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();
builder.with("status", ":", TicketStatus.OPEN);
builder.with("priority", ":", TicketPriority.HIGH);
Specification<SupportTicket> spec = builder.build();

// Find tickets where subject OR description contains "payment"
SpecificationBuilder<SupportTicket> searchBuilder = new SpecificationBuilder<>();
searchBuilder.with("subject", "~", "payment");
searchBuilder.with("description", "~", "payment");
searchBuilder.withOr();
Specification<SupportTicket> searchSpec = searchBuilder.build();
```

### 4. PageableResponse Class

Located: `src/main/java/com/rawgul/dto/PageableResponse.java`

Generic wrapper for paginated API responses.

**Properties:**
- `content`: List of items for current page
- `page`: Current page number (0-indexed)
- `size`: Items per page
- `totalElements`: Total items across all pages
- `totalPages`: Total number of pages
- `first`: Whether this is the first page
- `last`: Whether this is the last page
- `hasNext`: Whether there's a next page
- `hasPrevious`: Whether there's a previous page
- `numberOfElements`: Items in current page
- `empty`: Whether the page is empty

**Usage:**
```java
Page<SupportTicket> page = repository.findAll(spec, pageable);
PageableResponse<SupportTicket> response = PageableResponse.fromPage(page);
```

### 5. Repository Updates

Updated repositories to extend `JpaSpecificationExecutor` for dynamic queries:

```java
@Repository
public interface SupportTicketRepository extends 
    JpaRepository<SupportTicket, Long>, 
    JpaSpecificationExecutor<SupportTicket> {
    
    // Pageable methods
    Page<SupportTicket> findByUser(User user, Pageable pageable);
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
    Page<SupportTicket> findByPriority(TicketPriority priority, Pageable pageable);
}
```

### 6. TicketSearchController

Located: `src/main/java/com/rawgul/controller/TicketSearchController.java`

REST API controller for search and pagination operations.

**Endpoints:**

#### GET `/api/tickets/search`
Search tickets with dynamic filters and pagination.

**Query Parameters:**
- `page` (int, default: 0): Page number (0-indexed)
- `size` (int, default: 10): Items per page (max: 100)
- `sortBy` (string, default: "createdDate"): Field to sort by
- `sortDir` (string, default: "desc"): Sort direction (asc/desc)
- `status` (string, optional): Filter by status
- `priority` (string, optional): Filter by priority
- `assignedTo` (string, optional): Filter by assigned user
- `search` (string, optional): Search in subject and description
- `userId` (long, optional): Filter by user ID

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 45,
    "totalPages": 5,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false,
    "numberOfElements": 10,
    "empty": false
  }
}
```

#### GET `/api/tickets/user/{userId}`
Get paginated tickets for a specific user.

**Path Parameters:**
- `userId`: User ID

**Query Parameters:** Same as search endpoint

#### GET `/api/tickets/status/{status}`
Get paginated tickets by status.

**Path Parameters:**
- `status`: Ticket status (OPEN, IN_PROGRESS, RESOLVED, CLOSED)

**Query Parameters:** page, size, sortBy, sortDir

#### GET `/api/tickets/priority/{priority}`
Get paginated tickets by priority.

**Path Parameters:**
- `priority`: Ticket priority (LOW, MEDIUM, HIGH, CRITICAL)

**Query Parameters:** page, size, sortBy, sortDir

#### GET `/api/tickets/filters`
Get available filter options.

**Response:**
```json
{
  "success": true,
  "filters": {
    "statuses": [
      {"value": "OPEN", "label": "Open"},
      {"value": "IN_PROGRESS", "label": "In Progress"},
      ...
    ],
    "priorities": [
      {"value": "LOW", "label": "Low"},
      {"value": "MEDIUM", "label": "Medium"},
      ...
    ],
    "sortableFields": [
      {"value": "createdDate", "label": "Created Date"},
      {"value": "updatedDate", "label": "Updated Date"},
      ...
    ]
  }
}
```

---

## Frontend Components

### 1. Pagination CSS

Located: `assets/pagination.css`

Provides styling for:
- Pagination controls with page numbers
- Search bar with icon and clear button
- Filter dropdowns with custom styling
- Sort controls with direction toggle
- Page size selector
- Active filter tags with remove buttons
- Loading indicators
- No results state
- Responsive design for mobile devices

**Key Classes:**
- `.pagination-container`: Main pagination wrapper
- `.pagination-controls`: Page number buttons
- `.search-box`: Search input container
- `.filter-dropdown`: Filter select dropdowns
- `.sort-dropdown`: Sort field and direction
- `.filter-tags`: Active filter display
- `.no-results`: Empty state display

### 2. PaginationManager Class

Located: `assets/pagination.js`

JavaScript class for managing pagination, search, and filters.

**Constructor Options:**
```javascript
const paginationManager = new PaginationManager({
    apiEndpoint: '/api/tickets/search',     // API endpoint
    containerId: 'tickets-list',             // Results container ID
    paginationId: 'pagination-controls',     // Pagination container ID
    renderFunction: renderTicketCard,        // Item render function
    defaultFilters: {},                      // Default filter values
    defaultPageSize: 10                      // Default page size
});
```

**Methods:**

```javascript
// Initialize (sets up listeners and loads data)
await paginationManager.init();

// Load data from API
await paginationManager.loadData();

// Navigate to specific page
paginationManager.goToPage(2);

// Remove specific filter
paginationManager.removeFilter('status');

// Clear all filters
paginationManager.clearAllFilters();

// Update filter tag display
paginationManager.updateFilterTags();
```

**Features:**
- **Debounced Search**: 500ms delay to prevent excessive API calls
- **Dynamic Filters**: Automatically builds query parameters
- **State Management**: Tracks current page, filters, sort options
- **Smart Pagination**: Shows ellipsis for large page ranges
- **Filter Tags**: Visual display of active filters with remove buttons
- **Loading States**: Shows loading indicator during API calls
- **Error Handling**: Displays user-friendly error messages

### 3. Helper Functions

```javascript
// Load filter options from API
await loadFilterOptions();

// Populate dropdown menus
populateFilterDropdowns(filters);
```

---

## Usage Examples

### Backend: Building Complex Queries

**Example 1: Simple Filter**
```java
SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();
builder.with("status", ":", TicketStatus.OPEN);
Specification<SupportTicket> spec = builder.build();

Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
Page<SupportTicket> result = ticketRepository.findAll(spec, pageable);
```

**Example 2: Multiple Filters with AND**
```java
SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();
builder.with("status", ":", TicketStatus.OPEN);
builder.with("priority", ":", TicketPriority.HIGH);
builder.with("id", ":", userId, "user");
Specification<SupportTicket> spec = builder.build();
```

**Example 3: Search with OR**
```java
SpecificationBuilder<SupportTicket> searchBuilder = new SpecificationBuilder<>();
searchBuilder.with("subject", "~", "payment");
searchBuilder.with("description", "~", "payment");
searchBuilder.withOr();
Specification<SupportTicket> spec = searchBuilder.build();
```

**Example 4: Combining AND and OR**
```java
// Search: (subject LIKE 'payment' OR description LIKE 'payment')
SpecificationBuilder<SupportTicket> searchBuilder = new SpecificationBuilder<>();
searchBuilder.with("subject", "~", "payment");
searchBuilder.with("description", "~", "payment");
searchBuilder.withOr();
Specification<SupportTicket> searchSpec = searchBuilder.build();

// Filters: status = OPEN AND priority = HIGH
SpecificationBuilder<SupportTicket> filterBuilder = new SpecificationBuilder<>();
filterBuilder.with("status", ":", TicketStatus.OPEN);
filterBuilder.with("priority", ":", TicketPriority.HIGH);
Specification<SupportTicket> filterSpec = filterBuilder.build();

// Combine: (search) AND (filters)
Specification<SupportTicket> combinedSpec = 
    Specification.where(searchSpec).and(filterSpec);
```

### Frontend: Using PaginationManager

**Example 1: Basic Setup**
```javascript
// Define render function
function renderTicketCard(ticket) {
    return `
        <div class="ticket-card">
            <h3>${ticket.subject}</h3>
            <p>${ticket.description}</p>
        </div>
    `;
}

// Initialize pagination
const paginationManager = new PaginationManager({
    apiEndpoint: '/api/tickets/search',
    containerId: 'tickets-list',
    paginationId: 'pagination-controls',
    renderFunction: renderTicketCard,
    defaultPageSize: 10
});

await paginationManager.init();
```

**Example 2: With Default Filters**
```javascript
const paginationManager = new PaginationManager({
    apiEndpoint: '/api/tickets/search',
    containerId: 'tickets-list',
    paginationId: 'pagination-controls',
    renderFunction: renderTicketCard,
    defaultFilters: {
        status: 'OPEN',
        priority: 'HIGH'
    },
    defaultPageSize: 20
});
```

**Example 3: Custom API Endpoint**
```javascript
// For user-specific tickets
const userTicketsManager = new PaginationManager({
    apiEndpoint: `/api/tickets/user/${userId}`,
    containerId: 'user-tickets-list',
    paginationId: 'user-pagination',
    renderFunction: renderTicketCard
});
```

---

## Configuration

### Backend Configuration

No additional configuration needed. The system uses standard Spring Data JPA settings.

**Optional: Adjust max page size**
```java
// In TicketSearchController.executeSearch()
if (size > 100) {
    size = 100; // Max page size
}
```

### Frontend Configuration

**Debounce Delay:**
```javascript
// In PaginationManager.setupEventListeners()
this.searchTimeout = setTimeout(() => {
    this.searchTerm = e.target.value;
    this.loadData();
}, 500); // Change delay here (milliseconds)
```

**Default Page Size:**
```javascript
const paginationManager = new PaginationManager({
    ...
    defaultPageSize: 20 // Change default page size
});
```

**Page Number Display:**
```javascript
// In PaginationManager.getPageNumbers()
const delta = 2; // Change number of pages shown on each side
```

---

## Customization

### Adding Custom Filters

**Backend: Add filter to controller**
```java
@GetMapping("/search")
public ResponseEntity<?> searchTickets(
    ...existing params...,
    @RequestParam(required = false) String category
) {
    // Add filter
    if (category != null && !category.isEmpty()) {
        builder.with("category", ":", category);
    }
    ...
}
```

**Frontend: Add filter dropdown**
```html
<div class="filter-dropdown">
    <label for="category-filter">Category</label>
    <select id="category-filter">
        <option value="">All Categories</option>
        <option value="billing">Billing</option>
        <option value="technical">Technical</option>
    </select>
</div>
```

```javascript
// Add event listener in setupEventListeners()
const categoryFilter = document.getElementById('category-filter');
if (categoryFilter) {
    categoryFilter.addEventListener('change', (e) => {
        this.filters.category = e.target.value || null;
        this.currentPage = 0;
        this.loadData();
        this.updateFilterTags();
    });
}
```

### Custom Sort Fields

**Backend: Already supports any field**

**Frontend: Add to sort dropdown**
```html
<select id="sort-field">
    <option value="createdDate">Created Date</option>
    <option value="category">Category</option>
    <option value="ticketNumber">Ticket Number</option>
</select>
```

### Custom Render Function

```javascript
function renderCustomCard(item) {
    return `
        <div class="custom-card">
            <div class="card-header">
                <h3>${item.title}</h3>
                <span class="badge">${item.status}</span>
            </div>
            <div class="card-body">
                ${item.content}
            </div>
            <div class="card-footer">
                <button onclick="viewItem(${item.id})">View</button>
            </div>
        </div>
    `;
}
```

---

## Best Practices

### Backend

1. **Use Indexes**: Add database indexes on frequently filtered/sorted fields
   ```java
   @Table(indexes = {
       @Index(name = "idx_status", columnList = "status"),
       @Index(name = "idx_priority", columnList = "priority"),
       @Index(name = "idx_created_date", columnList = "created_date")
   })
   ```

2. **Limit Page Size**: Prevent excessive data transfer
   ```java
   if (size > 100) size = 100;
   ```

3. **Validate Enums**: Handle invalid enum values gracefully
   ```java
   try {
       TicketStatus status = TicketStatus.valueOf(statusParam.toUpperCase());
   } catch (IllegalArgumentException e) {
       return ResponseEntity.badRequest()...
   }
   ```

4. **Use Projections**: For list views, consider using DTO projections
   ```java
   @Query("SELECT new com.rawgul.dto.TicketSummary(t.id, t.subject, t.status) FROM SupportTicket t")
   Page<TicketSummary> findAllSummaries(Pageable pageable);
   ```

### Frontend

1. **Debounce Search**: Prevent API spam during typing (already implemented)

2. **Loading States**: Always show loading indicators
   ```javascript
   this.showLoading();
   try {
       await this.loadData();
   } finally {
       this.hideLoading();
   }
   ```

3. **Error Handling**: Display user-friendly error messages
   ```javascript
   catch (error) {
       this.showError('Failed to load data. Please try again.');
   }
   ```

4. **Preserve State**: Consider using URL parameters to preserve filter state
   ```javascript
   // Update URL with current filters
   const params = new URLSearchParams({
       page: this.currentPage,
       status: this.filters.status,
       search: this.searchTerm
   });
   history.pushState(null, '', `?${params.toString()}`);
   ```

5. **Mobile Optimization**: Test on mobile devices (responsive design included)

6. **Accessibility**: Ensure keyboard navigation and screen reader support
   ```html
   <button aria-label="Go to page 2" onclick="goToPage(1)">2</button>
   ```

---

## Troubleshooting

### Common Issues

**1. No results showing**
- Check authentication token
- Verify API endpoint URL
- Check browser console for errors
- Verify data exists in database

**2. Filters not working**
- Ensure filter dropdown IDs match JavaScript
- Check enum values match backend
- Verify filter parameters in network tab

**3. Pagination not working**
- Check `JpaSpecificationExecutor` is extended
- Verify Pageable import: `org.springframework.data.domain.Pageable`
- Ensure page parameter is 0-indexed

**4. Search too slow**
- Add database indexes on searchable fields
- Consider full-text search (Elasticsearch, Solr)
- Limit search to specific fields

**5. Sort not working**
- Verify field name matches entity property
- Check for typos in sortBy parameter
- Ensure field is not nested (use join for related fields)

---

## Performance Optimization

### Database

1. **Add Indexes**
   ```sql
   CREATE INDEX idx_ticket_status ON support_tickets(status);
   CREATE INDEX idx_ticket_priority ON support_tickets(priority);
   CREATE INDEX idx_ticket_created_date ON support_tickets(created_date);
   ```

2. **Use Query Hints**
   ```java
   @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
   Page<SupportTicket> findAll(Specification<SupportTicket> spec, Pageable pageable);
   ```

### Caching

1. **Cache Filter Options**
   ```java
   @Cacheable("filterOptions")
   public Map<String, Object> getFilterOptions() {
       ...
   }
   ```

2. **Cache Page Results** (short TTL)
   ```java
   @Cacheable(value = "ticketPages", key = "#spec.toString() + #pageable.toString()")
   Page<SupportTicket> findAll(Specification<SupportTicket> spec, Pageable pageable);
   ```

### Frontend

1. **Implement Virtual Scrolling** for very large lists
2. **Lazy Load Images** in result cards
3. **Use Web Workers** for heavy rendering
4. **Implement Request Cancellation** for outdated requests

---

## Security Considerations

1. **Authentication**: All endpoints require authentication
   ```java
   @PreAuthorize("isAuthenticated()")
   ```

2. **Authorization**: Ensure users can only see their own tickets
   ```java
   if (!currentUser.isAdmin() && !ticket.getUser().equals(currentUser)) {
       throw new AccessDeniedException();
   }
   ```

3. **Input Validation**: Validate all query parameters
   ```java
   if (size < 1 || size > 100) {
       return ResponseEntity.badRequest()...
   }
   ```

4. **SQL Injection Prevention**: JPA Criteria API prevents SQL injection

5. **XSS Prevention**: Escape HTML in frontend
   ```javascript
   function escapeHtml(text) {
       const div = document.createElement('div');
       div.textContent = text;
       return div.innerHTML;
   }
   ```

---

## Future Enhancements

1. **Advanced Search**: Boolean operators, field-specific search
2. **Saved Filters**: Allow users to save common filter combinations
3. **Export**: Export search results to CSV/Excel
4. **Bulk Actions**: Select multiple items for bulk operations
5. **Real-time Updates**: WebSocket integration for live updates
6. **Search History**: Track and suggest recent searches
7. **Faceted Search**: Show result counts for each filter option
8. **Elasticsearch Integration**: For faster full-text search

---

## API Request Examples

### cURL Examples

**Search with filters:**
```bash
curl -X GET "http://localhost:8080/api/tickets/search?page=0&size=10&status=OPEN&priority=HIGH&sortBy=createdDate&sortDir=desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Search with text:**
```bash
curl -X GET "http://localhost:8080/api/tickets/search?search=payment&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Get user tickets:**
```bash
curl -X GET "http://localhost:8080/api/tickets/user/123?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### JavaScript Fetch Examples

```javascript
// Search tickets
const response = await fetch('/api/tickets/search?page=0&size=10&status=OPEN', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
const data = await response.json();

// With multiple filters
const params = new URLSearchParams({
    page: 0,
    size: 10,
    status: 'OPEN',
    priority: 'HIGH',
    search: 'payment',
    sortBy: 'createdDate',
    sortDir: 'desc'
});

const response = await fetch(`/api/tickets/search?${params}`, {
    headers: {
        'Authorization': `Bearer ${token}`
    }
});
```

---

## Testing

### Backend Unit Tests

```java
@Test
void testSearchWithFilters() {
    // Arrange
    SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();
    builder.with("status", ":", TicketStatus.OPEN);
    builder.with("priority", ":", TicketPriority.HIGH);
    
    Pageable pageable = PageRequest.of(0, 10);
    
    // Act
    Page<SupportTicket> result = ticketRepository.findAll(builder.build(), pageable);
    
    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().stream()
        .allMatch(t -> t.getStatus() == TicketStatus.OPEN && 
                      t.getPriority() == TicketPriority.HIGH));
}
```

### Frontend Tests

```javascript
// Test pagination manager initialization
test('PaginationManager initializes correctly', () => {
    const manager = new PaginationManager({
        apiEndpoint: '/api/test',
        containerId: 'test-container',
        paginationId: 'test-pagination',
        renderFunction: (item) => `<div>${item.name}</div>`
    });
    
    expect(manager.currentPage).toBe(0);
    expect(manager.pageSize).toBe(10);
    expect(manager.sortDir).toBe('desc');
});

// Test debounced search
test('Search input is debounced', async () => {
    jest.useFakeTimers();
    const manager = new PaginationManager({...});
    
    // Simulate typing
    simulateInput('test');
    simulateInput('test query');
    
    // Should only call API once after debounce
    jest.advanceTimersByTime(500);
    expect(mockLoadData).toHaveBeenCalledTimes(1);
});
```

---

## Conclusion

The search and pagination system provides a powerful, flexible, and user-friendly way to navigate through large datasets. The system is:

- **Scalable**: Handles millions of records efficiently
- **Flexible**: Supports complex queries with multiple filters
- **User-Friendly**: Intuitive interface with instant feedback
- **Mobile-Responsive**: Works on all device sizes
- **Performant**: Optimized queries and debounced searches
- **Secure**: Authentication and authorization built-in
- **Extensible**: Easy to add new filters and features

For questions or support, please contact the development team.

---

**Version**: 1.0  
**Last Updated**: November 3, 2025  
**Author**: Finance Tracker Development Team
