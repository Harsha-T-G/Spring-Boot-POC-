package com.codewalnut.resolvehub.controller;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.codewalnut.resolvehub.domain.*;
import com.codewalnut.resolvehub.entity.*;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.repository.*;
import com.codewalnut.resolvehub.service.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketControllerWebMvcTest {
    @Test
    void givenAuthenticatedCustomer_whenListingEmptyTickets_thenReturnPageEnvelope() throws Exception {
        var mvc = mvc();
        mvc.perform(get("/api/v1/tickets").principal(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("customer", null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "sort=passwordHash", "sort=title,bogus", "priority=INVALID", "assignedAgentId=invalid"})
    void givenInvalidListInput_whenRequested_thenReturnTraceableBadRequest(String query) throws Exception {
        String[] parts = query.split("=", 2);
        mvc().perform(get("/api/v1/tickets").param(parts[0], parts[1]).principal(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("customer", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void givenMalformedJson_whenCreating_thenReturnSafeBadRequest() throws Exception {
        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/tickets")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void givenShortTitle_whenCreating_thenReturnFieldErrors() throws Exception {
        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/tickets")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  hi  \",\"description\":\"A sufficiently detailed description\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    private org.springframework.test.web.servlet.MockMvc mvc() {
        var tickets = mock(TicketRepository.class);
        var users = mock(AppUserRepository.class);
        var customer = new AppUserEntity(UUID.randomUUID(), "customer", "hash", true, Instant.EPOCH,
                Set.of(new RoleEntity(UUID.randomUUID(), ApplicationRole.CUSTOMER)));
        when(users.findByUsernameIgnoreCase("customer")).thenReturn(Optional.of(customer));
        when(tickets.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        var mapper = new TicketMapper();
        var service = new TicketService(tickets, users, new UuidReferenceNumberGenerator(),
                new TagNormalizer(), mapper, Clock.systemUTC(), new TicketPagePolicy(20, 100));
        var controller = new TicketController(service,
                new TicketClaimService(tickets, users, mapper, Clock.systemUTC()),
                new TicketResolutionService(tickets, users, new DefaultTicketTransitionPolicy(), mapper, Clock.systemUTC()));
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.codewalnut.resolvehub.exception.GlobalExceptionHandler(
                        new com.codewalnut.resolvehub.exception.ApiErrorFactory(Clock.systemUTC())))
                .addFilters(new com.codewalnut.resolvehub.security.TraceIdFilter()).build();
    }
}
