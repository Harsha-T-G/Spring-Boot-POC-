package com.codewalnut.resolvehub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codewalnut.resolvehub.dto.CsrfTokenResponse;

@RestController
@Tag(name = "Security", description = "Browser-compatible CSRF token acquisition")
public class CsrfController {
    @GetMapping("/api/csrf")
    @Operation(summary = "Get a CSRF token", description = "Use Basic credentials. "
            + "Swagger obtains this token automatically before write operations.")
    @SecurityRequirement(name = "basicAuth")
    public CsrfTokenResponse csrf(@Parameter(hidden = true) CsrfToken token) {
        return new CsrfTokenResponse(token.getHeaderName(), token.getToken());
    }
}
