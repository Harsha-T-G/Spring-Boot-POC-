package com.codewalnut.resolvehub.security;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class TraceIdIntegrationTest {
    @Test
    void givenValidTrace_whenRequestCompletes_thenRetainHeaderAndClearMdc() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/info");
        String trace = "bd9a3bfb-a9f8-4d84-8ae7-e42d1e038bc2";
        request.addHeader("X-Trace-Id", trace);
        var response = new MockHttpServletResponse();
        new TraceIdFilter().doFilter(request, response, (incoming, outgoing) ->
                assertThat(MDC.get("traceId")).isEqualTo(trace));

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(trace);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void givenInvalidTrace_whenRequestFails_thenGenerateUuidAndClearMdc() {
        var request = new MockHttpServletRequest("GET", "/api/info");
        request.addHeader("X-Trace-Id", "1-1-1-1-1");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(() -> new TraceIdFilter().doFilter(request, response, (incoming, outgoing) -> {
            throw new jakarta.servlet.ServletException("failure");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(UUID.fromString(response.getHeader("X-Trace-Id")).toString())
                .isEqualTo(response.getHeader("X-Trace-Id"));
        assertThat(MDC.get("traceId")).isNull();
    }
}
