package com.codewalnut.resolvehub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketResolutionRollbackIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenResolvedTicketOwnedByAnotherAgent_whenResolutionRequested_thenReturnForbiddenWithoutStateDisclosure()
            throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId()).with(csrf()).with(httpBasic(agentOne.getUsername(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(agentOne.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(resolution())).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(agentTwo.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(resolution()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void givenOpenTicket_whenInvalidResolutionRequested_thenKeepAllStateUnchanged() throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))
                        .header("X-Trace-Id", "24ff716d-e726-4bfd-87d9-05c2f45f2e41")
                        .contentType(MediaType.APPLICATION_JSON).content(resolution()))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedAgentId").isEmpty())
                .andExpect(jsonPath("$.resolvedAt").isEmpty()).andExpect(jsonPath("$.resolutionSummary").isEmpty())
                .andExpect(jsonPath("$.updatedAt").value(NOW.minusSeconds(60).toString()));
    }

    @Test
    void givenClaimedTicket_whenOtherAgentResolves_thenRejectAndAdminCanStillResolve() throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW.minusSeconds(60));
        mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId()).with(csrf()).with(httpBasic(agentOne.getUsername(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(agentTwo.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(resolution())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resolutionSummary").isEmpty()).andExpect(jsonPath("$.resolvedAt").isEmpty());
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(resolution()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").value(NOW.toString()))
                .andExpect(jsonPath("$.assignedAgentId").value(agentOne.getId().toString()));
        mvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(resolution())).andExpect(status().isConflict());
    }

    private String resolution() {
        return """
                {"status":"RESOLVED","resolutionSummary":"The configuration was corrected and verified."}
                """;
    }
}
