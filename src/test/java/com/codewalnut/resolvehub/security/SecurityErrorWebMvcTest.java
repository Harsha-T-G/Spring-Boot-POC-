package com.codewalnut.resolvehub.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import com.codewalnut.resolvehub.config.SecurityConfig;
import com.codewalnut.resolvehub.config.TimeConfig;
import com.codewalnut.resolvehub.controller.InfoController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InfoController.class)
@Import({SecurityConfig.class, TimeConfig.class, com.codewalnut.resolvehub.exception.ApiErrorFactory.class,
        SecurityErrorWebMvcTest.IdentityConfiguration.class})
class SecurityErrorWebMvcTest {
    @Autowired MockMvc mvc;

    @TestConfiguration
    static class IdentityConfiguration {
        @Bean
        UserDetailsService users(org.springframework.security.crypto.password.PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(User.withUsername("customer")
                    .password(encoder.encode("unused-test-password")).roles("CUSTOMER").build(),
                    User.withUsername("long-password-user").password(encoder.encode("a".repeat(72)))
                            .roles("CUSTOMER").build(),
                    User.withUsername("unicode-password-user").password(encoder.encode("é".repeat(36)))
                            .roles("CUSTOMER").build());
        }
    }

    @Test
    void givenOverlongIncorrectPassword_whenAuthenticating_thenReturnUnauthorized() throws Exception {
        mvc.perform(get("/api/info").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("long-password-user", "a".repeat(72) + "incorrect-suffix")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenMaximumUtf8Password_whenAuthenticating_thenAcceptExactPasswordAndRejectSuffix() throws Exception {
        mvc.perform(get("/api/info").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("unicode-password-user", "é".repeat(36))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/info").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("unicode-password-user", "é".repeat(36) + "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAuthenticatedCaller_whenCsrfEndpointRequested_thenReturnNotFound() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/csrf")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("customer", "unused-test-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenNoCredentials_whenProtectedRouteRequested_thenReturnJsonUnauthorizedWithTrace() throws Exception {
        mvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"ResolveHub\""));
    }

    @Test
    void givenNoCredentials_whenInfoRequested_thenReturnPublicMetadata() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("resolvehub-lite"));
    }

    @Test
    void givenInvalidCredentials_whenProtectedRouteRequested_thenReturnSafeJsonUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("customer", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required or credentials invalid"));
    }

    @Test
    void givenAuthenticatedCustomer_whenInfoRequested_thenRetainUsernameForRequestLogging() throws Exception {
        var result = mvc.perform(get("/api/info").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .httpBasic("customer", "unused-test-password")))
                .andExpect(status().isOk()).andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getRequest().getAttribute("authenticatedUsername"))
                .isEqualTo("customer");
        org.assertj.core.api.Assertions.assertThat(org.slf4j.MDC.get("traceId")).isNull();
    }
}
