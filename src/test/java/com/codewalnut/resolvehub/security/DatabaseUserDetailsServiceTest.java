package com.codewalnut.resolvehub.security;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void givenEnabledDatabaseUser_whenLoaded_thenMapsCredentialsAndAuthorities() {
        // Arrange
        AppUserEntity user = user("agent-one", true, ApplicationRole.SUPPORT_AGENT);
        when(appUserRepository.findByUsernameIgnoreCase("agent-one")).thenReturn(Optional.of(user));

        // Act
        UserDetails details = userDetailsService.loadUserByUsername("  agent-one  ");

        // Assert
        assertEquals("agent-one", details.getUsername());
        assertEquals(user.getPasswordHash(), details.getPassword());
        assertTrue(details.isEnabled());
        assertEquals(
                Set.of("ROLE_SUPPORT_AGENT"),
                details.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void givenDisabledDatabaseUser_whenLoaded_thenPrincipalIsDisabled() {
        // Arrange
        AppUserEntity user = user("disabled-user", false, ApplicationRole.CUSTOMER);
        when(appUserRepository.findByUsernameIgnoreCase("disabled-user")).thenReturn(Optional.of(user));

        // Act
        UserDetails details = userDetailsService.loadUserByUsername("disabled-user");

        // Assert
        assertFalse(details.isEnabled());
    }

    @Test
    void givenUnknownUsername_whenLoaded_thenThrowsGenericException() {
        // Arrange
        when(appUserRepository.findByUsernameIgnoreCase("missing-user")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing-user"));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    private AppUserEntity user(String username, boolean enabled, ApplicationRole role) {
        return new AppUserEntity(
                UUID.randomUUID(),
                username,
                "$2a$10$test-hash-value-not-used-for-comparison123456789",
                enabled,
                Instant.parse("2026-09-11T00:00:00Z"),
                Set.of(new RoleEntity(UUID.randomUUID(), role)));
    }
}
