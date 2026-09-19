package com.codewalnut.resolvehub.service;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.ReferenceNumberGenerator;
import com.codewalnut.resolvehub.domain.TagNormalizer;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.dto.TicketPageResponse;
import com.codewalnut.resolvehub.repository.TicketSpecifications;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codewalnut.resolvehub.dto.CreateTicketRequest;
import com.codewalnut.resolvehub.dto.TicketResponse;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.exception.DisabledUserException;
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
import java.time.Instant;
import java.util.UUID;

@Service
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final TagNormalizer tagNormalizer;
    private final TicketMapper ticketMapper;
    private final Clock clock;
    private final TicketPagePolicy pagePolicy;

    public TicketService(
            TicketRepository ticketRepository,
            AppUserRepository appUserRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            TagNormalizer tagNormalizer,
            TicketMapper ticketMapper,
            Clock clock,
            TicketPagePolicy pagePolicy) {
        this.ticketRepository = ticketRepository;
        this.appUserRepository = appUserRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.tagNormalizer = tagNormalizer;
        this.ticketMapper = ticketMapper;
        this.clock = clock;
        this.pagePolicy = pagePolicy;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public TicketPageResponse list(
            TicketStatus status, TicketPriority priority,
            UUID assignedAgentId, String search, int page, Integer size, String sort, String username) {
        var requestedPage = pagePolicy.create(page, size, sort);
        var specification = TicketSpecifications.visibleAndFiltered(
                findUser(username), status, priority, assignedAgentId, search, requestedPage.getSort());
        var result = ticketRepository.findAll(specification,
                PageRequest.of(requestedPage.getPageNumber(), requestedPage.getPageSize()));
        return new TicketPageResponse(
                result.getContent().stream().map(ticketMapper::toResponse).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public TicketResponse create(CreateTicketRequest request, String authenticatedUsername) {
        AppUserEntity actor = findUser(authenticatedUsername);
        AppUserEntity customer = selectCustomer(request, actor);
        Instant createdAt = clock.instant();
        TicketEntity ticket = new TicketEntity(
                UUID.randomUUID(),
                referenceNumberGenerator.generate(),
                request.title().trim(),
                request.description().trim(),
                request.priority(),
                customer,
                tagNormalizer.normalize(request.tags()),
                createdAt);
        TicketResponse response = ticketMapper.toResponse(ticketRepository.save(ticket));
        LOG.info("ticket_created ticketId={} customerId={}", response.id(), customer.getId());
        return response;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public TicketResponse get(UUID ticketId, String authenticatedUsername) {
        AppUserEntity actor = findUser(authenticatedUsername);
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        verifyVisible(ticket, actor);
        return ticketMapper.toResponse(ticket);
    }

    private AppUserEntity selectCustomer(CreateTicketRequest request, AppUserEntity actor) {
        if (hasRole(actor, ApplicationRole.ADMIN)) {
            if (request.customerId() == null) {
                throw new IllegalArgumentException("customerId is required for ADMIN ticket creation");
            }
            AppUserEntity customer = appUserRepository.findById(request.customerId())
                    .orElseThrow(() -> new UserNotFoundException(request.customerId().toString()));
            if (!hasRole(customer, ApplicationRole.CUSTOMER)) {
                throw new UserNotFoundException(request.customerId().toString());
            }
            if (!customer.isEnabled()) {
                throw new DisabledUserException();
            }
            return customer;
        }
        if (request.customerId() != null && !request.customerId().equals(actor.getId())) {
            throw new TicketAccessDeniedException();
        }
        return actor;
    }

    private void verifyVisible(TicketEntity ticket, AppUserEntity actor) {
        if (hasRole(actor, ApplicationRole.ADMIN)) {
            return;
        }
        if (hasRole(actor, ApplicationRole.CUSTOMER) && ticket.getCustomer().getId().equals(actor.getId())) {
            return;
        }
        if (hasRole(actor, ApplicationRole.SUPPORT_AGENT)
                && (ticket.getStatus() == TicketStatus.OPEN
                || ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getId().equals(actor.getId()))) {
            return;
        }
        throw new TicketAccessDeniedException();
    }

    private AppUserEntity findUser(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private boolean hasRole(AppUserEntity user, ApplicationRole role) {
        return user.getRoles().stream().map(RoleEntity::getName).anyMatch(role::equals);
    }
}
