package com.codewalnut.resolvehub.security;

import org.junit.jupiter.api.Test;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActuatorSecurityIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenRunningPostgres_whenHealthRequested_thenReturnPublicUpWithoutDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void givenActuatorInfo_whenAnonymousThenAuthenticated_thenRequireAuthentication() throws Exception {
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))).andExpect(status().isOk());
        mvc.perform(get("/actuator/env").with(csrf()).with(httpBasic(admin.getUsername(), PASSWORD))).andExpect(status().isNotFound());
    }
}
