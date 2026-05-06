package com.rawgul.repository;

import com.rawgul.model.SupportTicket;
import com.rawgul.model.SupportTicket.TicketPriority;
import com.rawgul.model.SupportTicket.TicketStatus;
import com.rawgul.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SupportTicket entity.
 * Provides database access methods for support ticket management operations.
 * 
 * @author Raghul
 * @since 1.0
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    /**
     * Find ticket by ticket number.
     * 
     * @param ticketNumber the unique ticket number
     * @return Optional containing the ticket if found, empty otherwise
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets for a specific user.
     * 
     * @param user the user entity
     * @return List of tickets for the user
     */
    List<SupportTicket> findByUser(User user);

    /**
     * Find all tickets for a specific user ordered by creation date.
     * 
     * @param user the user entity
     * @return List of tickets ordered by creation date descending
     */
    List<SupportTicket> findByUserOrderByCreatedDateDesc(User user);

    /**
     * Find all tickets for a specific user with pagination.
     * 
     * @param user the user entity
     * @param pageable pagination information
     * @return Page of tickets for the user
     */
    Page<SupportTicket> findByUser(User user, Pageable pageable);

    /**
     * Find all tickets created by a specific user ID.
     * 
     * @param userId the ID of the user
     * @return List of tickets created by the user
     */
    @Query("SELECT st FROM SupportTicket st WHERE st.user.id = :userId ORDER BY st.createdDate DESC")
    List<SupportTicket> findByUserId(@Param("userId") Long userId);

    /**
     * Find all tickets with a specific status.
     * 
     * @param status the ticket status to filter by
     * @return List of tickets with the specified status
     */
    List<SupportTicket> findByStatus(TicketStatus status);

    /**
     * Find all tickets with a specific status with pagination.
     * 
     * @param status the ticket status to filter by
     * @param pageable pagination information
     * @return Page of tickets with the specified status
     */
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    /**
     * Find all tickets with a specific priority.
     * 
     * @param priority the ticket priority to filter by
     * @return List of tickets with the specified priority
     */
    List<SupportTicket> findByPriority(TicketPriority priority);

    /**
     * Find all tickets with a specific priority with pagination.
     * 
     * @param priority the ticket priority to filter by
     * @param pageable pagination information
     * @return Page of tickets with the specified priority
     */
    Page<SupportTicket> findByPriority(TicketPriority priority, Pageable pageable);

    /**
     * Find all tickets with a specific status and priority.
     * 
     * @param status the ticket status
     * @param priority the ticket priority
     * @return List of tickets matching the criteria
     */
    List<SupportTicket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    /**
     * Find all tickets assigned to a specific admin/support user.
     * 
     * @param assignedTo the name/ID of the assigned user
     * @return List of tickets assigned to the user
     */
    List<SupportTicket> findByAssignedTo(String assignedTo);

    /**
     * Find tickets by user ID and status list.
     * 
     * @param userId the ID of the user
     * @param statuses list of statuses to filter by
     * @return List of tickets matching the criteria
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId AND t.status IN :statuses")
    List<SupportTicket> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<TicketStatus> statuses);

    /**
     * Find tickets created within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return List of tickets created in the date range
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.createdDate BETWEEN :startDate AND :endDate ORDER BY t.createdDate DESC")
    List<SupportTicket> findTicketsCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count tickets by status.
     * 
     * @param status the ticket status
     * @return Number of tickets with the specified status
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);

    /**
     * Count open tickets for a specific user.
     * 
     * @param userId the ID of the user
     * @return Number of open tickets for the user
     */
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.user.id = :userId AND t.status IN ('OPEN', 'IN_PROGRESS')")
    Long countOpenTicketsByUserId(@Param("userId") Long userId);

    /**
     * Find the 10 most recently created tickets.
     * 
     * @return List of the 10 most recent tickets
     */
    List<SupportTicket> findTop10ByOrderByCreatedDateDesc();

    /**
     * Find all open tickets (status = OPEN or IN_PROGRESS).
     * 
     * @return List of open tickets ordered by priority and creation date
     */
    @Query("SELECT st FROM SupportTicket st WHERE st.status IN ('OPEN', 'IN_PROGRESS') ORDER BY st.priority DESC, st.createdDate ASC")
    List<SupportTicket> findAllOpenTickets();

    /**
     * Find all unassigned tickets.
     * 
     * @return List of unassigned tickets ordered by priority
     */
    @Query("SELECT st FROM SupportTicket st WHERE st.assignedTo IS NULL ORDER BY st.priority DESC, st.createdDate ASC")
    List<SupportTicket> findAllUnassignedTickets();

    /**
     * Find high priority open tickets.
     * 
     * @return List of high/critical priority open tickets
     */
    @Query("SELECT st FROM SupportTicket st WHERE st.priority IN ('HIGH', 'CRITICAL') AND st.status IN ('OPEN', 'IN_PROGRESS') ORDER BY st.priority DESC, st.createdDate ASC")
    List<SupportTicket> findHighPriorityOpenTickets();

}
