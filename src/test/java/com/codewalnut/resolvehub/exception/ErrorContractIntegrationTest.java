package com.codewalnut.resolvehub.exception;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.codewalnut.resolvehub.security.TraceIdFilter;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorContractIntegrationTest {
    @RestController
    static class FailingController {
        @GetMapping("/failure")
        String fail() {
            throw new IllegalStateException("SQL password=DO-NOT-EXPOSE");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new org.springframework.dao.OptimisticLockingFailureException("SQL DO-NOT-EXPOSE");
        }
    }

    @Test
    void givenUnexpectedFailure_whenHandled_thenReturnSanitizedTraceableError() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))))
                .addFilters(new TraceIdFilter()).build();
        mvc.perform(get("/failure").header("X-Trace-Id", "bd9a3bfb-a9f8-4d84-8ae7-e42d1e038bc2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.traceId").value("bd9a3bfb-a9f8-4d84-8ae7-e42d1e038bc2"))
                .andExpect(jsonPath("$.path").value("/failure"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void givenOptimisticConflict_whenHandled_thenReturnSanitizedConflict() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory(Clock.systemUTC())))
                .addFilters(new TraceIdFilter()).build();
        mvc.perform(get("/conflict")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ticket was changed by another request; reload and retry"));
    }
}
