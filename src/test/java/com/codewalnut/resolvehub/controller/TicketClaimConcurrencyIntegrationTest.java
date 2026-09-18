package com.codewalnut.resolvehub.controller;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketClaimConcurrencyIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenTwoAgents_whenClaimRequestsRace_thenExactlyOneSucceedsAndOneConflicts() throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(agentOne, agentTwo).stream().map(agent -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Claim start timed out");
                }
                return mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                                .with(csrf()).with(httpBasic(agent.getUsername(), PASSWORD)))
                        .andReturn();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var first = results.get(0).get(20, TimeUnit.SECONDS);
            var second = results.get(1).get(20, TimeUnit.SECONDS);
            assertThat(List.of(first.getResponse().getStatus(), second.getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 409);
            var winner = first.getResponse().getStatus() == 200 ? agentOne : agentTwo;
            mvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.assignedAgentId").value(winner.getId().toString()))
                    .andExpect(jsonPath("$.updatedAt").value(NOW.toString()))
                    .andExpect(jsonPath("$.resolvedAt").isEmpty());
        } finally {
            start.countDown();
        }
    }
}
