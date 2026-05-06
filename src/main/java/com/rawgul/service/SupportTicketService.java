package com.rawgul.service;

import com.rawgul.dto.SupportTicketRequest;
import com.rawgul.exceptions.ResourceNotFoundException;
import com.rawgul.model.SupportTicket;
import com.rawgul.model.SupportTicket.TicketPriority;
import com.rawgul.model.SupportTicket.TicketStatus;
import com.rawgul.model.User;
import com.rawgul.repository.SupportTicketRepository;
import com.rawgul.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;

/**
     * Create a support ticket from the frontend form (public access).
     * Automatically associates with existing user if email matches.
     * 
     * @param request the support ticket request
     * @return Created support ticket
     * @throws com.rawgul.exceptions.ValidationException if validation fails
     */
    public SupportTicket createTicket(SupportTicketRequest request) {
        log.info("Creating support ticket for email: {}", request.getEmail());
        
        validateTicketRequest(request);

        SupportTicket ticket = SupportTicket.builder()
                .subject(request.getSubject())
                .message(request.getQuery())
                .priority(request.getPriorityEnum())
                .status(TicketStatus.OPEN)
                .fullName(request.getFullname())
                .contactEmail(request.getEmail())
                .build();

        // Try to associate with existing user
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    ticket.setUser(user);
                    log.debug("Associated ticket with existing user id: {}", user.getId());
                });

        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Support ticket created with number: {}", savedTicket.getTicketNumber());
        
        return savedTicket;
    }
    
    /**
     * Validate support ticket request.
     * 
     * @param request the ticket request to validate
     * @throws com.rawgul.exceptions.ValidationException if validation fails
     */
    private void validateTicketRequest(SupportTicketRequest request) {
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new com.rawgul.exceptions.ValidationException("Subject is required");
        }
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new com.rawgul.exceptions.ValidationException("Message is required");
        }
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new com.rawgul.exceptions.ValidationException("Email is required");
        }
        
        if (request.getFullname() == null || request.getFullname().trim().isEmpty()) {
            throw new com.rawgul.exceptions.ValidationException("Full name is required");
        }
    }    /**
     * Create a support ticket for authenticated user.
     * 
     * @param userId the ID of the authenticated user
     * @param request the support ticket request
     * @return Created support ticket
     * @throws ResourceNotFoundException if user not found
     */
    public SupportTicket createTicketForUser(Long userId, SupportTicketRequest request) {
        log.info("Creating support ticket for user id: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        validateTicketRequest(request);

        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.getSubject())
                .message(request.getQuery())
                .priority(request.getPriorityEnum())
                .status(TicketStatus.OPEN)
                .fullName(request.getFullname() != null ? request.getFullname() : user.getFullName())
                .contactEmail(request.getEmail() != null ? request.getEmail() : user.getEmail())
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Support ticket created for user with ticket number: {}", savedTicket.getTicketNumber());
        
        return savedTicket;
    }

    public SupportTicket getTicketById(Long id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    public SupportTicket getTicketByNumber(String ticketNumber) {
        return supportTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with number: " + ticketNumber));
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    public List<SupportTicket> getTicketsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return supportTicketRepository.findByUserOrderByCreatedDateDesc(user);
    }

    public List<SupportTicket> getTicketsByStatus(TicketStatus status) {
        return supportTicketRepository.findByStatus(status);
    }

    public List<SupportTicket> getTicketsByPriority(TicketPriority priority) {
        return supportTicketRepository.findByPriority(priority);
    }

    /**
     * Assign ticket to support staff (admin operation).
     * 
     * @param ticketId the ticket ID
     * @param assignee the name/ID of the assignee
     * @return Updated support ticket
     * @throws ResourceNotFoundException if ticket not found
     */
    public SupportTicket assignTicket(Long ticketId, String assignee) {
        log.info("Assigning ticket {} to {}", ticketId, assignee);
        
        SupportTicket ticket = getTicketById(ticketId);
        ticket.assignTo(assignee);
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Ticket {} assigned successfully", ticketId);
        
        return savedTicket;
    }

    /**
     * Resolve ticket with resolution notes (admin operation).
     * 
     * @param ticketId the ticket ID
     * @param resolutionNotes the resolution notes
     * @return Updated support ticket
     * @throws ResourceNotFoundException if ticket not found
     */
    public SupportTicket resolveTicket(Long ticketId, String resolutionNotes) {
        log.info("Resolving ticket {}", ticketId);
        
        SupportTicket ticket = getTicketById(ticketId);
        ticket.resolve(resolutionNotes);
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Ticket {} resolved successfully", ticketId);
        
        return savedTicket;
    }

    /**
     * Close ticket (admin operation).
     * 
     * @param ticketId the ticket ID
     * @return Updated support ticket
     * @throws ResourceNotFoundException if ticket not found
     */
    public SupportTicket closeTicket(Long ticketId) {
        log.info("Closing ticket {}", ticketId);
        
        SupportTicket ticket = getTicketById(ticketId);
        ticket.close();
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Ticket {} closed successfully", ticketId);
        
        return savedTicket;
    }

    /**
     * Update ticket status (admin operation).
     * 
     * @param ticketId the ticket ID
     * @param status the new status
     * @return Updated support ticket
     * @throws ResourceNotFoundException if ticket not found
     */
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus status) {
        log.info("Updating ticket {} status to {}", ticketId, status);
        
        SupportTicket ticket = getTicketById(ticketId);
        ticket.setStatus(status);
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Ticket {} status updated successfully", ticketId);
        
        return savedTicket;
    }
    
    /**
     * Update ticket priority (admin operation).
     * 
     * @param ticketId the ticket ID
     * @param priority the new priority
     * @return Updated support ticket
     * @throws ResourceNotFoundException if ticket not found
     */
    public SupportTicket updateTicketPriority(Long ticketId, TicketPriority priority) {
        log.info("Updating ticket {} priority to {}", ticketId, priority);
        
        SupportTicket ticket = getTicketById(ticketId);
        ticket.setPriority(priority);
        
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        log.info("Ticket {} priority updated successfully", ticketId);
        
        return savedTicket;
    }

    public Long getOpenTicketsCount() {
        return supportTicketRepository.countByStatus(TicketStatus.OPEN);
    }

    public Long getOpenTicketsByUser(Long userId) {
        return supportTicketRepository.countOpenTicketsByUserId(userId);
    }

    public List<SupportTicket> getRecentTickets(int limit) {
        return supportTicketRepository.findTop10ByOrderByCreatedDateDesc();
    }

    /**
     * Get all open tickets (admin dashboard).
     * 
     * @return List of open tickets
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllOpenTickets() {
        return supportTicketRepository.findAllOpenTickets();
    }
    
    /**
     * Get all unassigned tickets (admin dashboard).
     * 
     * @return List of unassigned tickets
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllUnassignedTickets() {
        return supportTicketRepository.findAllUnassignedTickets();
    }
    
    /**
     * Get high priority open tickets (admin dashboard).
     * 
     * @return List of high priority open tickets
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getHighPriorityOpenTickets() {
        return supportTicketRepository.findHighPriorityOpenTickets();
    }
    
    /**
     * Get tickets by user ID.
     * 
     * @param userId the user ID
     * @return List of tickets for the user
     */
    @Transactional(readOnly = true)
    public List<SupportTicket> getTicketsByUserId(Long userId) {
        return supportTicketRepository.findByUserId(userId);
    }

    /**
     * Delete ticket (admin operation).
     * 
     * @param ticketId the ticket ID
     * @throws ResourceNotFoundException if ticket not found
     */
    public void deleteTicket(Long ticketId) {
        log.info("Deleting ticket {}", ticketId);
        
        if (!supportTicketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }
        
        supportTicketRepository.deleteById(ticketId);
        log.info("Ticket {} deleted successfully", ticketId);
    }

}
