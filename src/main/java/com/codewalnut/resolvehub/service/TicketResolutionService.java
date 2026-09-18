package com.codewalnut.resolvehub.service;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.TicketTransitionPolicy;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;
import com.codewalnut.resolvehub.dto.TicketResponse;
import com.codewalnut.resolvehub.dto.UpdateTicketStatusRequest;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.exception.TicketAccessDeniedException;
import com.codewalnut.resolvehub.exception.TicketNotFoundException;
import com.codewalnut.resolvehub.exception.UserNotFoundException;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import com.codewalnut.resolvehub.exception.ConcurrentTicketUpdateException;

@Service
public class TicketResolutionService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketResolutionService.class);

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketTransitionPolicy transitionPolicy;
    private final TicketMapper ticketMapper;
    private final Clock clock;

    public TicketResolutionService(
            TicketRepository ticketRepository,
            AppUserRepository appUserRepository,
            TicketTransitionPolicy transitionPolicy,
            TicketMapper ticketMapper,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.transitionPolicy = transitionPolicy;
        this.ticketMapper = ticketMapper;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','ADMIN')")
    public TicketResponse resolve(
            UUID ticketId,
            UpdateTicketStatusRequest request,
            String authenticatedUsername) {
        AppUserEntity actor = appUserRepository.findByUsernameIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new UserNotFoundException(authenticatedUsername));
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (!hasRole(actor, ApplicationRole.ADMIN) && ticket.getAssignedAgent() != null
                && !ticket.getAssignedAgent().getId().equals(actor.getId())) {
            throw new TicketAccessDeniedException();
        }
        if (request.status() != TicketStatus.RESOLVED) {
            throw new InvalidTicketTransitionException(ticket.getStatus(), request.status());
        }
        transitionPolicy.verifyAllowed(ticket.getStatus(), request.status());
        if (!hasRole(actor, ApplicationRole.ADMIN)
                && (ticket.getAssignedAgent() == null
                || !ticket.getAssignedAgent().getId().equals(actor.getId()))) {
            throw new TicketAccessDeniedException();
        }
        ticket.resolve(request.resolutionSummary().trim(), clock.instant());
        try {
            TicketResponse response = ticketMapper.toResponse(ticketRepository.saveAndFlush(ticket));
            LOG.info("ticket_resolved ticketId={} actorId={}", ticketId, actor.getId());
            return response;
        } catch (OptimisticLockingFailureException exception) {
            throw new ConcurrentTicketUpdateException(exception);
        }
    }

    private boolean hasRole(AppUserEntity user, ApplicationRole role) {
        return user.getRoles().stream().map(RoleEntity::getName).anyMatch(role::equals);
    }
}
