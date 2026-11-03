package com.employee_managemet.payroll_service.dto.request;

import com.employee_managemet.payroll_service.enums.PayrollStatus;
import jakarta.validation.constraints.NotNull;

public record PayrollStatusUpdateRequest(

        @NotNull(message = "Payroll status is required")
        PayrollStatus status

) {}