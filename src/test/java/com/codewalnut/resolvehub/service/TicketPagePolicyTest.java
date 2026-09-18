package com.codewalnut.resolvehub.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class TicketPagePolicyTest {

    private final TicketPagePolicy policy = new TicketPagePolicy(20, 100);

    @Test
    void givenNoSizeOrSort_whenPageRequested_thenUsesConfiguredDefaults() {
        var page = policy.create(0, null, null);

        assertEquals(20, page.getPageSize());
        assertEquals(Sort.Direction.DESC, page.getSort().getOrderFor("createdAt").getDirection());
        assertNotNull(page.getSort().getOrderFor("id"));
    }

    @Test
    void givenInvalidPageOrSize_whenPageRequested_thenRejectsInput() {
        assertThrows(IllegalArgumentException.class, () -> policy.create(-1, 20, null));
        assertThrows(IllegalArgumentException.class, () -> policy.create(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> policy.create(0, 101, null));
    }

    @Test
    void givenUnsupportedSort_whenPageRequested_thenRejectsInput() {
        assertThrows(IllegalArgumentException.class, () -> policy.create(0, 10, "description,asc"));
        assertThrows(IllegalArgumentException.class, () -> policy.create(0, 10, "title,invalid"));
        assertThrows(IllegalArgumentException.class, () -> policy.create(0, 10, "title,asc,extra"));
    }

    @Test
    void givenSupportedSortAndMaximumSize_whenPageRequested_thenPreservesRequest() {
        var page = policy.create(2, 100, "title,asc");

        assertEquals(2, page.getPageNumber());
        assertEquals(100, page.getPageSize());
        assertEquals(Sort.Direction.ASC, page.getSort().getOrderFor("title").getDirection());
    }
}
