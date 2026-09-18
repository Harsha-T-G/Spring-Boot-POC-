package com.codewalnut.resolvehub.controller;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.codewalnut.resolvehub.domain.*;
import com.codewalnut.resolvehub.entity.*;
import com.codewalnut.resolvehub.repository.*;
import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketListingApiIntegrationTest extends PostgreSqlTestSupport {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired RoleRepository roles;
    @Autowired TicketRepository tickets;

    @BeforeEach
    void setUp() {
        var customer = users.saveAndFlush(new AppUserEntity(UUID.randomUUID(), "list-customer", "hash", true,
                Instant.EPOCH, Set.of(roles.findByName(ApplicationRole.CUSTOMER).orElseThrow())));
        var other = users.saveAndFlush(new AppUserEntity(UUID.randomUUID(), "list-other", "hash", true,
                Instant.EPOCH, Set.of(roles.findByName(ApplicationRole.CUSTOMER).orElseThrow())));
        for (var priority : TicketPriority.values()) {
            tickets.saveAndFlush(new TicketEntity(UUID.randomUUID(), "RH-" + UUID.randomUUID(),
                    "Searchable ticket " + priority, "A sufficiently detailed description", priority,
                    customer, Set.of("list"), Instant.EPOCH));
        }
        tickets.saveAndFlush(new TicketEntity(UUID.randomUUID(), "RH-" + UUID.randomUUID(),
                "Other customer ticket", "A sufficiently detailed description", TicketPriority.HIGH,
                other, Set.of(), Instant.EPOCH));
    }

    @Test
    void givenCustomerTickets_whenListedByPriority_thenScopeAndPaginateInBusinessOrder() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(user("list-customer").roles("CUSTOMER"))
                        .param("sort", "priority,asc").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$.content[1].priority").value("HIGH"));
    }

    @Test
    void givenFilters_whenListed_thenCombineSearchPriorityAndStatus() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(user("list-customer").roles("CUSTOMER"))
                        .param("search", "SEARCHABLE").param("priority", "LOW").param("status", "OPEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void givenLiteralWildcardSearch_whenListed_thenDoNotMatchAllTitles() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(user("list-customer").roles("CUSTOMER"))
                        .param("search", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void givenInvalidPage_whenListed_thenReturnBadRequest() throws Exception {
        mvc.perform(get("/api/v1/tickets").with(user("list-customer").roles("CUSTOMER"))
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
