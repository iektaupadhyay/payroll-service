
package com.employee_managemet.payroll_service.dto.request;

import com.employee_managemet.payroll_service.enums.DeductionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PayrollDeductionRequest(

        @NotNull(message = "Deduction type is required")
        DeductionType deductionType,

        @NotNull(message = "Deduction amount is required")
        @DecimalMin(value = "0.0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Size(max = 255)
        String description

) {}
