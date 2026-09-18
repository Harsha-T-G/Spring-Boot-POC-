package com.codewalnut.resolvehub.repository;

import com.codewalnut.resolvehub.domain.ApplicationRole;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import com.codewalnut.resolvehub.entity.AppUserEntity;
import com.codewalnut.resolvehub.entity.RoleEntity;
import com.codewalnut.resolvehub.entity.TicketEntity;
import com.codewalnut.resolvehub.support.PostgreSqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketRepositoryTest extends PostgreSqlTestSupport {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    private AppUserEntity customer;

    @BeforeEach
    void setUp() {
        RoleEntity customerRole = roleRepository.findByName(ApplicationRole.CUSTOMER).orElseThrow();
        customer = appUserRepository.saveAndFlush(new AppUserEntity(
                UUID.randomUUID(),
                "repository-customer",
                "$2a$10$test-hash-value-not-used-for-comparison123456789",
                true,
                Instant.parse("2026-09-11T00:00:00Z"),
                Set.of(customerRole)));
    }

    @Test
    void givenDuplicateReferenceNumber_whenTicketsArePersisted_thenDatabaseRejectsDuplicate() {
        // Arrange
        ticketRepository.saveAndFlush(ticket("RH-DUPLICATE", TicketPriority.LOW, Instant.parse("2026-09-11T01:00:00Z")));

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(
                        ticket("RH-DUPLICATE", TicketPriority.HIGH, Instant.parse("2026-09-11T02:00:00Z"))));
    }

    @Test
    void givenMatchingTickets_whenFilteredAndPaged_thenDatabaseReturnsRequestedPage() {
        // Arrange
        ticketRepository.save(ticket("RH-LOW-ONE", TicketPriority.LOW, Instant.parse("2026-09-11T01:00:00Z")));
        ticketRepository.save(ticket("RH-HIGH-ONE", TicketPriority.HIGH, Instant.parse("2026-09-11T02:00:00Z")));
        ticketRepository.saveAndFlush(ticket("RH-HIGH-TWO", TicketPriority.HIGH, Instant.parse("2026-09-11T03:00:00Z")));

        // Act
        Page<TicketEntity> result = ticketRepository.findAllByStatusAndPriority(
                TicketStatus.OPEN,
                TicketPriority.HIGH,
                PageRequest.of(0, 1));

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(TicketPriority.HIGH, result.getContent().getFirst().getPriority());
    }

    private TicketEntity ticket(String referenceNumber, TicketPriority priority, Instant createdAt) {
        return new TicketEntity(
                UUID.randomUUID(),
                referenceNumber,
                "Valid ticket title",
                "A sufficiently detailed ticket description",
                priority,
                customer,
                Set.of("api", "support"),
                createdAt);
    }
}
