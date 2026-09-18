
package com.codewalnut.resolvehub;

import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest extends PostgreSqlTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenEmptyDatabase_whenApplicationStarts_thenFlywayCreatesRequiredSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class);
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('app_users', 'roles', 'app_user_roles', 'tickets', 'ticket_tags')
                """,
                Integer.class);

        assertEquals(2, migrationCount);
        assertEquals(5, tableCount);
    }

    @Test
    void givenAppliedMigrations_whenSchemaIsInspected_thenCriticalConstraintsAndIndexesExist() {
        Integer usernameIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'app_users_username_unique_lower'",
                Integer.class);
        Integer referenceIndexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'tickets_reference_number_unique'",
                Integer.class);

        assertEquals(1, usernameIndexCount);
        assertEquals(1, referenceIndexCount);
    }
}
