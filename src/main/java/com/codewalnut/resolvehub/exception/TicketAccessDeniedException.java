package com.codewalnut.resolvehub.exception;

public class TicketAccessDeniedException extends RuntimeException {

    public TicketAccessDeniedException() {
        super("You do not have access to this ticket");
    }
}
