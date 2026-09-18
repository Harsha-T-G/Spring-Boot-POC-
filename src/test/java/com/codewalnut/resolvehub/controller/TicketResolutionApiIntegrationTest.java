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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketResolutionApiIntegrationTest extends PostgreSqlTestSupport {

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
    private AppUserEntity assignedAgent;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void givenShortUnicodeSummary_whenResolved_thenReturnFieldErrorWithoutMutation() throws Exception {
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        claim(ticket.getId());
        var before = jdbcTemplate.queryForMap("select * from tickets where id = ?", ticket.getId());
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"resolutionSummary\":\"" + "😀".repeat(10) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.resolutionSummary").exists());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForMap("select * from tickets where id = ?", ticket.getId()))
                .isEqualTo(before);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {20, 500})
    void givenValidUnicodeBoundary_whenResolved_thenPersistExactSummary(int length) throws Exception {
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        claim(ticket.getId());
        String summary = "😀".repeat(length);
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"resolutionSummary\":\"" + summary + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutionSummary").value(summary));
        ticketRepository.flush();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "select resolution_summary from tickets where id = ?", String.class, ticket.getId()))
                .isEqualTo(summary);
    }

    @BeforeEach
    void setUp() {
        customer = appUserRepository.saveAndFlush(user("resolve-customer", ApplicationRole.CUSTOMER));
        assignedAgent = appUserRepository.saveAndFlush(user("resolve-agent", ApplicationRole.SUPPORT_AGENT));
        appUserRepository.saveAndFlush(user("other-agent", ApplicationRole.SUPPORT_AGENT));
    }

    @Test
    void givenClaimedTicket_whenAssignedAgentResolves_thenReturnsResolvedTicket() throws Exception {
        // Arrange
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        claim(ticket.getId());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolutionRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionSummary")
                        .value("The access configuration was corrected and verified."))
                .andExpect(jsonPath("$.resolvedAt").exists());
    }

    @Test
    void givenOpenTicket_whenResolutionIsRequested_thenReturnsConflict() throws Exception {
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolutionRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void givenTicketAssignedToAnotherAgent_whenResolutionIsRequested_thenReturnsForbidden() throws Exception {
        // Arrange
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        claim(ticket.getId());

        // Act & Assert
        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("other-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolutionRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenShortResolutionSummary_whenResolutionIsRequested_thenReturnsBadRequest() throws Exception {
        TicketEntity ticket = ticketRepository.saveAndFlush(ticket());
        claim(ticket.getId());

        mockMvc.perform(patch("/api/v1/tickets/{id}/status", ticket.getId())
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"resolutionSummary\":\"Too short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.resolutionSummary").exists());
    }

    private void claim(UUID ticketId) throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{id}/claim", ticketId)
                        .with(csrf()).with(httpBasic("resolve-agent", "test-password")))
                .andExpect(status().isOk());
    }

    private AppUserEntity user(String username, ApplicationRole roleName) {
        RoleEntity role = roleRepository.findByName(roleName).orElseThrow();
        return new AppUserEntity(
                UUID.randomUUID(), username, passwordEncoder.encode("test-password"), true,
                Instant.parse("2026-09-11T00:00:00Z"), Set.of(role));
    }

    private TicketEntity ticket() {
        return new TicketEntity(
                UUID.randomUUID(), "RH-" + UUID.randomUUID().toString().toUpperCase(),
                "Resolvable ticket", "A sufficiently detailed resolvable ticket description",
                TicketPriority.HIGH, customer, Set.of("resolution"), Instant.parse("2026-09-11T01:00:00Z"));
    }

    private String resolutionRequest() {
        return """
                {
                  "status": "RESOLVED",
                  "resolutionSummary": "The access configuration was corrected and verified."
                }
                """;
    }
}
