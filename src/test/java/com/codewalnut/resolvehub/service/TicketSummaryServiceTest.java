package com.codewalnut.resolvehub.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.TicketRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TicketSummaryServiceTest {
    @Test
    void givenEmptyDatabase_whenSummarized_thenReturnZeroFilledDefensiveMaps() {
        var repository = mock(TicketRepository.class);
        var result = new TicketSummaryService(repository, new TicketMapper()).summarize();

        assertThat(result.total()).isZero();
        assertThat(result.unassigned()).isZero();
        assertThat(result.recentTickets()).isEmpty();
        assertThat(result.byStatus()).containsEntry(TicketStatus.OPEN, 0L)
                .containsEntry(TicketStatus.IN_PROGRESS, 0L).containsEntry(TicketStatus.RESOLVED, 0L);
        assertThat(result.byPriority()).hasSize(4).containsEntry(TicketPriority.CRITICAL, 0L);
        result.byStatus().put(TicketStatus.OPEN, 42L);
        assertThat(result.byStatus()).containsEntry(TicketStatus.OPEN, 0L);
    }
}
