package com.rawgul.dto;

import com.rawgul.model.SupportTicket.TicketPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating support tickets from the frontend support.html form
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullname;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotNull(message = "Priority is required")
    private String priority; // Will be converted to TicketPriority enum

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    private String query;

    // Helper method to get priority as enum
    public TicketPriority getPriorityEnum() {
        try {
            return TicketPriority.valueOf(priority.toUpperCase());
        } catch (Exception e) {
            return TicketPriority.MEDIUM;
        }
    }

}
