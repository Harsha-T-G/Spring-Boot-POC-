package com.codewalnut.resolvehub.exception;

public class ConcurrentTicketUpdateException extends RuntimeException {
    public ConcurrentTicketUpdateException(Throwable cause) {
        super("Ticket was changed by another request; reload and retry", cause);
    }
}
