package com.employee_managemet.payroll_service.dto.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(

        Instant timestamp,

        int status,

        String error,

        String message,

        String path,

        String correlationId,

        Map<String, String> validationErrors

) {}