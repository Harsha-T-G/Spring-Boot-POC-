package com.codewalnut.resolvehub.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SwaggerBasicAuthenticationIntegrationTest extends ApiScenarioTestSupport {
    @Test
    void givenAnonymousBrowser_whenSwaggerRequested_thenChallengeWithNativeBasicAuthentication() throws Exception {
        for (String path : java.util.List.of("/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config")) {
            mvc.perform(get(path).accept(MediaType.TEXT_HTML)).andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Basic realm=\"ResolveHub\""))
                    .andExpect(header().doesNotExist("Location"));
        }
    }

    @Test
    void givenBasicCredentials_whenSwaggerAndWriteRequested_thenWorkWithoutSessionOrAuthorizationControls() throws Exception {
        var credentials = httpBasic(customer.getUsername(), PASSWORD);
        mvc.perform(get("/swagger-ui/index.html").with(credentials)).andExpect(status().isOk());
        var docs = mvc.perform(get("/v3/api-docs").with(credentials)).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist()).andReturn();
        json.readTree(docs.getResponse().getContentAsString()).get("paths").forEach(path ->
                path.forEach(operation -> assertThat(operation.path("security").isEmpty()).isTrue()));
        mvc.perform(get("/swagger-ui/swagger-initializer.js").with(credentials)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("/api/csrf"))));
        var write = mvc.perform(post("/api/v1/tickets").with(credentials)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Browser ticket","description":"A sufficiently detailed browser ticket","priority":"HIGH"}
                        """)).andExpect(status().isCreated()).andReturn();
        assertThat(write.getRequest().getSession(false)).isNull();
    }

    @Test
    void givenInvalidCredentialsOrLoginRoute_whenRequested_thenDoNotExposeSwaggerOrLoginPage() throws Exception {
        mvc.perform(get("/swagger-ui.html").with(httpBasic(customer.getUsername(), "incorrect-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/login").with(httpBasic(customer.getUsername(), PASSWORD)))
                .andExpect(status().isNotFound());
    }
}
