package com.codewalnut.resolvehub.config;

import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
class OpenApiIntegrationTest extends ApiScenarioTestSupport {

    @Test
    void givenAuthenticatedUser_whenDefinitionRequested_thenDocumentApiWithoutAuthorizationControls() throws Exception {
        mvc.perform(get("/v3/api-docs").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("ResolveHub Lite API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0-SNAPSHOT"))
                .andExpect(jsonPath("$.paths['/api/csrf']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/tickets']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tickets/{id}/claim']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/reports/summary']").exists())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/users']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/tickets'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/tickets'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/tickets'].post.responses['201']").exists());
    }

    @Test
    void givenOpenApiEnabled_whenSwaggerPageRequested_thenRedirectToInteractiveUi() throws Exception {
        mvc.perform(get("/swagger-ui.html").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
