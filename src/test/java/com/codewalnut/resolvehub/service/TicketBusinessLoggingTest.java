package com.codewalnut.resolvehub.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import com.codewalnut.resolvehub.domain.*;
import com.codewalnut.resolvehub.dto.CreateTicketRequest;
import com.codewalnut.resolvehub.entity.*;
import com.codewalnut.resolvehub.exception.TicketAlreadyClaimedException;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class TicketBusinessLoggingTest {
    @Test
    void givenCustomer_whenTicketCreated_thenLogIdentifierWithoutBodyOrHash(CapturedOutput output) {
        var users = mock(AppUserRepository.class);
        var tickets = mock(TicketRepository.class);
        var customer = new AppUserEntity(UUID.randomUUID(), "customer", "PRIVATE-HASH", true, Instant.EPOCH,
                Set.of(new RoleEntity(UUID.randomUUID(), ApplicationRole.CUSTOMER)));
        when(users.findByUsernameIgnoreCase("customer")).thenReturn(Optional.of(customer));
        when(tickets.save(any(TicketEntity.class))).thenAnswer(call -> call.getArgument(0));
        var service = new TicketService(tickets, users, new UuidReferenceNumberGenerator(), new TagNormalizer(),
                new TicketMapper(), Clock.systemUTC(), new TicketPagePolicy(20, 100));
        var result = service.create(new CreateTicketRequest("Private title", "Private description that must not be logged",
                TicketPriority.HIGH, null, null), "customer");

        assertThat(output).contains("ticket_created", result.id().toString())
                .doesNotContain("PRIVATE-HASH", "Private title", "Private description");
    }

    @Test
    void givenAlreadyClaimedTicket_whenClaimRejected_thenLogConflictIdentifier(CapturedOutput output) {
        var users = mock(AppUserRepository.class);
        var tickets = mock(TicketRepository.class);
        var agent = new AppUserEntity(UUID.randomUUID(), "agent", "PRIVATE-HASH", true, Instant.EPOCH, Set.of());
        when(users.findByUsernameIgnoreCase("agent")).thenReturn(Optional.of(agent));
        UUID id = UUID.randomUUID();
        when(tickets.existsById(id)).thenReturn(true);
        var service = new TicketClaimService(tickets, users, new TicketMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.claim(id, "agent")).isInstanceOf(TicketAlreadyClaimedException.class);
        assertThat(output).contains("ticket_claim_rejected", id.toString()).doesNotContain("PRIVATE-HASH");
    }
}
