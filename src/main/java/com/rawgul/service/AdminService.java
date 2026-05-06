package com.rawgul.service;

import com.rawgul.dto.UserStatsResponse;
import com.rawgul.exceptions.ResourceNotFoundException;
import com.rawgul.model.Role;
import com.rawgul.model.SupportTicket;
import com.rawgul.model.User;
import com.rawgul.repository.RoleRepository;
import com.rawgul.repository.SupportTicketRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service.
 * Handles administrative operations including user management, ticket management,
 * and system analytics.
 * 
 * @author Raghul
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final RoleRepository roleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ============================================================================
    // USER MANAGEMENT
    // ============================================================================

    /**
     * Get all users with pagination.
     * 
     * @param pageable pagination information
     * @return Page of users
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable);
    }

    /**
     * Search users by username or email.
     * 
     * @param query search query
     * @param pageable pagination information
     * @return Page of matching users
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        log.debug("Searching users with query: {}", query);
        
        List<User> allUsers = userRepository.findAll();
        List<User> filteredUsers = allUsers.stream()
                .filter(user -> 
                    user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(query.toLowerCase()) ||
                    (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(query.toLowerCase())) ||
                    (user.getLastName() != null && user.getLastName().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        
        return new PageImpl<>(
            filteredUsers.subList(start, end),
            pageable,
            filteredUsers.size()
        );
    }

    /**
     * Get user statistics.
     * 
     * @param userId the user ID
     * @return User statistics
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        log.debug("Fetching statistics for user id: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        List<SupportTicket> tickets = supportTicketRepository.findByUserId(userId);
        
        long totalTickets = tickets.size();
        long openTickets = tickets.stream()
                .filter(t -> t.getStatus() == SupportTicket.TicketStatus.OPEN || 
                           t.getStatus() == SupportTicket.TicketStatus.IN_PROGRESS)
                .count();
        long resolvedTickets = tickets.stream()
                .filter(t -> t.getStatus() == SupportTicket.TicketStatus.RESOLVED || 
                           t.getStatus() == SupportTicket.TicketStatus.CLOSED)
                .count();
        long pendingTickets = tickets.stream()
                .filter(t -> t.getStatus() == SupportTicket.TicketStatus.PENDING_USER)
                .count();
        
        String roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(", "));
        
        return UserStatsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .active(user.getActive())
                .accountLocked(user.getAccountLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().format(DATE_FORMATTER) : "Never")
                .createdDate(user.getCreatedDate() != null ? user.getCreatedDate().format(DATE_FORMATTER) : "Unknown")
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .resolvedTickets(resolvedTickets)
                .pendingTickets(pendingTickets)
                .roles(roles)
                .build();
    }

    /**
     * Update user roles.
     * 
     * @param userId the user ID
     * @param roleIds the set of role IDs
     * @return Updated user
     */
    public User updateUserRoles(Long userId, Set<Long> roleIds) {
        log.info("Updating roles for user id: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        Set<Role> roles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
            roles.add(role);
        }
        
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        log.info("Roles updated successfully for user id: {}", userId);
        return savedUser;
    }

    // ============================================================================
    // TICKET MANAGEMENT
    // ============================================================================

    /**
     * Get all support tickets with filtering and pagination.
     * 
     * @param status filter by status (optional)
     * @param priority filter by priority (optional)
     * @param pageable pagination information
     * @return Page of tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> getAllTickets(
            SupportTicket.TicketStatus status,
            SupportTicket.TicketPriority priority,
            Pageable pageable) {
        
        log.debug("Fetching tickets - status: {}, priority: {}", status, priority);
        
        List<SupportTicket> allTickets;
        
        if (status != null && priority != null) {
            allTickets = supportTicketRepository.findByStatusAndPriority(status, priority);
        } else if (status != null) {
            allTickets = supportTicketRepository.findByStatus(status);
        } else if (priority != null) {
            allTickets = supportTicketRepository.findByPriority(priority);
        } else {
            allTickets = supportTicketRepository.findAll();
        }
        
        // Sort tickets
        allTickets.sort((t1, t2) -> {
            LocalDateTime date1 = t1.getCreatedDate();
            LocalDateTime date2 = t2.getCreatedDate();
            return date2.compareTo(date1); // Descending order
        });
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTickets.size());
        
        return new PageImpl<>(
            allTickets.subList(start, end),
            pageable,
            allTickets.size()
        );
    }

    /**
     * Search tickets by subject, message, or ticket number.
     * 
     * @param query search query
     * @param pageable pagination information
     * @return Page of matching tickets
     */
    @Transactional(readOnly = true)
    public Page<SupportTicket> searchTickets(String query, Pageable pageable) {
        log.debug("Searching tickets with query: {}", query);
        
        List<SupportTicket> allTickets = supportTicketRepository.findAll();
        List<SupportTicket> filteredTickets = allTickets.stream()
                .filter(ticket -> 
                    ticket.getTicketNumber().toLowerCase().contains(query.toLowerCase()) ||
                    ticket.getSubject().toLowerCase().contains(query.toLowerCase()) ||
                    ticket.getMessage().toLowerCase().contains(query.toLowerCase()) ||
                    (ticket.getFullName() != null && ticket.getFullName().toLowerCase().contains(query.toLowerCase())) ||
                    (ticket.getContactEmail() != null && ticket.getContactEmail().toLowerCase().contains(query.toLowerCase()))
                )
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredTickets.size());
        
        return new PageImpl<>(
            filteredTickets.subList(start, end),
            pageable,
            filteredTickets.size()
        );
    }

    // ============================================================================
    // STATISTICS AND ANALYTICS
    // ============================================================================

    /**
     * Get dashboard statistics.
     * 
     * @return Map of dashboard statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        log.debug("Fetching dashboard statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(User::getActive)
                .count();
        long inactiveUsers = totalUsers - activeUsers;
        long lockedUsers = userRepository.findAll().stream()
                .filter(User::getAccountLocked)
                .count();
        
        // Ticket statistics
        long totalTickets = supportTicketRepository.count();
        long openTickets = supportTicketRepository.countByStatus(SupportTicket.TicketStatus.OPEN);
        long inProgressTickets = supportTicketRepository.countByStatus(SupportTicket.TicketStatus.IN_PROGRESS);
        long resolvedTickets = supportTicketRepository.countByStatus(SupportTicket.TicketStatus.RESOLVED);
        long closedTickets = supportTicketRepository.countByStatus(SupportTicket.TicketStatus.CLOSED);
        long pendingTickets = supportTicketRepository.countByStatus(SupportTicket.TicketStatus.PENDING_USER);
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", inactiveUsers);
        stats.put("lockedUsers", lockedUsers);
        
        stats.put("totalTickets", totalTickets);
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("resolvedTickets", resolvedTickets);
        stats.put("closedTickets", closedTickets);
        stats.put("pendingTickets", pendingTickets);
        
        return stats;
    }

    /**
     * Get user statistics.
     * 
     * @return Map of user statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        log.debug("Fetching user statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        List<User> allUsers = userRepository.findAll();
        
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::getActive).count();
        long inactiveUsers = totalUsers - activeUsers;
        long lockedUsers = allUsers.stream().filter(User::getAccountLocked).count();
        
        // Users by role
        Map<String, Long> usersByRole = new HashMap<>();
        allUsers.forEach(user -> {
            user.getRoles().forEach(role -> {
                usersByRole.merge(role.getName().name(), 1L, Long::sum);
            });
        });
        
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", inactiveUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("usersByRole", usersByRole);
        
        return stats;
    }

    /**
     * Get ticket statistics.
     * 
     * @return Map of ticket statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTicketStatistics() {
        log.debug("Fetching ticket statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        long totalTickets = supportTicketRepository.count();
        
        // Tickets by status
        Map<String, Long> ticketsByStatus = new HashMap<>();
        for (SupportTicket.TicketStatus status : SupportTicket.TicketStatus.values()) {
            ticketsByStatus.put(status.name(), supportTicketRepository.countByStatus(status));
        }
        
        // Tickets by priority
        Map<String, Long> ticketsByPriority = new HashMap<>();
        for (SupportTicket.TicketPriority priority : SupportTicket.TicketPriority.values()) {
            long count = supportTicketRepository.findByPriority(priority).size();
            ticketsByPriority.put(priority.name(), count);
        }
        
        // Unassigned tickets
        long unassignedTickets = supportTicketRepository.findAllUnassignedTickets().size();
        
        stats.put("totalTickets", totalTickets);
        stats.put("ticketsByStatus", ticketsByStatus);
        stats.put("ticketsByPriority", ticketsByPriority);
        stats.put("unassignedTickets", unassignedTickets);
        
        return stats;
    }

    /**
     * Get ticket count by status.
     * 
     * @return Map of status to count
     */
    @Transactional(readOnly = true)
    public Map<SupportTicket.TicketStatus, Long> getTicketCountByStatus() {
        log.debug("Fetching ticket count by status");
        
        Map<SupportTicket.TicketStatus, Long> counts = new EnumMap<>(SupportTicket.TicketStatus.class);
        for (SupportTicket.TicketStatus status : SupportTicket.TicketStatus.values()) {
            counts.put(status, supportTicketRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Get ticket count by priority.
     * 
     * @return Map of priority to count
     */
    @Transactional(readOnly = true)
    public Map<SupportTicket.TicketPriority, Long> getTicketCountByPriority() {
        log.debug("Fetching ticket count by priority");
        
        Map<SupportTicket.TicketPriority, Long> counts = new EnumMap<>(SupportTicket.TicketPriority.class);
        for (SupportTicket.TicketPriority priority : SupportTicket.TicketPriority.values()) {
            long count = supportTicketRepository.findByPriority(priority).size();
            counts.put(priority, count);
        }
        return counts;
    }

    /**
     * Get recent activity (users and tickets).
     * 
     * @param limit the number of recent items to fetch
     * @return Map of recent activity
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRecentActivity(int limit) {
        log.debug("Fetching recent activity with limit: {}", limit);
        
        Map<String, Object> activity = new HashMap<>();
        
        // Recent users
        List<User> allUsers = userRepository.findAll();
        List<User> recentUsers = allUsers.stream()
                .sorted((u1, u2) -> {
                    LocalDateTime date1 = u1.getCreatedDate();
                    LocalDateTime date2 = u2.getCreatedDate();
                    if (date1 == null) return 1;
                    if (date2 == null) return -1;
                    return date2.compareTo(date1);
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        // Recent tickets
        List<SupportTicket> recentTickets = supportTicketRepository.findTop10ByOrderByCreatedDateDesc()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        activity.put("recentUsers", recentUsers);
        activity.put("recentTickets", recentTickets);
        
        return activity;
    }

}
