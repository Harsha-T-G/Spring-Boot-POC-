package com.codewalnut.resolvehub.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codewalnut.resolvehub.domain.*;
import com.codewalnut.resolvehub.entity.*;
import com.codewalnut.resolvehub.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiScenarioTestSupport.FixedTime.class)
public abstract class ApiScenarioTestSupport extends PostgreSqlTestSupport {
    protected static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");
    protected static final String PASSWORD = "test-only-password";
    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected AppUserRepository users;
    @Autowired protected RoleRepository roles;
    @Autowired protected TicketRepository tickets;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected PlatformTransactionManager transactions;
    protected AppUserEntity customer;
    protected AppUserEntity otherCustomer;
    protected AppUserEntity agentOne;
    protected AppUserEntity agentTwo;
    protected AppUserEntity admin;
    private final List<UUID> userIds = new ArrayList<>();

    @TestConfiguration
    public static class FixedTime {
        @Bean @Primary
        Clock scenarioClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void createScenarioUsers() {
        customer = createUser(ApplicationRole.CUSTOMER, true);
        otherCustomer = createUser(ApplicationRole.CUSTOMER, true);
        agentOne = createUser(ApplicationRole.SUPPORT_AGENT, true);
        agentTwo = createUser(ApplicationRole.SUPPORT_AGENT, true);
        admin = createUser(ApplicationRole.ADMIN, true);
    }

    protected AppUserEntity createUser(ApplicationRole role, boolean enabled) {
        return new TransactionTemplate(transactions).execute(status -> {
            UUID id = UUID.randomUUID();
            userIds.add(id);
            return users.saveAndFlush(new AppUserEntity(id, "scenario-" + id, encoder.encode(PASSWORD),
                    enabled, NOW, Set.of(roles.findByName(role).orElseThrow())));
        });
    }

    protected TicketEntity createTicket(AppUserEntity owner, TicketPriority priority, Instant createdAt) {
        return tickets.saveAndFlush(new TicketEntity(UUID.randomUUID(), "RH-" + UUID.randomUUID(),
                "Scenario ticket", "A sufficiently detailed scenario description", priority, owner,
                Set.of("scenario"), createdAt));
    }

    @AfterEach
    void removeScenarioData() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var owned = tickets.findAll((root, query, builder) -> root.get("customer").get("id").in(userIds));
            tickets.deleteAll(owned);
            tickets.flush();
            users.deleteAllById(userIds);
            users.flush();
        });
    }
}
