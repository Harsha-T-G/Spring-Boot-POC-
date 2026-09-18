package com.codewalnut.resolvehub.dto;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String referenceNumber,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        UUID customerId,
        UUID assignedAgentId,
        Set<String> tags,
        String resolutionSummary,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt) {

    public TicketResponse {
        tags = Set.copyOf(tags);
    }
}
