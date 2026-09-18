package com.codewalnut.resolvehub.config;

import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDisabledIntegrationTest extends ApiScenarioTestSupport {

    @Test
    void givenNonDevelopmentProfile_whenDocumentationRequested_thenOpenApiIsNotRegistered() throws Exception {
        mvc.perform(get("/v3/api-docs").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/swagger-ui.html").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isNotFound());
    }
}
