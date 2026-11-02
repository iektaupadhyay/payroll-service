package com.employee_managemet.payroll_service.dto.response;

import com.employee_managemet.payroll_service.enums.DeductionType;

import java.math.BigDecimal;

public record PayrollDeductionResponse(
        Long id,
        DeductionType deductionType,
        BigDecimal amount,
        String description
) {}