package com.codewalnut.resolvehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CsrfSecurityIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenAuthenticatedClient_whenCookieAndTokenReturnedWithWrite_thenCreateWithoutSession() throws Exception {
        var tokenResult = mvc.perform(get("/api/csrf").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andReturn();
        var token = json.readTree(tokenResult.getResponse().getContentAsString());
        var cookie = tokenResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(tokenResult.getRequest().getSession(false)).isNull();

        mvc.perform(post("/api/v1/tickets").with(httpBasic(customer.getUsername(), PASSWORD))
                        .cookie(cookie).header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isCreated());
    }

    @Test
    void givenAuthenticatedClient_whenTokenMissingOrInvalid_thenReturnSafeForbidden() throws Exception {
        for (String token : java.util.List.of("", "invalid-token")) {
            mvc.perform(post("/api/v1/tickets").with(httpBasic(customer.getUsername(), PASSWORD))
                            .header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON).content(request()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Access denied"))
                    .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
        assertThat(tickets.count()).isZero();
    }

    @Test
    void givenMissingOrInvalidCredentials_whenPostingOrFetchingToken_thenReturnUnauthorized() throws Exception {
        mvc.perform(get("/api/csrf")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/tickets").with(httpBasic(customer.getUsername(), "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    private String request() {
        return """
                {"title":"Valid title","description":"A sufficiently detailed ticket description","priority":"HIGH"}
                """;
    }
}
