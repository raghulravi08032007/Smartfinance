package com.rawgul.controller;

import com.rawgul.dto.MessageResponse;
import com.rawgul.dto.UserStatsResponse;
import com.rawgul.model.Role;
import com.rawgul.model.SupportTicket;
import com.rawgul.model.User;
import com.rawgul.service.AdminService;
import com.rawgul.service.SupportTicketService;
import com.rawgul.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin Controller.
 * Handles administrative operations including user management, ticket management,
 * and system analytics.
 * 
 * All endpoints require ADMIN role.
 * 
 * @author Raghul
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final SupportTicketService supportTicketService;

    // ============================================================================
    // USER MANAGEMENT ENDPOINTS
    // ============================================================================

    /**
     * Get all users with pagination and sorting.
     * 
     * GET /api/admin/users
     * 
     * @param page the page number (default: 0)
     * @param size the page size (default: 10)
     * @param sortBy the field to sort by (default: id)
     * @param direction the sort direction (default: ASC)
     * @return Paginated list of users
     */
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        
        log.debug("Admin: Fetching users - page: {}, size: {}, sortBy: {}, direction: {}", 
                page, size, sortBy, direction);
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<User> users = adminService.getAllUsers(pageable);
        
        return ResponseEntity.ok(users);
    }

    /**
     * Search users by username or email.
     * 
     * GET /api/admin/users/search
     * 
     * @param query the search query
     * @param page the page number
     * @param size the page size
     * @return Paginated list of matching users
     */
    @GetMapping("/users/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Admin: Searching users with query: {}", query);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = adminService.searchUsers(query, pageable);
        
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID with full details.
     * 
     * GET /api/admin/users/{id}
     * 
     * @param id the user ID
     * @return User entity with all details
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.debug("Admin: Fetching user details for id: {}", id);
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user statistics.
     * 
     * GET /api/admin/users/{id}/stats
     * 
     * @param id the user ID
     * @return User statistics including ticket counts
     */
    @GetMapping("/users/{id}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long id) {
        log.debug("Admin: Fetching user statistics for id: {}", id);
        UserStatsResponse stats = adminService.getUserStats(id);
        return ResponseEntity.ok(stats);
    }

    /**
     * Update user roles.
     * 
     * PUT /api/admin/users/{id}/roles
     * 
     * @param id the user ID
     * @param roles the set of role IDs
     * @return Updated user
     */
    @PutMapping("/users/{id}/roles")
    public ResponseEntity<User> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Set<Long> roles) {
        
        log.info("Admin: Updating roles for user id: {}", id);
        User user = adminService.updateUserRoles(id, roles);
        return ResponseEntity.ok(user);
    }

    /**
     * Activate user account.
     * 
     * PUT /api/admin/users/{id}/activate
     * 
     * @param id the user ID
     * @return MessageResponse
     */
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable Long id) {
        log.info("Admin: Activating user account: {}", id);
        userService.activateUser(id);
        return ResponseEntity.ok(new MessageResponse("User account activated successfully"));
    }

    /**
     * Deactivate user account.
     * 
     * PUT /api/admin/users/{id}/deactivate
     * 
     * @param id the user ID
     * @return MessageResponse
     */
    @PutMapping("/users/{id}/deactivate")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable Long id) {
        log.info("Admin: Deactivating user account: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(new MessageResponse("User account deactivated successfully"));
    }

    /**
     * Lock user account.
     * 
     * PUT /api/admin/users/{id}/lock
     * 
     * @param id the user ID
     * @return MessageResponse
     */
    @PutMapping("/users/{id}/lock")
    public ResponseEntity<MessageResponse> lockUser(@PathVariable Long id) {
        log.info("Admin: Locking user account: {}", id);
        userService.lockUserAccount(id);
        return ResponseEntity.ok(new MessageResponse("User account locked successfully"));
    }

    /**
     * Unlock user account.
     * 
     * PUT /api/admin/users/{id}/unlock
     * 
     * @param id the user ID
     * @return MessageResponse
     */
    @PutMapping("/users/{id}/unlock")
    public ResponseEntity<MessageResponse> unlockUser(@PathVariable Long id) {
        log.info("Admin: Unlocking user account: {}", id);
        userService.unlockUserAccount(id);
        return ResponseEntity.ok(new MessageResponse("User account unlocked successfully"));
    }

    /**
     * Delete user account.
     * 
     * DELETE /api/admin/users/{id}
     * 
     * @param id the user ID
     * @return No content
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Admin: Deleting user: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // TICKET MANAGEMENT ENDPOINTS
    // ============================================================================

    /**
     * Get all support tickets with pagination, filtering, and sorting.
     * 
     * GET /api/admin/tickets
     * 
     * @param status filter by status (optional)
     * @param priority filter by priority (optional)
     * @param page the page number
     * @param size the page size
     * @param sortBy the field to sort by
     * @param direction the sort direction
     * @return Paginated list of tickets
     */
    @GetMapping("/tickets")
    public ResponseEntity<Page<SupportTicket>> getAllTickets(
            @RequestParam(required = false) SupportTicket.TicketStatus status,
            @RequestParam(required = false) SupportTicket.TicketPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        
        log.debug("Admin: Fetching tickets - status: {}, priority: {}, page: {}, size: {}", 
                status, priority, page, size);
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<SupportTicket> tickets = adminService.getAllTickets(status, priority, pageable);
        
        return ResponseEntity.ok(tickets);
    }

    /**
     * Search tickets by subject, message, or ticket number.
     * 
     * GET /api/admin/tickets/search
     * 
     * @param query the search query
     * @param page the page number
     * @param size the page size
     * @return Paginated list of matching tickets
     */
    @GetMapping("/tickets/search")
    public ResponseEntity<Page<SupportTicket>> searchTickets(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Admin: Searching tickets with query: {}", query);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<SupportTicket> tickets = adminService.searchTickets(query, pageable);
        
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get ticket by ID.
     * 
     * GET /api/admin/tickets/{id}
     * 
     * @param id the ticket ID
     * @return Support ticket
     */
    @GetMapping("/tickets/{id}")
    public ResponseEntity<SupportTicket> getTicketById(@PathVariable Long id) {
        log.debug("Admin: Fetching ticket details for id: {}", id);
        SupportTicket ticket = supportTicketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Update ticket status.
     * 
     * PUT /api/admin/tickets/{id}/status
     * 
     * @param id the ticket ID
     * @param status the new status
     * @return Updated ticket
     */
    @PutMapping("/tickets/{id}/status")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam SupportTicket.TicketStatus status) {
        
        log.info("Admin: Updating ticket {} status to {}", id, status);
        SupportTicket ticket = supportTicketService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Update ticket priority.
     * 
     * PUT /api/admin/tickets/{id}/priority
     * 
     * @param id the ticket ID
     * @param priority the new priority
     * @return Updated ticket
     */
    @PutMapping("/tickets/{id}/priority")
    public ResponseEntity<SupportTicket> updateTicketPriority(
            @PathVariable Long id,
            @RequestParam SupportTicket.TicketPriority priority) {
        
        log.info("Admin: Updating ticket {} priority to {}", id, priority);
        SupportTicket ticket = supportTicketService.updateTicketPriority(id, priority);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Assign ticket to support agent.
     * 
     * PUT /api/admin/tickets/{id}/assign
     * 
     * @param id the ticket ID
     * @param assignee the assignee name
     * @return Updated ticket
     */
    @PutMapping("/tickets/{id}/assign")
    public ResponseEntity<SupportTicket> assignTicket(
            @PathVariable Long id,
            @RequestParam String assignee) {
        
        log.info("Admin: Assigning ticket {} to {}", id, assignee);
        SupportTicket ticket = supportTicketService.assignTicket(id, assignee);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Resolve ticket with resolution notes.
     * 
     * PUT /api/admin/tickets/{id}/resolve
     * 
     * @param id the ticket ID
     * @param resolutionNotes the resolution notes
     * @return Updated ticket
     */
    @PutMapping("/tickets/{id}/resolve")
    public ResponseEntity<SupportTicket> resolveTicket(
            @PathVariable Long id,
            @RequestBody String resolutionNotes) {
        
        log.info("Admin: Resolving ticket {}", id);
        SupportTicket ticket = supportTicketService.resolveTicket(id, resolutionNotes);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Close ticket.
     * 
     * PUT /api/admin/tickets/{id}/close
     * 
     * @param id the ticket ID
     * @return Updated ticket
     */
    @PutMapping("/tickets/{id}/close")
    public ResponseEntity<SupportTicket> closeTicket(@PathVariable Long id) {
        log.info("Admin: Closing ticket {}", id);
        SupportTicket ticket = supportTicketService.closeTicket(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Delete ticket.
     * 
     * DELETE /api/admin/tickets/{id}
     * 
     * @param id the ticket ID
     * @return No content
     */
    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        log.info("Admin: Deleting ticket: {}", id);
        supportTicketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // STATISTICS AND ANALYTICS ENDPOINTS
    // ============================================================================

    /**
     * Get dashboard statistics.
     * 
     * GET /api/admin/stats/dashboard
     * 
     * @return Dashboard statistics
     */
    @GetMapping("/stats/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.debug("Admin: Fetching dashboard statistics");
        Map<String, Object> stats = adminService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get user statistics.
     * 
     * GET /api/admin/stats/users
     * 
     * @return User statistics
     */
    @GetMapping("/stats/users")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        log.debug("Admin: Fetching user statistics");
        Map<String, Object> stats = adminService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get ticket statistics.
     * 
     * GET /api/admin/stats/tickets
     * 
     * @return Ticket statistics
     */
    @GetMapping("/stats/tickets")
    public ResponseEntity<Map<String, Object>> getTicketStatistics() {
        log.debug("Admin: Fetching ticket statistics");
        Map<String, Object> stats = adminService.getTicketStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get ticket statistics by status.
     * 
     * GET /api/admin/stats/tickets/status
     * 
     * @return Ticket count by status
     */
    @GetMapping("/stats/tickets/status")
    public ResponseEntity<Map<SupportTicket.TicketStatus, Long>> getTicketsByStatus() {
        log.debug("Admin: Fetching tickets by status");
        Map<SupportTicket.TicketStatus, Long> stats = adminService.getTicketCountByStatus();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get ticket statistics by priority.
     * 
     * GET /api/admin/stats/tickets/priority
     * 
     * @return Ticket count by priority
     */
    @GetMapping("/stats/tickets/priority")
    public ResponseEntity<Map<SupportTicket.TicketPriority, Long>> getTicketsByPriority() {
        log.debug("Admin: Fetching tickets by priority");
        Map<SupportTicket.TicketPriority, Long> stats = adminService.getTicketCountByPriority();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get system activity logs (recent users and tickets).
     * 
     * GET /api/admin/stats/activity
     * 
     * @param limit the number of recent activities to fetch
     * @return Recent activity data
     */
    @GetMapping("/stats/activity")
    public ResponseEntity<Map<String, Object>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("Admin: Fetching recent activity with limit: {}", limit);
        Map<String, Object> activity = adminService.getRecentActivity(limit);
        return ResponseEntity.ok(activity);
    }

    // ============================================================================
    // ROLE MANAGEMENT ENDPOINTS
    // ============================================================================

    /**
     * Get all available roles.
     * 
     * GET /api/admin/roles
     * 
     * @return List of all roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        log.debug("Admin: Fetching all roles");
        List<Role> roles = userService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

}
