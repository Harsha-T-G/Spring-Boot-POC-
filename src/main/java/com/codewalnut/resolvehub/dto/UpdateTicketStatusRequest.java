package com.codewalnut.resolvehub.dto;

import com.codewalnut.resolvehub.domain.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.CodePointLength;

public record UpdateTicketStatusRequest(
        @NotNull TicketStatus status,
        @NotBlank @CodePointLength(min = 20, max = 500) String resolutionSummary) {
    public UpdateTicketStatusRequest {
        resolutionSummary = resolutionSummary == null ? null : resolutionSummary.trim();
    }
}
