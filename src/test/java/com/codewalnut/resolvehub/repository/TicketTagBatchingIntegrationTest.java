package com.codewalnut.resolvehub.repository;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.mapper.TicketMapper;
import com.codewalnut.resolvehub.support.ApiScenarioTestSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class TicketTagBatchingIntegrationTest extends ApiScenarioTestSupport {

    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired TicketMapper mapper;

    @Test
    void givenTaggedTicketsAcrossPages_whenMappingOnePage_thenBatchTagsAndPreservePageContents() {
        var expectedIds = new ArrayList<UUID>();
        for (int index = 0; index < 12; index++) {
            var id = new UUID(0, index + 1);
            expectedIds.add(id);
            tickets.save(new TicketEntity(id, "RH-" + id, "Batch page ticket " + index,
                    "A sufficiently detailed description for batching", TicketPriority.HIGH,
                    customer, Set.of("tag-" + id), NOW));
        }
        tickets.flush();
        entityManager.clear();
        var actor = users.findByUsernameIgnoreCase(customer.getUsername()).orElseThrow();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var page = tickets.findAll(TicketSpecifications.visibleAndFiltered(actor, null, null, null, null,
                Sort.by("id")), PageRequest.of(0, 10));
        var responses = page.map(mapper::toResponse);

        assertThat(responses.getTotalElements()).isEqualTo(12);
        assertThat(responses.getContent()).extracting(response -> response.id())
                .containsExactlyElementsOf(expectedIds.subList(0, 10));
        responses.forEach(response -> assertThat(response.tags()).containsExactly("tag-" + response.id()));
        assertThat(statistics.getCollectionFetchCount()).isLessThanOrEqualTo(1);
    }
}
