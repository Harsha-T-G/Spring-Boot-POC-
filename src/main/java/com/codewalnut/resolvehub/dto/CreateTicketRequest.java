package com.codewalnut.resolvehub.dto;

import com.codewalnut.resolvehub.domain.TicketPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CodePointLength;

import java.util.List;
import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank @CodePointLength(min = 5, max = 120) String title,
        @NotBlank @CodePointLength(min = 20, max = 1000) String description,
        @NotNull TicketPriority priority,
        @Size(max = 5) List<@Valid @NotBlank @CodePointLength(max = 100) String> tags,
        UUID customerId) {
    public CreateTicketRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
        tags = tags == null ? null : tags.stream()
                .map(tag -> tag == null ? null : tag.trim().toLowerCase(java.util.Locale.ROOT))
                .toList();
    }
}
