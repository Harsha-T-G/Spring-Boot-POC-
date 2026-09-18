package com.codewalnut.resolvehub.service;

import com.codewalnut.resolvehub.dto.TicketResponse;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.exception.TicketAlreadyClaimedException;
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

@Service
public class TicketClaimService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketClaimService.class);

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final TicketMapper ticketMapper;
    private final Clock clock;

    public TicketClaimService(
            TicketRepository ticketRepository,
            AppUserRepository appUserRepository,
            TicketMapper ticketMapper,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.ticketMapper = ticketMapper;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public TicketResponse claim(UUID ticketId, String authenticatedUsername) {
        AppUserEntity agent = appUserRepository.findByUsernameIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new UserNotFoundException(authenticatedUsername));
        int updatedRows = ticketRepository.claimOpenTicket(ticketId, agent, clock.instant());
        if (updatedRows == 0) {
            if (!ticketRepository.existsById(ticketId)) {
                throw new TicketNotFoundException(ticketId);
            }
            LOG.info("ticket_claim_rejected ticketId={} agentId={}", ticketId, agent.getId());
            throw new TicketAlreadyClaimedException(ticketId);
        }
        TicketEntity claimedTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        LOG.info("ticket_claimed ticketId={} agentId={}", ticketId, agent.getId());
        return ticketMapper.toResponse(claimedTicket);
    }
}
