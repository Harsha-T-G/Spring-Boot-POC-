package com.codewalnut.resolvehub.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

@Component
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
public class SwaggerCsrfIndexTransformer extends SwaggerIndexPageTransformer {
    private final String interceptor;

    public SwaggerCsrfIndexTransformer(SwaggerUiConfigProperties config, SwaggerUiOAuthProperties oauth,
            SwaggerWelcomeCommon welcome, ObjectMapperProvider mapper) throws IOException {
        super(config, oauth, welcome, mapper);
        interceptor = new ClassPathResource("swagger/csrf-interceptor.js").getContentAsString(StandardCharsets.UTF_8);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain)
            throws IOException {
        Resource transformed = super.transform(request, resource, chain);
        if (!"swagger-initializer.js".equals(resource.getFilename())) {
            return transformed;
        }
        String script = transformed.getContentAsString(StandardCharsets.UTF_8);
        return new TransformedResource(resource,
                script.replace("SwaggerUIBundle({", "SwaggerUIBundle({\n" + interceptor).getBytes(StandardCharsets.UTF_8));
    }
}
