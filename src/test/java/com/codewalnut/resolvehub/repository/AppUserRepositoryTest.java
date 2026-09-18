package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppUserRepositoryTest extends PostgreSqlTestSupport {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void givenPersistedUser_whenLookedUpWithDifferentCase_thenReturnsUserWithRoles() {
        // Arrange
        RoleEntity supportAgentRole = roleRepository.findByName(ApplicationRole.SUPPORT_AGENT).orElseThrow();
        AppUserEntity savedUser = appUserRepository.saveAndFlush(user("Agent-One", supportAgentRole));
        entityManager.clear();

        // Act
        Optional<AppUserEntity> result = appUserRepository.findByUsernameIgnoreCase("agent-one");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.orElseThrow().getId());
        assertEquals(Set.of(ApplicationRole.SUPPORT_AGENT), result.orElseThrow().getRoles().stream()
                .map(RoleEntity::getName)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void givenUsernameDifferingOnlyByCase_whenBothArePersisted_thenDatabaseRejectsDuplicate() {
        // Arrange
        RoleEntity customerRole = roleRepository.findByName(ApplicationRole.CUSTOMER).orElseThrow();
        appUserRepository.saveAndFlush(user("customer-one", customerRole));

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> appUserRepository.saveAndFlush(user("CUSTOMER-ONE", customerRole)));
    }

    private AppUserEntity user(String username, RoleEntity role) {
        return new AppUserEntity(
                UUID.randomUUID(),
                username,
                "$2a$10$test-hash-value-not-used-for-comparison123456789",
                true,
                Instant.parse("2026-09-11T00:00:00Z"),
                Set.of(role));
    }
}
