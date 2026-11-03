package com.employee_managemet.payroll_service.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void shouldReuseValidIncomingCorrelationIdAndEchoItInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payroll/1");
        request.addHeader("X-Correlation-ID", "trace-123.ABC_9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValue = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> mdcValue.set(MDC.get("correlationId")));

        assertEquals("trace-123.ABC_9", response.getHeader("X-Correlation-ID"));
        assertEquals("trace-123.ABC_9", mdcValue.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payroll");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String generated = response.getHeader("X-Correlation-ID");
        assertNotNull(generated);
        assertDoesNotThrow(() -> UUID.fromString(generated));
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenIncomingHeaderIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payroll");
        request.addHeader("X-Correlation-ID", "invalid id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String generated = response.getHeader("X-Correlation-ID");
        assertNotEquals("invalid id with spaces", generated);
        assertDoesNotThrow(() -> UUID.fromString(generated));
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenHeaderExceeds64Characters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payroll");
        request.addHeader("X-Correlation-ID", "x".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertDoesNotThrow(() -> UUID.fromString(response.getHeader("X-Correlation-ID")));
    }

    @Test
    void shouldClearMdcWhenFilterChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payroll");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () -> filter.doFilter(request, response,
                (req, res) -> { throw new ServletException("downstream failure"); }));
        assertNull(MDC.get("correlationId"));
    }
}
