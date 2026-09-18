package com.codewalnut.resolvehub.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class TicketPagePolicy {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "priority", "status", "title");

    private final int defaultSize;
    private final int maxSize;

    public TicketPagePolicy(
            @Value("${resolvehub.paging.default-size}") int defaultSize,
            @Value("${resolvehub.paging.max-size}") int maxSize) {
        if (defaultSize < 1 || maxSize < defaultSize) {
            throw new IllegalArgumentException("Invalid paging configuration");
        }
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    public PageRequest create(int page, Integer size, String sort) {
        int requestedSize = size == null ? defaultSize : size;
        if (page < 0 || requestedSize < 1 || requestedSize > maxSize) {
            throw new IllegalArgumentException("page must be nonnegative and size must be between 1 and " + maxSize);
        }
        String[] parts = (sort == null ? "createdAt,desc" : sort).split(",", -1);
        if (parts.length > 2 || !SORT_FIELDS.contains(parts[0])) {
            throw new IllegalArgumentException("Unsupported ticket sort");
        }
        Sort.Direction direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
        return PageRequest.of(page, requestedSize,
                Sort.by(direction, parts[0]).and(Sort.by("id")));
    }
}
