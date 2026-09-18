package com.codewalnut.resolvehub.controller;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import com.codewalnut.resolvehub.domain.DefaultTicketTransitionPolicy;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketTransitionPolicy;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TicketResolutionConcurrencyIntegrationTest.ConcurrentResolutionConfiguration.class)
class TicketResolutionConcurrencyIntegrationTest extends ApiScenarioTestSupport {

    @Test
    void givenTwoResolutionsReadingSameVersion_whenRequestsRace_thenOneCommitsAndOneConflicts() throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                        .with(httpBasic(agentOne.getUsername(), PASSWORD)).with(csrf()))
                .andExpect(status().isOk());
        long claimedVersion = tickets.findById(ticket.getId()).orElseThrow().getVersion();
        var summaries = List.of("The assigned agent corrected and verified the configuration.",
                "The administrator repaired and validated the customer account.");
        var actors = List.of(agentOne, admin);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var requests = List.of(0, 1).stream().map(index -> executor.submit(() ->
                    mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                                    .with(httpBasic(actors.get(index).getUsername(), PASSWORD)).with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json.createObjectNode().put("status", "RESOLVED")
                                            .put("resolutionSummary", summaries.get(index)).toString()))
                            .andReturn())).toList();
            MvcResult first = requests.get(0).get(20, TimeUnit.SECONDS);
            MvcResult second = requests.get(1).get(20, TimeUnit.SECONDS);
            assertThat(List.of(first.getResponse().getStatus(), second.getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 409);
            int winnerIndex = first.getResponse().getStatus() == 200 ? 0 : 1;
            MvcResult winner = winnerIndex == 0 ? first : second;
            MvcResult loser = winnerIndex == 0 ? second : first;
            var winnerBody = json.readTree(winner.getResponse().getContentAsString());
            assertThat(winnerBody.path("resolutionSummary").asText()).isEqualTo(summaries.get(winnerIndex));
            assertThat(winnerBody.path("resolvedAt").asText()).isEqualTo(NOW.toString());
            assertThat(json.readTree(loser.getResponse().getContentAsString()).path("status").asInt())
                    .isEqualTo(409);
            mvc.perform(get("/api/v1/tickets/{id}", ticket.getId())
                            .with(httpBasic(admin.getUsername(), PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.resolutionSummary").value(summaries.get(winnerIndex)))
                    .andExpect(jsonPath("$.resolvedAt").value(NOW.toString()))
                    .andExpect(jsonPath("$.updatedAt").value(NOW.toString()))
                    .andExpect(jsonPath("$.createdAt").value(NOW.minusSeconds(60).toString()))
                    .andExpect(jsonPath("$.assignedAgentId").value(agentOne.getId().toString()))
                    .andExpect(jsonPath("$.customerId").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.tags[0]").value("scenario"));
            assertThat(tickets.findById(ticket.getId()).orElseThrow().getVersion())
                    .isEqualTo(claimedVersion + 1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @TestConfiguration
    static class ConcurrentResolutionConfiguration {

        @Bean
        @Primary
        TicketTransitionPolicy concurrentResolutionPolicy(DefaultTicketTransitionPolicy policy) {
            var bothSnapshotsLoaded = new CyclicBarrier(2);
            return (current, requested) -> {
                policy.verifyAllowed(current, requested);
                try {
                    bothSnapshotsLoaded.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Resolution rendezvous interrupted", exception);
                } catch (Exception exception) {
                    throw new IllegalStateException("Both resolution snapshots must load before mutation", exception);
                }
            };
        }
    }
}
