package com.codewalnut.resolvehub.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.codewalnut.resolvehub.domain.*;
import com.codewalnut.resolvehub.dto.UpdateTicketStatusRequest;
import com.codewalnut.resolvehub.entity.*;
import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TicketResolutionServiceTest {

    @Test
    void givenOpenTicketAndAdmin_whenPatchRequestsInProgress_thenRejectWithoutChangingTicket() {
        Instant now = Instant.parse("2026-09-12T00:00:00Z");
        AppUserEntity admin = new AppUserEntity(UUID.randomUUID(), "admin", "hash", true, now,
                Set.of(new RoleEntity(UUID.randomUUID(), ApplicationRole.ADMIN)));
        TicketEntity ticket = new TicketEntity(UUID.randomUUID(), "RH-TEST", "Valid title",
                "A description long enough", TicketPriority.HIGH, admin, Set.of(), now);
        TicketRepository tickets = mock(TicketRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tickets.saveAndFlush(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TicketResolutionService service = new TicketResolutionService(tickets, users,
                new DefaultTicketTransitionPolicy(), new TicketMapper(), Clock.fixed(now, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.resolve(ticket.getId(), new UpdateTicketStatusRequest(
                TicketStatus.IN_PROGRESS, "A sufficiently long summary"), "admin"))
                .isInstanceOf(InvalidTicketTransitionException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getResolvedAt()).isNull();
    }
}
