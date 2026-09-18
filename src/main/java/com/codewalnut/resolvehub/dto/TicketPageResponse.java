package com.codewalnut.resolvehub.dto;

import java.util.List;

public record TicketPageResponse(List<TicketResponse> content, int number, int size,
        long totalElements, int totalPages) {
    public TicketPageResponse {
        content = List.copyOf(content);
    }
}
