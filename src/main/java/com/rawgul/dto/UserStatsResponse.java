package com.rawgul.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Statistics Response DTO.
 * Used by admin to view user statistics.
 * 
 * @author Raghul
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    
    // Account statistics
    private Boolean active;
    private Boolean accountLocked;
    private Integer failedLoginAttempts;
    private String lastLogin;
    private String createdDate;
    
    // Ticket statistics
    private Long totalTickets;
    private Long openTickets;
    private Long resolvedTickets;
    private Long pendingTickets;
    
    // Role information
    private String roles;
    
}
