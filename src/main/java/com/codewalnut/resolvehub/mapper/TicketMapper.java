package com.codewalnut.resolvehub.mapper;

import com.codewalnut.resolvehub.dto.TicketResponse;
import com.codewalnut.resolvehub.entity.TicketEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TicketMapper {

    public TicketResponse toResponse(TicketEntity ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReferenceNumber(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCustomer().getId(),
                Optional.ofNullable(ticket.getAssignedAgent()).map(agent -> agent.getId()).orElse(null),
                ticket.getTags(),
                ticket.getResolutionSummary(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt());
    }
}
