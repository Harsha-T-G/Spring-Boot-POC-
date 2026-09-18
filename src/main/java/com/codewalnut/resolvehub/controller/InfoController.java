package com.codewalnut.resolvehub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codewalnut.resolvehub.dto.ApplicationInfoResponse;

@RestController
@RequestMapping("/api/info")
@Tag(name = "Application", description = "Public application metadata")
public class InfoController {

    private final String applicationName;
    private final String applicationVersion;

    public InfoController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${info.app.version}") String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping
    @Operation(summary = "Get application information")
    public ApplicationInfoResponse getApplicationInfo() {
        return new ApplicationInfoResponse(applicationName, applicationVersion);
    }

}
