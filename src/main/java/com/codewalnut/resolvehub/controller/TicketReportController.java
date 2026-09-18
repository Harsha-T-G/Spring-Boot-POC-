package com.codewalnut.resolvehub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codewalnut.resolvehub.dto.TicketSummaryResponse;
import com.codewalnut.resolvehub.service.TicketSummaryService;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Administrative support-ticket reporting")
public class TicketReportController {
    private final TicketSummaryService service;

    public TicketReportController(TicketSummaryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get the ticket summary report")
    @SecurityRequirement(name = "basicAuth")
    public TicketSummaryResponse summarize() {
        return service.summarize();
    }
}
