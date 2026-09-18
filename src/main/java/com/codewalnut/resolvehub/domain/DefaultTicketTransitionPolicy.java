package com.codewalnut.resolvehub.domain;

import com.codewalnut.resolvehub.exception.InvalidTicketTransitionException;
import org.springframework.stereotype.Component;

@Component
public class DefaultTicketTransitionPolicy implements TicketTransitionPolicy {

    @Override
    public void verifyAllowed(TicketStatus currentStatus, TicketStatus requestedStatus) {
        boolean allowed = currentStatus == TicketStatus.OPEN && requestedStatus == TicketStatus.IN_PROGRESS
                || currentStatus == TicketStatus.IN_PROGRESS && requestedStatus == TicketStatus.RESOLVED;
        if (!allowed) {
            throw new InvalidTicketTransitionException(currentStatus, requestedStatus);
        }
    }
}
