package com.codewalnut.resolvehub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketSummaryReportIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenEmptyDatabase_whenAdminRequestsSummary_thenReturnZeroFilledCounts() throws Exception {
        mvc.perform(get("/api/v1/reports/summary").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.byStatus.OPEN").value(0))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(0))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(0))
                .andExpect(jsonPath("$.recentTickets").isEmpty());
    }

    @Test
    void givenMixedTickets_whenAdminRequestsSummary_thenAggregateAndLimitRecentTickets() throws Exception {
        createTicket(customer, TicketPriority.LOW, NOW.minusSeconds(4));
        var claimed = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(3));
        var resolved = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(2));
        var newest = createTicket(customer, TicketPriority.CRITICAL, NOW.minusSeconds(1));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            tickets.claimOpenTicket(claimed.getId(), agentOne, NOW);
            tickets.claimOpenTicket(resolved.getId(), agentTwo, NOW);
            var ticket = tickets.findById(resolved.getId()).orElseThrow();
            ticket.resolve("A sufficiently detailed resolution", NOW);
            tickets.saveAndFlush(ticket);
        });

        mvc.perform(get("/api/v1/reports/summary").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.unassigned").value(2))
                .andExpect(jsonPath("$.byStatus.OPEN").value(2))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(1))
                .andExpect(jsonPath("$.byPriority.HIGH").value(2))
                .andExpect(jsonPath("$.byPriority.MEDIUM").value(0))
                .andExpect(jsonPath("$.recentTickets.length()").value(3))
                .andExpect(jsonPath("$.recentTickets[0].id").value(newest.getId().toString()))
                .andExpect(jsonPath("$.recentTickets[1].id").value(resolved.getId().toString()))
                .andExpect(jsonPath("$.recentTickets[2].id").value(claimed.getId().toString()));
    }

    @Test
    void givenNonAdminUsers_whenSummaryRequested_thenDenyAccess() throws Exception {
        for (var actor : java.util.List.of(customer, agentOne)) {
            mvc.perform(get("/api/v1/reports/summary").with(csrf()).with(httpBasic(actor.getUsername(), PASSWORD)))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        }
        mvc.perform(get("/api/v1/reports/summary")).andExpect(status().isUnauthorized());
    }
}
