package com.codewalnut.resolvehub.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;
import static org.assertj.core.api.Assertions.*;

class TicketEntityTest {
    @ParameterizedTest
    @ValueSource(ints = {19, 501})
    void givenInvalidUnicodeSummary_whenResolved_thenRejectWithoutMutation(int length) {
        var ticket = ticket();
        ReflectionTestUtils.setField(ticket, "status", TicketStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(ticket, "assignedAgent", ticket.getCustomer());
        assertThatThrownBy(() -> ticket.resolve("😀".repeat(length), Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolutionSummary()).isNull();
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 500})
    void givenValidUnicodeSummary_whenResolved_thenPreserveSummary(int length) {
        var ticket = ticket();
        ReflectionTestUtils.setField(ticket, "status", TicketStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(ticket, "assignedAgent", ticket.getCustomer());
        String summary = "😀".repeat(length);
        ticket.resolve(summary, Instant.EPOCH);
        assertThat(ticket.getResolutionSummary()).isEqualTo(summary);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void givenOpenTicket_whenResolvedDirectly_thenRejectInvalidState() {
        var ticket = ticket();
        assertThatThrownBy(() -> ticket.resolve("A sufficiently long resolution", Instant.EPOCH))
                .isInstanceOf(InvalidTicketTransitionException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void givenClaimedTicket_whenSummaryTooShort_thenRejectWithoutMutation() {
        var ticket = ticket();
        ReflectionTestUtils.setField(ticket, "status", TicketStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(ticket, "assignedAgent", ticket.getCustomer());
        assertThatThrownBy(() -> ticket.resolve(" short ", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolvedAt()).isNull();
    }

    @Test
    void givenClaimedTicket_whenResolved_thenNormalizeSummaryAndSetBothTimestamps() {
        var ticket = ticket();
        ReflectionTestUtils.setField(ticket, "status", TicketStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(ticket, "assignedAgent", ticket.getCustomer());
        Instant resolved = Instant.parse("2026-09-12T00:00:00Z");
        ticket.resolve("  A sufficiently long resolution  ", resolved);
        assertThat(ticket.getResolutionSummary()).isEqualTo("A sufficiently long resolution");
        assertThat(ticket.getResolvedAt()).isEqualTo(resolved);
        assertThat(ticket.getUpdatedAt()).isEqualTo(resolved);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    private TicketEntity ticket() {
        var customer = new AppUserEntity(UUID.randomUUID(), "customer", "hash", true, Instant.EPOCH, Set.of());
        return new TicketEntity(UUID.randomUUID(), "RH-TEST", "Valid title", "A sufficiently long description",
                TicketPriority.HIGH, customer, Set.of(), Instant.EPOCH);
    }
}
