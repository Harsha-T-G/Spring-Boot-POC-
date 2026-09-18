package com.codewalnut.resolvehub.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.repository.AppUserRepository;
import com.codewalnut.resolvehub.repository.RoleRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevelopmentUserSeederTest {
    @Test
    void givenDevelopmentPasswords_whenSeededTwice_thenCreateFourHashedUsersOnlyOnce() {
        var persisted = new ArrayList<AppUserEntity>();
        var users = mock(AppUserRepository.class);
        var roles = mock(RoleRepository.class);
        when(users.findByUsernameIgnoreCase(anyString())).thenAnswer(call -> persisted.stream()
                .filter(user -> user.getUsername().equals(call.getArgument(0))).findFirst());
        when(users.save(any(AppUserEntity.class))).thenAnswer(call -> {
            AppUserEntity user = call.getArgument(0);
            persisted.add(user);
            return user;
        });
        when(roles.findByName(any())).thenAnswer(call -> Optional.of(new RoleEntity(UUID.randomUUID(), call.getArgument(0))));
        var environment = new MockEnvironment();
        for (String variable : new String[]{"CUSTOMER", "AGENT_ONE", "AGENT_TWO", "ADMIN"}) {
            environment.setProperty("RESOLVEHUB_" + variable + "_PASSWORD", "test-only-long-password");
        }
        var encoder = new BCryptPasswordEncoder(4);
        var seeder = new DevelopmentUserSeeder(users, roles, encoder, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), environment);

        seeder.run(null);
        seeder.run(null);

        assertThat(persisted).extracting(AppUserEntity::getUsername)
                .containsExactly("customer", "agent-one", "agent-two", "admin");
        assertThat(persisted).allSatisfy(user -> {
            assertThat(user.getPasswordHash()).startsWith("$2");
            assertThat(encoder.matches("test-only-long-password", user.getPasswordHash())).isTrue();
            assertThat(user.getCreatedAt()).isEqualTo(Instant.EPOCH);
        });
        assertThat(persisted.get(1).getRoles()).extracting(RoleEntity::getName).containsExactly(ApplicationRole.SUPPORT_AGENT);
    }

    @Test
    void givenMissingPasswords_whenSeeding_thenFailClearlyBeforeCreatingUsers() {
        var seeder = new DevelopmentUserSeeder(mock(AppUserRepository.class), mock(RoleRepository.class),
                new BCryptPasswordEncoder(4), Clock.systemUTC(), new MockEnvironment());
        assertThatThrownBy(() -> seeder.run(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESOLVEHUB_CUSTOMER_PASSWORD");
    }
}
