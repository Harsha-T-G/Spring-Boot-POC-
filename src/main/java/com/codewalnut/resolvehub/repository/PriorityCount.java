package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.domain.TicketPriority;

public record PriorityCount(TicketPriority priority, long count) {
}
