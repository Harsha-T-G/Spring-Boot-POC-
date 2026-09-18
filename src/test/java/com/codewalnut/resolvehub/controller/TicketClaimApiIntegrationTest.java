package com.codewalnut.resolvehub.controller;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;
import com.codewalnut.resolvehub.repository.TicketRepository;
import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketClaimApiIntegrationTest extends PostgreSqlTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUserEntity customer;
    private AppUserEntity agent;

    @BeforeEach
    void setUp() {
        customer = appUserRepository.saveAndFlush(user("claim-customer", ApplicationRole.CUSTOMER));
        agent = appUserRepository.saveAndFlush(user("claim-agent", ApplicationRole.SUPPORT_AGENT));
    }

    @Test
    void givenOpenTicket_whenSupportAgentClaims_thenReturnsAssignedInProgressTicket() throws Exception {
        // Arrange
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());

        // Act & Assert
        mockMvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                        .with(csrf()).with(httpBasic("claim-agent", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedAgentId").value(agent.getId().toString()));
    }

    @Test
    void givenAlreadyClaimedTicket_whenSupportAgentClaimsAgain_thenReturnsConflict() throws Exception {
        // Arrange
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        mockMvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                        .with(csrf()).with(httpBasic("claim-agent", "test-password")))
                .andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                        .with(csrf()).with(httpBasic("claim-agent", "test-password")))
                .andExpect(status().isConflict());
    }

    @Test
    void givenOpenTicket_whenCustomerAttemptsClaim_thenReturnsForbidden() throws Exception {
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());

        mockMvc.perform(post("/api/v1/tickets/{id}/claim", ticket.getId())
                        .with(csrf()).with(httpBasic("claim-customer", "test-password")))
                .andExpect(status().isForbidden());
    }

    private AppUserEntity user(String username, ApplicationRole applicationRole) {
        RoleEntity role = roleRepository.findByName(applicationRole).orElseThrow();
        return new AppUserEntity(
                UUID.randomUUID(), username, passwordEncoder.encode("test-password"), true,
                Instant.parse("2026-09-11T00:00:00Z"), Set.of(role));
    }

    private TicketEntity ticket() {
        return new TicketEntity(
                UUID.randomUUID(), "RH-" + UUID.randomUUID().toString().toUpperCase(),
                "Claimable ticket", "A sufficiently detailed claimable ticket description",
                TicketPriority.HIGH, customer, Set.of("claim"), Instant.parse("2026-09-11T01:00:00Z"));
    }
}
