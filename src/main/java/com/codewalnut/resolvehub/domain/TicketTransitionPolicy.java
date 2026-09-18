package com.codewalnut.resolvehub.domain;

public interface TicketTransitionPolicy {

    void verifyAllowed(TicketStatus currentStatus, TicketStatus requestedStatus);
}
