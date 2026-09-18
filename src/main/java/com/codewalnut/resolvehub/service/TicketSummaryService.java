package com.codewalnut.resolvehub.service;

import java.util.EnumMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.dto.TicketSummaryResponse;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.TicketRepository;

@Service
public class TicketSummaryService {
    private final TicketRepository repository;
    private final TicketMapper mapper;

    public TicketSummaryService(TicketRepository repository, TicketMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    @PreAuthorize("hasRole('ADMIN')")
    public TicketSummaryResponse summarize() {
        var byStatus = new EnumMap<TicketStatus, Long>(TicketStatus.class);
        var byPriority = new EnumMap<TicketPriority, Long>(TicketPriority.class);
        for (var status : TicketStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (var priority : TicketPriority.values()) {
            byPriority.put(priority, 0L);
        }
        repository.countGroupedByStatus().forEach(row -> byStatus.put(row.status(), row.count()));
        repository.countGroupedByPriority().forEach(row -> byPriority.put(row.priority(), row.count()));
        return new TicketSummaryResponse(repository.count(), byStatus, byPriority,
                repository.countByAssignedAgentIsNull(),
                repository.findTop3ByOrderByCreatedAtDescIdAsc().stream().map(mapper::toResponse).toList());
    }
}
