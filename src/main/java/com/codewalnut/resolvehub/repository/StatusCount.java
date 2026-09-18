package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.domain.TicketStatus;

public record StatusCount(TicketStatus status, long count) {
}
