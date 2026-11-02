package com.employee_managemet.payroll_service.dto.response;

import com.employee_managemet.payroll_service.enums.PayrollStatus;

import java.math.BigDecimal;

public record PayrollSummaryResponse(

        Long id,

        Long employeeId,
        String employeeCode,
        String employeeName,

        Integer payPeriodMonth,
        Integer payPeriodYear,

        BigDecimal grossSalary,
        BigDecimal totalDeductions,
        BigDecimal netSalary,

        PayrollStatus paymentStatus

) {}