package com.codewalnut.resolvehub.repository;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.assertj.core.api.Assertions.*;

class TicketOptimisticLockRepositoryTest extends ApiScenarioTestSupport {
    @Test
    void givenTwoSnapshots_whenSecondResolutionIsSaved_thenRejectStaleVersionAndKeepFirstSummary() {
        var created = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        var transaction = new TransactionTemplate(transactions);
        long initialVersion = transaction.execute(status -> tickets.findById(created.getId()).orElseThrow().getVersion());
        transaction.executeWithoutResult(status -> tickets.claimOpenTicket(created.getId(), agentOne, NOW));
        var first = transaction.execute(status -> tickets.findById(created.getId()).orElseThrow());
        var second = transaction.execute(status -> tickets.findById(created.getId()).orElseThrow());
        assertThat(first.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(second.getVersion()).isEqualTo(first.getVersion());
        long claimedVersion = first.getVersion();
        first.resolve("The first valid resolution summary", NOW);
        second.resolve("The second valid resolution summary", NOW);

        transaction.executeWithoutResult(status -> tickets.saveAndFlush(first));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> tickets.saveAndFlush(second)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        var persisted = transaction.execute(status -> tickets.findById(created.getId()).orElseThrow());
        assertThat(persisted.getResolutionSummary()).isEqualTo("The first valid resolution summary");
        assertThat(persisted.getVersion()).isEqualTo(claimedVersion + 1);
    }
}
