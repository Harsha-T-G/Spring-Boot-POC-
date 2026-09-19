package com.codewalnut.resolvehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class StatelessBasicSecurityIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenAuthenticatedClient_whenWritingWithoutCsrf_thenCreateWithoutSession() throws Exception {
        var result = mvc.perform(post("/api/v1/tickets").with(httpBasic(customer.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void givenAuthenticatedClient_whenCsrfOrUserManagementRequested_thenReturnNotFound() throws Exception {
        mvc.perform(get("/api/csrf").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/users").with(httpBasic(admin.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"extra-user","password":"unused-password","roles":["CUSTOMER"]}
                                """))
                .andExpect(status().isNotFound());
        assertThat(tickets.count()).isZero();
    }

    @Test
    void givenMissingOrInvalidCredentials_whenPosting_thenReturnUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tickets").with(httpBasic(customer.getUsername(), "wrong-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
    }

    private String request() {
        return """
                {"title":"Valid title","description":"A sufficiently detailed ticket description","priority":"HIGH"}
                """;
    }
}
