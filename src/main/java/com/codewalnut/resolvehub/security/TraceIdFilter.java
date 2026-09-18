package com.codewalnut.resolvehub.security;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader("X-Trace-Id");
        String trace = incoming != null && incoming.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                ? incoming : UUID.randomUUID().toString();
        request.setAttribute("traceId", trace);
        response.setHeader("X-Trace-Id", trace);
        MDC.put("traceId", trace);
        long started = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            try {
                LOG.info("request traceId={} method={} path={} status={} durationMs={} username={}", trace,
                        safe(request.getMethod()), safe(request.getRequestURI()), failed ? 500 : response.getStatus(),
                        (System.nanoTime() - started) / 1_000_000,
                        safe(request.getAttribute("authenticatedUsername")));
            } finally {
                MDC.remove("traceId");
            }
        }
    }

    private String safe(Object value) {
        return value == null ? "anonymous" : value.toString().replaceAll("[\\p{Cntrl}]", "_");
    }
}
