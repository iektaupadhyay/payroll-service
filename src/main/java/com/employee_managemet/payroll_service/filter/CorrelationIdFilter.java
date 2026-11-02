package com.employee_managemet.payroll_service.filter;

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

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private static final String CORRELATION_ID_MDC = "correlationId";

    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("^[a-zA-Z0-9._-]{1,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = getCorrelationId(request);

        long startTime = System.currentTimeMillis();

        MDC.put(CORRELATION_ID_MDC, correlationId);

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {

            filterChain.doFilter(request, response);

        } finally {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.info(
                    "HTTP request completed | method={} | path={} | status={} | durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    executionTime
            );

            MDC.remove(CORRELATION_ID_MDC);
        }
    }

    private String getCorrelationId(HttpServletRequest request) {

        String correlationId =
                request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId != null
                && VALID_CORRELATION_ID.matcher(correlationId).matches()) {

            return correlationId;
        }

        return UUID.randomUUID().toString();
    }
}