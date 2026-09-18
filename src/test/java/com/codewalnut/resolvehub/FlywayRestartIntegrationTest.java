package com.codewalnut.resolvehub;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

class FlywayRestartIntegrationTest {

    @Test
    void givenMigratedDatabaseWithTicket_whenApplicationRestarts_thenHistoryAndStoredRowsAreUnchanged() {
        try (var database = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("resolvehub_restart")
                .withUsername("restart_test")
                .withPassword("restart-test-only")) {
            database.start();
            List<Map<String, Object>> migrationHistory;
            List<Map<String, Object>> storedUsers;
            List<Map<String, Object>> storedTickets;
            List<Map<String, Object>> storedTags;
            try (var firstApplication = startApplication(database)) {
                var jdbc = firstApplication.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Integer.class)).isZero();
                UUID customerId = UUID.randomUUID();
                UUID ticketId = UUID.randomUUID();
                Timestamp createdAt = Timestamp.from(Instant.parse("2026-09-12T10:00:00Z"));
                String passwordHash = firstApplication.getBean(PasswordEncoder.class).encode("restart-test-password");
                jdbc.update("INSERT INTO app_users (id, username, password_hash, enabled, created_at) VALUES (?, ?, ?, ?, ?)",
                        customerId, "restart-customer", passwordHash, true, createdAt);
                jdbc.update("""
                        INSERT INTO tickets (id, reference_number, title, description, priority, status,
                                             customer_id, created_at, updated_at, version)
                        VALUES (?, ?, ?, ?, 'HIGH', 'OPEN', ?, ?, ?, 0)
                        """, ticketId, "RH-" + ticketId, "Preserved restart ticket",
                        "Existing ticket data must survive application restart.", customerId, createdAt, createdAt);
                jdbc.update("INSERT INTO ticket_tags (ticket_id, tag) VALUES (?, ?)", ticketId, "restart");
                migrationHistory = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
                assertThat(migrationHistory).hasSize(2).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
                storedUsers = jdbc.queryForList("SELECT * FROM app_users ORDER BY id");
                storedTickets = jdbc.queryForList("SELECT * FROM tickets ORDER BY id");
                storedTags = jdbc.queryForList("SELECT * FROM ticket_tags ORDER BY ticket_id, tag");
            }

            try (var restartedApplication = startApplication(database)) {
                var jdbc = restartedApplication.getBean(JdbcTemplate.class);
                var flyway = restartedApplication.getBean(Flyway.class);
                assertThat(flyway.info().pending()).isEmpty();
                assertThat(jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"))
                        .isEqualTo(migrationHistory);
                assertThat(jdbc.queryForList("SELECT * FROM app_users ORDER BY id")).isEqualTo(storedUsers);
                assertThat(jdbc.queryForList("SELECT * FROM tickets ORDER BY id")).isEqualTo(storedTickets);
                assertThat(jdbc.queryForList("SELECT * FROM ticket_tags ORDER BY ticket_id, tag")).isEqualTo(storedTags);
            }
        }
    }

    private ConfigurableApplicationContext startApplication(PostgreSQLContainer<?> database) {
        return new SpringApplicationBuilder(ResolveHubApplication.class)
                .profiles("test")
                .run("--server.port=0", "--spring.datasource.url=" + database.getJdbcUrl(),
                        "--spring.datasource.username=" + database.getUsername(),
                        "--spring.datasource.password=" + database.getPassword(),
                        "--spring.jpa.hibernate.ddl-auto=validate", "--spring.flyway.enabled=true");
    }
}
