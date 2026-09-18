package com.codewalnut.resolvehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityAccessIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenAnotherCustomersTicket_whenCustomerReads_thenReturnTraceableForbidden() throws Exception {
        var ticket = createTicket(otherCustomer, TicketPriority.LOW, NOW);
        String trace = "93c84e42-010c-48bd-915c-b6f10d8ae2cb";
        mvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD))
                        .header("X-Trace-Id", trace))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.traceId").value(trace));
    }

    @Test
    void givenClaimedTicket_whenOtherAgentReadsOrLists_thenDenyReadAndExcludeFromList() throws Exception {
        var ticket = createTicket(customer, TicketPriority.HIGH, NOW);
        mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId()).with(csrf()).with(httpBasic(agentOne.getUsername(), PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/tickets/{id}", ticket.getId()).with(csrf()).with(httpBasic(agentTwo.getUsername(), PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/tickets").with(csrf()).with(httpBasic(agentTwo.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/tickets").with(csrf()).with(httpBasic(agentOne.getUsername(), PASSWORD))
                        .param("assignedAgentId", agentOne.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void givenAdmin_whenClaimRequested_thenDenyAgentOnlyOperation() throws Exception {
        var ticket = createTicket(customer, TicketPriority.LOW, NOW);
        mvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId()).with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenCustomer_whenCreatingForAnotherCustomer_thenDenyRequest() throws Exception {
        mvc.perform(post("/api/v1/tickets").with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(otherCustomer.getId().toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdmin_whenCreatingForEnabledCustomer_thenSetCustomerAndFixedClock() throws Exception {
        mvc.perform(post("/api/v1/tickets").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(customer.getId().toString())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.customerId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString()))
                .andExpect(jsonPath("$.updatedAt").value(NOW.toString()));
    }

    @Test
    void givenAdmin_whenCreatingForDisabledCustomer_thenReturnConflict() throws Exception {
        var disabled = createUser(ApplicationRole.CUSTOMER, false);
        mvc.perform(post("/api/v1/tickets").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(disabled.getId().toString())))
                .andExpect(status().isConflict());
    }

    @Test
    void givenMissingTicket_whenRead_thenReturnNotFound() throws Exception {
        mvc.perform(get("/api/v1/tickets/{id}", java.util.UUID.randomUUID())
                        .with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD))).andExpect(status().isNotFound());
    }

    private String createRequest(String customerId) {
        return """
                {"title":"Valid title","description":"A sufficiently detailed description",
                 "priority":"HIGH","tags":[" Java ","java"],"customerId":"%s"}
                """.formatted(customerId);
    }
}
