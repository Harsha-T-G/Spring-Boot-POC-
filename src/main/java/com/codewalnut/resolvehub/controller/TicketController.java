package com.codewalnut.resolvehub.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.dto.CreateTicketRequest;
import com.codewalnut.resolvehub.dto.TicketPageResponse;
import com.codewalnut.resolvehub.dto.TicketResponse;
import com.codewalnut.resolvehub.dto.UpdateTicketStatusRequest;
import com.codewalnut.resolvehub.service.TicketClaimService;
import com.codewalnut.resolvehub.service.TicketResolutionService;
import com.codewalnut.resolvehub.service.TicketService;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Create, find, claim and resolve support tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketClaimService ticketClaimService;
    private final TicketResolutionService ticketResolutionService;

    public TicketController(
            TicketService ticketService,
            TicketClaimService ticketClaimService,
            TicketResolutionService ticketResolutionService) {
        this.ticketService = ticketService;
        this.ticketClaimService = ticketClaimService;
        this.ticketResolutionService = ticketResolutionService;
    }

    @PostMapping
    @Operation(summary = "Create a ticket",
            description = "Customers create for themselves; administrators must supply a customerId.")
    @ApiResponse(responseCode = "201", description = "Ticket created")
    @SecurityRequirement(name = "basicAuth")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {
        TicketResponse response = ticketService.create(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a visible ticket")
    @SecurityRequirement(name = "basicAuth")
    public TicketResponse get(@PathVariable UUID id, Authentication authentication) {
        return ticketService.get(id, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List visible tickets", description = "Results are role-scoped, filtered, paged and stably sorted.")
    @SecurityRequirement(name = "basicAuth")
    public TicketPageResponse list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) UUID assignedAgentId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        return ticketService.list(status, priority, assignedAgentId, search, page, size, sort,
                authentication.getName());
    }

    @PostMapping("/{id}/claim")
    @Operation(summary = "Claim an open ticket",
            description = "Support agents only. A competing or repeated claim returns 409 Conflict.")
    @SecurityRequirement(name = "basicAuth")
    public TicketResponse claim(@PathVariable UUID id, Authentication authentication) {
        return ticketClaimService.claim(id, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Resolve a claimed ticket",
            description = "The assigned support agent or an administrator can resolve an IN_PROGRESS ticket.")
    @SecurityRequirement(name = "basicAuth")
    public TicketResponse resolve(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            Authentication authentication) {
        return ticketResolutionService.resolve(id, request, authentication.getName());
    }
}
