package com.codewalnut.resolvehub.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    @Bean
    OpenAPI resolveHubOpenApi(@Value("${info.app.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("ResolveHub Lite API")
                        .version(version)
                        .description("Use the browser's built-in username/password prompt before opening Swagger. "
                                + "HTTP Basic authenticates each request; role permissions are enforced by the server."));
    }

    @Bean
    OpenApiCustomizer useBrowserCredentialsWithoutAuthorizationControls() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    operation.setSecurity(List.of());
                }));
    }
}
