package com.codewalnut.resolvehub.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import com.codewalnut.resolvehub.ResolveHubApplication;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class DevelopmentUserSeederIntegrationTest {

    private static final List<SeedAccount> ACCOUNTS = List.of(
            new SeedAccount("customer", "CUSTOMER", "ROLE_CUSTOMER", "customer-test-only-password"),
            new SeedAccount("agent-one", "AGENT_ONE", "ROLE_SUPPORT_AGENT", "agent-one-test-only-password"),
            new SeedAccount("agent-two", "AGENT_TWO", "ROLE_SUPPORT_AGENT", "agent-two-test-only-password"),
            new SeedAccount("admin", "ADMIN", "ROLE_ADMIN", "admin-test-only-password"));

    @Test
    void givenConfiguredDevelopmentPasswords_whenApplicationStartsAndRestarts_thenAccountsAuthenticateAndRemainUnchanged(
            CapturedOutput output) throws Exception {
        try (var database = newDatabase()) {
            database.start();
            List<Map<String, Object>> originalUsers;
            List<Map<String, Object>> originalRoles;
            try (var application = startApplication(database, "dev", true)) {
                var jdbc = application.getBean(JdbcTemplate.class);
                var encoder = application.getBean(PasswordEncoder.class);
                var authenticationManager = application.getBean(AuthenticationConfiguration.class)
                        .getAuthenticationManager();
                originalUsers = jdbc.queryForList("SELECT * FROM app_users ORDER BY username");
                originalRoles = jdbc.queryForList("SELECT * FROM app_user_roles ORDER BY user_id, role_id");
                assertThat(originalUsers).hasSize(4);
                assertThat(originalRoles).hasSize(4);
                for (SeedAccount account : ACCOUNTS) {
                    String storedHash = jdbc.queryForObject(
                            "SELECT password_hash FROM app_users WHERE username = ?", String.class, account.username());
                    assertThat(storedHash).startsWith("$2").isNotEqualTo(account.password());
                    assertThat(encoder.matches(account.password(), storedHash)).isTrue();
                    var authentication = authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken.unauthenticated(account.username(), account.password()));
                    assertThat(authentication.isAuthenticated()).isTrue();
                    assertThat(authentication.getName()).isEqualTo(account.username());
                    assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                            .containsExactly(account.authority());
                    assertThat(output.getAll()).doesNotContain(account.password(), storedHash);
                }
            }

            try (var restartedApplication = startApplication(database, "dev", true)) {
                var jdbc = restartedApplication.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForList("SELECT * FROM app_users ORDER BY username")).isEqualTo(originalUsers);
                assertThat(jdbc.queryForList("SELECT * FROM app_user_roles ORDER BY user_id, role_id"))
                        .isEqualTo(originalRoles);
                for (SeedAccount account : ACCOUNTS) {
                    assertThat(output.getAll()).doesNotContain(account.password());
                }
                originalUsers.forEach(row -> assertThat(output.getAll()).doesNotContain((String) row.get("password_hash")));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"dev, false", "test, true"})
    void givenSeedingDisabledOrNonDevelopmentProfile_whenApplicationStarts_thenNoDevelopmentAccountsAreCreated(
            String profile, boolean seedingEnabled) {
        try (var database = newDatabase()) {
            database.start();
            try (var application = startApplication(database, profile, seedingEnabled)) {
                var jdbc = application.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_users", Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user_roles", Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class)).isEqualTo(3);
            }
        }
    }

    private PostgreSQLContainer<?> newDatabase() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("resolvehub_seed_test")
                .withUsername("seed_test")
                .withPassword("seed-database-test-only");
    }

    private ConfigurableApplicationContext startApplication(
            PostgreSQLContainer<?> database, String profile, boolean seedingEnabled) {
        var arguments = new ArrayList<>(List.of("--server.port=0",
                "--spring.datasource.url=" + database.getJdbcUrl(),
                "--spring.datasource.username=" + database.getUsername(),
                "--spring.datasource.password=" + database.getPassword(),
                "--resolvehub.seed.enabled=" + seedingEnabled,
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.flyway.enabled=true"));
        if (profile.equals("dev") && seedingEnabled) {
            ACCOUNTS.forEach(account -> arguments.add(
                    "--RESOLVEHUB_" + account.passwordKey() + "_PASSWORD=" + account.password()));
        }
        return new SpringApplicationBuilder(ResolveHubApplication.class)
                .profiles(profile).run(arguments.toArray(String[]::new));
    }

    private record SeedAccount(String username, String passwordKey, String authority, String password) {
    }
}
