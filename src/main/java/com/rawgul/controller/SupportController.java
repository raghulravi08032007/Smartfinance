package com.rawgul.controller;

import com.rawgul.dto.MessageResponse;
import com.rawgul.dto.SupportTicketRequest;
import com.rawgul.model.SupportTicket;
import com.rawgul.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Support Ticket Controller.
 * Handles support ticket operations and maps to frontend support.html form.
 * 
 * @author Raghul
 * @since 1.0
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SupportController {

    private final SupportTicketService supportTicketService;

    /**
     * Create a support ticket (public endpoint for non-authenticated users).
     * 
     * POST /api/support/tickets
     * 
     * @param request the support ticket request
     * @return MessageResponse with ticket number
     */
    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createSupportTicket(@Valid @RequestBody SupportTicketRequest request) {
        SupportTicket ticket = supportTicketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }
    
    /**
     * Legacy endpoint for creating support ticket.
     * 
     * POST /api/support/ticket
     * 
     * @param request the support ticket request
     * @return MessageResponse with ticket number
     */
    @PostMapping("/ticket")
    public ResponseEntity<MessageResponse> createSupportTicketLegacy(@Valid @RequestBody SupportTicketRequest request) {
        SupportTicket ticket = supportTicketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Support ticket created successfully. Ticket number: " + ticket.getTicketNumber()));
    }

    /**
     * Create a support ticket for authenticated user.
     * 
     * POST /api/support/ticket/user
     * 
     * @param request the support ticket request
     * @param authentication the authentication object
     * @return MessageResponse with ticket number
     */
    @PostMapping("/ticket/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> createSupportTicketForUser(
            @Valid @RequestBody SupportTicketRequest request,
            Authentication authentication) {
        
        // In a real implementation, you would get the user ID from UserDetails
        // and use supportTicketService.createTicketForUser(userId, request)
        SupportTicket ticket = supportTicketService.createTicket(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Support ticket created successfully. Ticket number: " + ticket.getTicketNumber()));
    }

    /**
     * Get all tickets.
     * Admin: Get all tickets in system.
     * User: Get their own tickets.
     * 
     * GET /api/support/tickets
     * 
     * @param authentication the authentication object (optional)
     * @return List of tickets
     */
    @GetMapping("/tickets")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicket>> getAllTickets(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Admin: Get all tickets
            List<SupportTicket> tickets = supportTicketService.getAllTickets();
            return ResponseEntity.ok(tickets);
        } else {
            // User: Get all tickets (in a real app, filter by user ID)
            // TODO: Filter tickets by authenticated user's ID
            List<SupportTicket> tickets = supportTicketService.getAllTickets();
            return ResponseEntity.ok(tickets);
        }
    }
    
    /**
     * Get ticket by ID.
     * 
     * GET /api/support/tickets/{id}
     * 
     * @param id the ticket ID
     * @return Support ticket
     */
    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> getTicketById(@PathVariable Long id) {
        SupportTicket ticket = supportTicketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get ticket by ticket number.
     * 
     * GET /api/support/ticket/{ticketNumber}
     * 
     * @param ticketNumber the unique ticket number
     * @return Support ticket
     */
    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<SupportTicket> getTicketByNumber(@PathVariable String ticketNumber) {
        SupportTicket ticket = supportTicketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get tickets by status (admin only)
     */
    @GetMapping("/tickets/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicket>> getTicketsByStatus(
            @PathVariable SupportTicket.TicketStatus status) {
        List<SupportTicket> tickets = supportTicketService.getTicketsByStatus(status);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Update ticket (admin only).
     * Can update status, priority, or assignment.
     * 
     * PUT /api/support/tickets/{id}
     * 
     * @param id the ticket ID
     * @param updateRequest the update request (status, priority, assignee)
     * @return Updated support ticket
     */
    @PutMapping("/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> updateTicket(
            @PathVariable Long id,
            @RequestBody SupportTicket updateRequest) {
        SupportTicket ticket = supportTicketService.getTicketById(id);
        
        // Update fields if provided
        if (updateRequest.getStatus() != null) {
            ticket = supportTicketService.updateTicketStatus(id, updateRequest.getStatus());
        }
        if (updateRequest.getPriority() != null) {
            ticket = supportTicketService.updateTicketPriority(id, updateRequest.getPriority());
        }
        if (updateRequest.getAssignedTo() != null) {
            ticket = supportTicketService.assignTicket(id, updateRequest.getAssignedTo());
        }
        
        return ResponseEntity.ok(ticket);
    }

    /**
     * Assign ticket to support agent (admin only).
     * 
     * PUT /api/support/ticket/{ticketId}/assign
     * 
     * @param ticketId the ticket ID
     * @param assignee the assignee name
     * @return MessageResponse with success message
     */
    @PutMapping("/ticket/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> assignTicket(
            @PathVariable Long ticketId,
            @RequestParam String assignee) {
        supportTicketService.assignTicket(ticketId, assignee);
        return ResponseEntity.ok(new MessageResponse("Ticket assigned successfully"));
    }

    /**
     * Resolve ticket (admin only).
     * 
     * PUT /api/support/ticket/{ticketId}/resolve
     * 
     * @param ticketId the ticket ID
     * @param resolutionNotes the resolution notes
     * @return MessageResponse with success message
     */
    @PutMapping("/ticket/{ticketId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> resolveTicket(
            @PathVariable Long ticketId,
            @RequestBody String resolutionNotes) {
        supportTicketService.resolveTicket(ticketId, resolutionNotes);
        return ResponseEntity.ok(new MessageResponse("Ticket resolved successfully"));
    }

    /**
     * Close ticket (admin only).
     * 
     * PUT /api/support/ticket/{ticketId}/close
     * 
     * @param ticketId the ticket ID
     * @return MessageResponse with success message
     */
    @PutMapping("/ticket/{ticketId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> closeTicket(@PathVariable Long ticketId) {
        supportTicketService.closeTicket(ticketId);
        return ResponseEntity.ok(new MessageResponse("Ticket closed successfully"));
    }

    /**
     * Get open tickets count (admin only)
     */
    @GetMapping("/tickets/count/open")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getOpenTicketsCount() {
        Long count = supportTicketService.getOpenTicketsCount();
        return ResponseEntity.ok(count);
    }

}
