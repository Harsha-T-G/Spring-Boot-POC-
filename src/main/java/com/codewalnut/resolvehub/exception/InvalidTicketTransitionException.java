package com.codewalnut.resolvehub.exception;

import com.codewalnut.resolvehub.domain.TicketStatus;

public class InvalidTicketTransitionException extends RuntimeException {

    public InvalidTicketTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus) {
        super("Ticket transition from " + currentStatus + " to " + requestedStatus + " is not allowed");
    }
}
