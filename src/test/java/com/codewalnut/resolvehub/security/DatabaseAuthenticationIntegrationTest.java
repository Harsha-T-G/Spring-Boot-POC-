package com.codewalnut.resolvehub.security;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DatabaseAuthenticationIntegrationTest extends PostgreSqlTestSupport {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        RoleEntity customerRole = roleRepository.findByName(ApplicationRole.CUSTOMER).orElseThrow();
        appUserRepository.saveAndFlush(user("enabled-user", "correct-password", true, customerRole));
        appUserRepository.saveAndFlush(user("disabled-user", "correct-password", false, customerRole));
    }

    @Test
    void givenNoCredentials_whenProtectedEndpointIsRequested_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected-probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenValidDatabaseCredentials_whenProtectedEndpointIsRequested_thenAuthenticationSucceeds() throws Exception {
        mockMvc.perform(get("/api/protected-probe")
                        .with(csrf()).with(httpBasic("enabled-user", "correct-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenInvalidPassword_whenProtectedEndpointIsRequested_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected-probe")
                        .with(csrf()).with(httpBasic("enabled-user", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenDisabledDatabaseUser_whenProtectedEndpointIsRequested_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/protected-probe")
                        .with(csrf()).with(httpBasic("disabled-user", "correct-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenNoCredentials_whenInfoEndpointIsRequested_thenReturnsApplicationMetadata() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("resolvehub-lite"))
                .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"));
    }

    @Test
    void givenNoCredentials_whenHealthEndpointIsRequested_thenReturnsHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private AppUserEntity user(String username, String password, boolean enabled, RoleEntity role) {
        return new AppUserEntity(
                UUID.randomUUID(),
                username,
                PASSWORD_ENCODER.encode(password),
                enabled,
                Instant.parse("2026-09-11T00:00:00Z"),
                Set.of(role));
    }
}
