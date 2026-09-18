package com.codewalnut.resolvehub.dto;

import java.util.EnumMap;
import java.util.List;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;

public record TicketSummaryResponse(long total, EnumMap<TicketStatus, Long> byStatus,
        EnumMap<TicketPriority, Long> byPriority, long unassigned, List<TicketResponse> recentTickets) {
    public TicketSummaryResponse {
        byStatus = new EnumMap<>(byStatus);
        byPriority = new EnumMap<>(byPriority);
        recentTickets = List.copyOf(recentTickets);
    }

    @Override
    public EnumMap<TicketStatus, Long> byStatus() {
        return new EnumMap<>(byStatus);
    }

    @Override
    public EnumMap<TicketPriority, Long> byPriority() {
        return new EnumMap<>(byPriority);
    }
}
