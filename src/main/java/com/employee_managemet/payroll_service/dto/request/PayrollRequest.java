
package com.employee_managemet.payroll_service.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PayrollRequest(

        @NotNull(message = "Employee ID is required")
        @Positive(message = "Employee ID must be positive")
        Long employeeId,

        @NotNull(message = "Payroll month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer payPeriodMonth,

        @NotNull(message = "Payroll year is required")
        @Min(value = 2000, message = "Invalid payroll year")
        Integer payPeriodYear,

        @NotNull(message = "CTC is required")
        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal ctc,

        @NotNull(message = "Basic salary is required")
        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal basicSalary,

        @NotNull(message = "HRA is required")
        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal hra,

        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal fixedBonus,

        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal variableBonus,

        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal otherAllowances,

        @NotBlank(message = "Bank account number is required")
        @Pattern(
                regexp = "^[0-9]{9,18}$",
                message = "Invalid bank account number"
        )
        String bankAccountNumber

) {}
