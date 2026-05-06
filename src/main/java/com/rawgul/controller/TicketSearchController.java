package com.rawgul.controller;

import com.rawgul.dto.PageableResponse;
import com.rawgul.dto.SearchCriteria;
import com.rawgul.model.SupportTicket;
import com.rawgul.model.SupportTicket.TicketPriority;
import com.rawgul.model.SupportTicket.TicketStatus;
import com.rawgul.repository.SupportTicketRepository;
import com.rawgul.specification.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API Controller for support ticket search and pagination.
 * Provides endpoints for searching, filtering, and paginating support tickets.
 */
@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TicketSearchController {

    @Autowired
    private SupportTicketRepository ticketRepository;

    /**
     * Search and paginate support tickets with dynamic filters.
     * 
     * @param page Page number (0-indexed, default: 0)
     * @param size Number of items per page (default: 10)
     * @param sortBy Field to sort by (default: createdDate)
     * @param sortDir Sort direction (asc/desc, default: desc)
     * @param status Filter by ticket status
     * @param priority Filter by ticket priority
     * @param assignedTo Filter by assigned user
     * @param search Search term for subject and description
     * @param userId Filter by user ID
     * @return Pageable response with tickets and metadata
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId
    ) {
        try {
            // Build specification based on filters
            SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();

            // Status filter
            if (status != null && !status.isEmpty()) {
                try {
                    TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
                    builder.with("status", ":", ticketStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                            Map.of("success", false, "message", "Invalid status value: " + status)
                    );
                }
            }

            // Priority filter
            if (priority != null && !priority.isEmpty()) {
                try {
                    TicketPriority ticketPriority = TicketPriority.valueOf(priority.toUpperCase());
                    builder.with("priority", ":", ticketPriority);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                            Map.of("success", false, "message", "Invalid priority value: " + priority)
                    );
                }
            }

            // Assigned to filter
            if (assignedTo != null && !assignedTo.isEmpty()) {
                builder.with("assignedTo", "~", assignedTo);
            }

            // User ID filter
            if (userId != null) {
                builder.with("id", ":", userId, "user");
            }

            // Search filter (searches in subject and description)
            if (search != null && !search.isEmpty()) {
                // Create separate specifications for subject and description search
                SpecificationBuilder<SupportTicket> searchBuilder = new SpecificationBuilder<>();
                searchBuilder.with("subject", "~", search);
                searchBuilder.with("description", "~", search);
                searchBuilder.withOr();
                
                Specification<SupportTicket> searchSpec = searchBuilder.build();
                Specification<SupportTicket> filterSpec = builder.build();
                
                // Combine search with filters using AND
                Specification<SupportTicket> combinedSpec = filterSpec != null 
                        ? Specification.where(filterSpec).and(searchSpec)
                        : searchSpec;
                
                return executeSearch(combinedSpec, page, size, sortBy, sortDir);
            }

            // Build final specification
            Specification<SupportTicket> spec = builder.build();

            return executeSearch(spec, page, size, sortBy, sortDir);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Error searching tickets: " + e.getMessage())
            );
        }
    }

    /**
     * Execute the search with pagination and sorting.
     */
    private ResponseEntity<?> executeSearch(
            Specification<SupportTicket> spec,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // Validate page size
        if (size > 100) {
            size = 100; // Max page size
        }
        if (size < 1) {
            size = 10;
        }

        // Create sort
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Execute query
        Page<SupportTicket> ticketPage = spec != null 
                ? ticketRepository.findAll(spec, pageable)
                : ticketRepository.findAll(pageable);

        // Convert to response
        PageableResponse<SupportTicket> response = PageableResponse.fromPage(ticketPage);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
        ));
    }

    /**
     * Get paginated tickets for a specific user.
     * 
     * @param userId User ID
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Pageable response with user's tickets
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserTickets(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            SpecificationBuilder<SupportTicket> builder = new SpecificationBuilder<>();
            builder.with("id", ":", userId, "user");
            
            return executeSearch(builder.build(), page, size, sortBy, sortDir);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Error fetching user tickets: " + e.getMessage())
            );
        }
    }

    /**
     * Get paginated tickets by status.
     * 
     * @param status Ticket status
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Pageable response with filtered tickets
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTicketsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<SupportTicket> ticketPage = ticketRepository.findByStatus(ticketStatus, pageable);
            PageableResponse<SupportTicket> response = PageableResponse.fromPage(ticketPage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Invalid status value: " + status)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Error fetching tickets: " + e.getMessage())
            );
        }
    }

    /**
     * Get paginated tickets by priority.
     * 
     * @param priority Ticket priority
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param sortDir Sort direction
     * @return Pageable response with filtered tickets
     */
    @GetMapping("/priority/{priority}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTicketsByPriority(
            @PathVariable String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            TicketPriority ticketPriority = TicketPriority.valueOf(priority.toUpperCase());
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<SupportTicket> ticketPage = ticketRepository.findByPriority(ticketPriority, pageable);
            PageableResponse<SupportTicket> response = PageableResponse.fromPage(ticketPage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Invalid priority value: " + priority)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Error fetching tickets: " + e.getMessage())
            );
        }
    }

    /**
     * Get filter options for tickets (available statuses, priorities).
     * 
     * @return Filter options
     */
    @GetMapping("/filters")
    public ResponseEntity<?> getFilterOptions() {
        try {
            Map<String, Object> filters = new HashMap<>();
            
            // Get all statuses
            List<Map<String, String>> statuses = new ArrayList<>();
            for (TicketStatus status : TicketStatus.values()) {
                statuses.add(Map.of(
                        "value", status.name(),
                        "label", formatEnumName(status.name())
                ));
            }
            filters.put("statuses", statuses);
            
            // Get all priorities
            List<Map<String, String>> priorities = new ArrayList<>();
            for (TicketPriority priority : TicketPriority.values()) {
                priorities.add(Map.of(
                        "value", priority.name(),
                        "label", formatEnumName(priority.name())
                ));
            }
            filters.put("priorities", priorities);
            
            // Get sortable fields
            filters.put("sortableFields", Arrays.asList(
                    Map.of("value", "createdDate", "label", "Created Date"),
                    Map.of("value", "updatedDate", "label", "Updated Date"),
                    Map.of("value", "subject", "label", "Subject"),
                    Map.of("value", "status", "label", "Status"),
                    Map.of("value", "priority", "label", "Priority")
            ));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filters", filters
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Error fetching filter options: " + e.getMessage())
            );
        }
    }

    /**
     * Format enum name for display (e.g., IN_PROGRESS -> In Progress)
     */
    private String formatEnumName(String enumName) {
        return Arrays.stream(enumName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(enumName);
    }
}
