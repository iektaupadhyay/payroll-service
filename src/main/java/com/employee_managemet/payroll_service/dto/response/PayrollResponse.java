package com.employee_managemet.payroll_service.dto.response;

import com.employee_managemet.payroll_service.enums.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollResponse(

        Long id,
        Long employeeId,
        String employeeName,
        String employmentType,
        String maskedAccountNumber,

        Integer payPeriodMonth,
        Integer payPeriodYear,

        BigDecimal ctc,
        BigDecimal basicSalary,
        BigDecimal hra,
        BigDecimal fixedBonus,
        BigDecimal variableBonus,
        BigDecimal otherAllowances,

        BigDecimal grossSalary,
        BigDecimal employerPf,
        BigDecimal totalDeductions,
        BigDecimal netSalary,

        PayrollStatus paymentStatus,
        LocalDateTime creditedAt,

        List<PayrollDeductionResponse> deductions,
        List<PayrollLeaveDetailsResponse> leaveDetails,

        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}