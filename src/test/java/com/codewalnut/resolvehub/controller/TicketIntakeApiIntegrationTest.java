package com.codewalnut.resolvehub.controller;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketIntakeApiIntegrationTest extends PostgreSqlTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUserEntity customer;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.codewalnut.resolvehub.repository.TicketRepository ticketRepository;

    @Test
    void givenShortUnicodeText_whenCreated_thenReturnFieldErrorsWithoutInsert() throws Exception {
        Long count = jdbcTemplate.queryForObject("select count(*) from tickets", Long.class);
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"😀😀😀","description":"😀😀😀😀😀😀😀😀😀😀","priority":"HIGH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject("select count(*) from tickets", Long.class))
                .isEqualTo(count);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"5,20", "120,1000"})
    void givenValidUnicodeBoundaries_whenCreated_thenPersistExactText(int titleLength, int descriptionLength) throws Exception {
        String title = "😀".repeat(titleLength);
        String description = "😀".repeat(descriptionLength);
        String tag = "😀".repeat(100);
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                java.util.Map.of("title", title, "description", description, "priority", "HIGH", "tags", java.util.List.of(tag)));
        String response = mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText());
        ticketRepository.flush();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject("select title from tickets where id = ?", String.class, id))
                .isEqualTo(title);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject("select description from tickets where id = ?", String.class, id))
                .isEqualTo(description);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject("select tag from ticket_tags where ticket_id = ?", String.class, id))
                .isEqualTo(tag);
    }

    @BeforeEach
    void setUp() {
        RoleEntity customerRole = roleRepository.findByName(ApplicationRole.CUSTOMER).orElseThrow();
        customer = appUserRepository.saveAndFlush(user("ticket-customer", customerRole));
    }

    @Test
    void givenCustomerAndValidRequest_whenTicketIsCreated_thenReturnsCreatedNormalizedTicket() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot access billing portal",
                                  "description": "The billing portal returns an access denied message every time.",
                                  "priority": "HIGH",
                                  "tags": [" Billing ", "URGENT", "billing"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/tickets/.+")))
                .andExpect(jsonPath("$.referenceNumber", org.hamcrest.Matchers.matchesPattern("RH-[0-9A-F-]{36}")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.customerId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags", org.hamcrest.Matchers.containsInAnyOrder("billing", "urgent")));
    }

    @Test
    void givenCustomerOwnedTicket_whenTicketIsRequested_thenReturnsTicket() throws Exception {
        String response = mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ticketId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/tickets/{id}", ticketId)
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.title").value("Cannot access billing portal"));
    }

    @Test
    void givenInvalidTicketRequest_whenCustomerCreatesTicket_thenReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .with(csrf()).with(httpBasic("ticket-customer", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"bad","description":"short","priority":null,"tags":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.priority").exists());
    }

    private AppUserEntity user(String username, RoleEntity role) {
        return new AppUserEntity(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode("test-password"),
                true,
                Instant.parse("2026-09-11T00:00:00Z"),
                Set.of(role));
    }

    private String validRequest() {
        return """
                {
                  "title": "Cannot access billing portal",
                  "description": "The billing portal returns an access denied message every time.",
                  "priority": "HIGH",
                  "tags": ["billing"]
                }
                """;
    }
}
