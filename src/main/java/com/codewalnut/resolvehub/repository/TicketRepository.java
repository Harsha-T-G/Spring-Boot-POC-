package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {

    @Override
    @EntityGraph(attributePaths = {"tags", "customer", "assignedAgent"})
    Optional<TicketEntity> findById(UUID id);

    Page<TicketEntity> findAllByStatusAndPriority(
            TicketStatus status,
            TicketPriority priority,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TicketEntity ticket
               set ticket.assignedAgent = :agent,
                   ticket.status = com.codewalnut.resolvehub.domain.TicketStatus.IN_PROGRESS,
                   ticket.updatedAt = :updatedAt,
                   ticket.version = ticket.version + 1
             where ticket.id = :ticketId
               and ticket.status = com.codewalnut.resolvehub.domain.TicketStatus.OPEN
               and ticket.assignedAgent is null
            """)
    int claimOpenTicket(
            @Param("ticketId") UUID ticketId,
            @Param("agent") AppUserEntity agent,
            @Param("updatedAt") Instant updatedAt);

    long countByAssignedAgentIsNull();

    @Query("select new com.codewalnut.resolvehub.repository.StatusCount(t.status, count(t)) from TicketEntity t group by t.status")
    List<StatusCount> countGroupedByStatus();

    @Query("select new com.codewalnut.resolvehub.repository.PriorityCount(t.priority, count(t)) from TicketEntity t group by t.priority")
    List<PriorityCount> countGroupedByPriority();

    List<TicketEntity> findTop3ByOrderByCreatedAtDescIdAsc();
}
