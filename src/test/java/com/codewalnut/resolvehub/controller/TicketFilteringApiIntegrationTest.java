package com.codewalnut.resolvehub.controller;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TicketFilteringApiIntegrationTest extends ApiScenarioTestSupport {

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void givenMatchingTicketsInDifferentStates_whenFilteringStatus_thenReturnOnlyRequestedState(TicketStatus requested) throws Exception {
        var open = createTicket(customer, TicketPriority.HIGH, NOW);
        var inProgress = createTicket(customer, TicketPriority.HIGH, NOW);
        var resolved = createTicket(customer, TicketPriority.HIGH, NOW);
        tickets.claimOpenTicket(inProgress.getId(), agentOne, NOW);
        tickets.claimOpenTicket(resolved.getId(), agentOne, NOW);
        var toResolve = tickets.findById(resolved.getId()).orElseThrow();
        toResolve.resolve("The support issue has been fully resolved", NOW);
        tickets.saveAndFlush(toResolve);
        Map<TicketStatus, UUID> expectedIds = Map.of(TicketStatus.OPEN, open.getId(),
                TicketStatus.IN_PROGRESS, inProgress.getId(), TicketStatus.RESOLVED, resolved.getId());

        mvc.perform(get("/api/v1/tickets").with(httpBasic(customer.getUsername(), PASSWORD))
                        .param("status", requested.name()).param("priority", "HIGH").param("search", "Scenario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].id", contains(expectedIds.get(requested).toString())));
    }

    @Test
    void givenTicketsForDifferentAgentsAndUnassigned_whenAdminFiltersAssignee_thenReturnOnlyMatchingIds() throws Exception {
        var first = createTicket(customer, TicketPriority.HIGH, NOW);
        var second = createTicket(customer, TicketPriority.HIGH, NOW);
        createTicket(customer, TicketPriority.HIGH, NOW);
        tickets.claimOpenTicket(first.getId(), agentOne, NOW);
        tickets.claimOpenTicket(second.getId(), agentTwo, NOW);

        for (var assignment : Map.of(agentOne.getId(), first.getId(), agentTwo.getId(), second.getId()).entrySet()) {
            mvc.perform(get("/api/v1/tickets").with(httpBasic(admin.getUsername(), PASSWORD))
                            .param("assignedAgentId", assignment.getKey().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[*].id", contains(assignment.getValue().toString())));
        }
        mvc.perform(get("/api/v1/tickets").with(httpBasic(admin.getUsername(), PASSWORD))
                        .param("assignedAgentId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }
}
