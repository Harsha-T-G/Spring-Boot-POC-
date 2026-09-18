package com.codewalnut.resolvehub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import com.codewalnut.resolvehub.domain.ReferenceNumberGenerator;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TicketCreationRollbackIntegrationTest.DuplicateReference.class)
class TicketCreationRollbackIntegrationTest extends ApiScenarioTestSupport {
    @TestConfiguration
    static class DuplicateReference {
        @Bean @Primary
        ReferenceNumberGenerator duplicateReference() {
            return () -> "RH-DETERMINISTIC-DUPLICATE";
        }
    }

    @Test
    void givenDuplicateGeneratedReference_whenSecondTicketCreated_thenConflictAndRollbackTicketAndTags() throws Exception {
        String body = """
                {"title":"Valid title","description":"A sufficiently detailed description",
                 "priority":"HIGH","tags":["atomic","rollback"]}
                """;
        mvc.perform(post("/api/v1/tickets").with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/tickets").with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Request conflicts with existing data"));
        mvc.perform(get("/api/v1/tickets").with(csrf()).with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tags.length()").value(2));
    }
}
