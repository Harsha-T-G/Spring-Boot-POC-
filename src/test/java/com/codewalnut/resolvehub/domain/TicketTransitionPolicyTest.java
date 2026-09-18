package com.codewalnut.resolvehub.domain;

import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTransitionPolicyTest {

    private final TicketTransitionPolicy transitionPolicy = new DefaultTicketTransitionPolicy();

    @Test
    void givenOpenTicket_whenTransitionToInProgressIsRequested_thenAllowsTransition() {
        assertDoesNotThrow(() -> transitionPolicy.verifyAllowed(TicketStatus.OPEN, TicketStatus.IN_PROGRESS));
    }

    @Test
    void givenInProgressTicket_whenTransitionToResolvedIsRequested_thenAllowsTransition() {
        assertDoesNotThrow(() -> transitionPolicy.verifyAllowed(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED));
    }

    @Test
    void givenOpenTicket_whenTransitionToResolvedIsRequested_thenRejectsTransition() {
        InvalidTicketTransitionException exception = assertThrows(
                InvalidTicketTransitionException.class,
                () -> transitionPolicy.verifyAllowed(TicketStatus.OPEN, TicketStatus.RESOLVED));

        assertEquals("Ticket transition from OPEN to RESOLVED is not allowed", exception.getMessage());
    }

    @Test
    void givenResolvedTicket_whenTransitionToInProgressIsRequested_thenRejectsTransition() {
        assertThrows(
                InvalidTicketTransitionException.class,
                () -> transitionPolicy.verifyAllowed(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS));
    }
}
