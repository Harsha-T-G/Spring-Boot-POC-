package com.codewalnut.resolvehub.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.support.TransactionTemplate;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketPaginationIntegrationTest extends ApiScenarioTestSupport {
    @BeforeEach
    void createSortableTickets() {
        String[] titles = {"Zulu ticket", "Alpha ticket", "Alpha ticket", "Beta ticket", "Gamma ticket", "Zulu ticket"};
        TicketPriority[] priorities = {TicketPriority.LOW, TicketPriority.HIGH, TicketPriority.CRITICAL,
                TicketPriority.LOW, TicketPriority.MEDIUM, TicketPriority.HIGH};
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            for (int index = 0; index < titles.length; index++) {
                var ticket = tickets.saveAndFlush(new TicketEntity(new UUID(0, index + 1), "RH-" + UUID.randomUUID(),
                        titles[index], "A sufficiently detailed description", priorities[index], customer, Set.of(), NOW));
                if (index % 3 != 0) {
                    tickets.claimOpenTicket(ticket.getId(), agentOne, NOW);
                }
                if (index % 3 == 2) {
                    var claimed = tickets.findById(ticket.getId()).orElseThrow();
                    claimed.resolve("A sufficiently detailed resolution", NOW);
                    tickets.saveAndFlush(claimed);
                }
            }
        });
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "createdAt,asc|1 2 3 4 5 6", "createdAt,desc|1 2 3 4 5 6",
            "title,asc|2 3 4 5 1 6", "title,desc|1 6 5 4 2 3",
            "priority,asc|3 2 6 5 1 4", "priority,desc|1 4 5 2 6 3",
            "status,asc|2 5 1 4 3 6", "status,desc|3 6 1 4 2 5"
    })
    void givenTiedSortValues_whenReadingAllPages_thenKeepStableOrderWithoutDuplicates(String sort, String expected)
            throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            var result = mvc.perform(get("/api/v1/tickets").with(httpBasic(admin.getUsername(), PASSWORD))
                            .param("sort", sort).param("size", "2").param("page", Integer.toString(page)))
                    .andExpect(status().isOk()).andReturn();
            var response = json.readTree(result.getResponse().getContentAsString());
            assertThat(response.get("totalElements").asInt()).isEqualTo(6);
            assertThat(response.get("totalPages").asInt()).isEqualTo(3);
            assertThat(response.get("content").size()).isEqualTo(2);
            response.get("content").forEach(ticket -> ids.add(UUID.fromString(ticket.get("id").asText())
                    .getLeastSignificantBits()));
        }
        assertThat(ids).containsExactlyElementsOf(java.util.Arrays.stream(expected.split(" ")).map(Long::valueOf).toList());
    }
}
