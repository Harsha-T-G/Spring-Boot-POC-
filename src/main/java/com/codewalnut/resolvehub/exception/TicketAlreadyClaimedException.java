package com.codewalnut.resolvehub.exception;

import java.util.UUID;

public class TicketAlreadyClaimedException extends RuntimeException {

    public TicketAlreadyClaimedException(UUID ticketId) {
        super("Ticket " + ticketId + " is already claimed");
    }
}
