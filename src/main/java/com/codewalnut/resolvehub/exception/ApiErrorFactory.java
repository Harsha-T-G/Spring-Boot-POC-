package com.codewalnut.resolvehub.exception;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.codewalnut.resolvehub.dto.ApiErrorResponse;

@Component
public class ApiErrorFactory {
    private final Clock clock;

    public ApiErrorFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiErrorResponse create(HttpStatus status, String message, Map<String, String> fields,
            HttpServletRequest request) {
        Object trace = request.getAttribute("traceId");
        if (trace == null) {
            trace = UUID.randomUUID().toString();
            request.setAttribute("traceId", trace);
        }
        return new ApiErrorResponse(clock.instant(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), trace.toString(), fields);
    }
}
