package com.employee_managemet.payroll_service.dto;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollStatusUpdateRequest;
import com.employee_managemet.payroll_service.enums.DeductionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PayrollRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void payrollRequest_shouldPassWhenAllRequiredFieldsAreValid() {
        PayrollRequest request = new PayrollRequest(10L, 11, 2026,
                new BigDecimal("720000.00"), new BigDecimal("50000.00"),
                new BigDecimal("10000.00"), null, null, null, "12345678901234");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void payrollRequest_shouldRejectInvalidRequiredFields() {
        PayrollRequest request = new PayrollRequest(-10L, 13, 1999,
                new BigDecimal("-1"), new BigDecimal("-1"), null,
                null, null, null, "123");

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("employeeId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("payPeriodMonth")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("payPeriodYear")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ctc")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("bankAccountNumber")));
    }

    @Test
    void payrollRequest_shouldRejectAccountNumberOutsideAllowedLengthOrFormat() {
        PayrollRequest shortAccount = new PayrollRequest(10L, 11, 2026,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                null, null, null, "12345678");
        PayrollRequest nonNumericAccount = new PayrollRequest(10L, 11, 2026,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                null, null, null, "12345678AB");

        assertTrue(validator.validate(shortAccount).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("bankAccountNumber")));
        assertTrue(validator.validate(nonNumericAccount).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("bankAccountNumber")));
    }

    @Test
    void deductionRequest_shouldRejectMissingTypeAndNegativeAmount() {
        PayrollDeductionRequest request = new PayrollDeductionRequest(null,
                new BigDecimal("-10"), "Invalid");

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("deductionType")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void statusUpdateRequest_shouldRejectNullStatus() {
        assertEquals(1, validator.validate(new PayrollStatusUpdateRequest(null)).size());
    }
}
