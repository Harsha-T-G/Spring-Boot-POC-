package com.codewalnut.resolvehub.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;
import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserCreationApiIntegrationTest extends PostgreSqlTestSupport {

    private static final String PASSWORD = "test-only-user-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private RoleRepository roles;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminUsername;

    @BeforeEach
    void setUp() {
        adminUsername = createUser(ApplicationRole.ADMIN).getUsername();
    }

    @Test
    void givenAdminAndValidUser_whenCreatingUser_thenPersistHashedCredentialsAndReturnSafeResponse() throws Exception {
        String username = "new-user-" + UUID.randomUUID();
        String password = "new-user-test-password";

        String response = mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(username, password, List.of("CUSTOMER", "SUPPORT_AGENT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("CUSTOMER", "SUPPORT_AGENT")))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(response);
        AppUserEntity persisted = users.findByUsernameIgnoreCase(username).orElseThrow();
        assertThat(persisted.getId()).isEqualTo(UUID.fromString(created.get("id").asText()));
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getRoles()).extracting(role -> role.getName())
                .containsExactlyInAnyOrder(ApplicationRole.CUSTOMER, ApplicationRole.SUPPORT_AGENT);
        assertThat(persisted.getPasswordHash()).startsWith("$2").isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, persisted.getPasswordHash())).isTrue();
        assertThat(response).doesNotContain(password, persisted.getPasswordHash());

        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(username, password)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = ApplicationRole.class, names = {"CUSTOMER", "SUPPORT_AGENT"})
    void givenNonAdmin_whenCreatingUser_thenReturnForbiddenWithoutInsert(ApplicationRole role) throws Exception {
        AppUserEntity caller = createUser(role);
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(caller.getUsername(), PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("not-created", PASSWORD, List.of("ADMIN"))))
                .andExpect(status().isForbidden());

        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void givenAnonymousCaller_whenCreatingUser_thenReturnUnauthorizedWithoutInsert() throws Exception {
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("not-created", PASSWORD, List.of("CUSTOMER"))))
                .andExpect(status().isUnauthorized());

        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void givenExistingUsernameWithDifferentCase_whenCreatingUser_thenReturnConflict() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(adminUsername.toUpperCase(java.util.Locale.ROOT), PASSWORD, List.of("CUSTOMER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void givenBlankUsernameAndPasswordAndEmptyRoles_whenCreatingUser_thenReturnFieldErrorsWithoutInsert() throws Exception {
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("  ", "  ", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.roles").exists());

        assertThat(users.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "😀"})
    void givenPasswordOverSeventyTwoUtf8Bytes_whenCreatingUser_thenReturnPasswordErrorWithoutInsert(String character) throws Exception {
        String password = character.repeat(character.equals("a") ? 73 : 19);
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("not-created", password, List.of("CUSTOMER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void givenAdminWithoutCsrfToken_whenCreatingUser_thenReturnForbiddenWithoutInsert() throws Exception {
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request("not-created", PASSWORD, List.of("CUSTOMER"))))
                .andExpect(status().isForbidden());

        assertThat(users.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"support:west", ":support", "support:"})
    void givenColonUsername_whenCreatingUser_thenReturnFieldErrorWithoutInsert(String username) throws Exception {
        long count = users.count();

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(username, PASSWORD, List.of("CUSTOMER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());

        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void givenDistinctLowercaseUnicodeUsernames_whenBothCreated_thenEachAuthenticatesIndependently() throws Exception {
        String prefix = "unicode-" + UUID.randomUUID() + "-";
        String sigma = prefix + "σ";
        String finalSigma = prefix + "ς";
        String secondPassword = "another-test-only-password";
        provisionCustomer(sigma, PASSWORD);
        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(sigma, PASSWORD)))
                .andExpect(status().isOk());

        provisionCustomer(finalSigma, secondPassword);

        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(sigma, PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(finalSigma, secondPassword)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(sigma, secondPassword)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/tickets").with(httpBasic(finalSigma, PASSWORD)))
                .andExpect(status().isUnauthorized());
        assertThat(users.findByUsernameIgnoreCase(sigma).orElseThrow().getUsername()).isEqualTo(sigma);
        assertThat(users.findByUsernameIgnoreCase(finalSigma).orElseThrow().getUsername()).isEqualTo(finalSigma);
    }

    private void provisionCustomer(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic(adminUsername, PASSWORD)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(username, password, List.of("CUSTOMER"))))
                .andExpect(status().isCreated());
    }

    private AppUserEntity createUser(ApplicationRole role) {
        return users.saveAndFlush(new AppUserEntity(UUID.randomUUID(), "user-api-" + UUID.randomUUID(),
                passwordEncoder.encode(PASSWORD), true, Instant.parse("2026-09-12T10:00:00Z"),
                Set.of(roles.findByName(role).orElseThrow())));
    }

    private String request(String username, String password, List<String> assignedRoles) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password, "roles", assignedRoles));
    }
}
