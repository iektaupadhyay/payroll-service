package com.employee_managemet.payroll_service.mapper;

import com.employee_managemet.payroll_service.enums.DeductionType;
import com.employee_managemet.payroll_service.enums.PayrollStatus;
import com.employee_managemet.payroll_service.model.Payroll;
import com.employee_managemet.payroll_service.model.PayrollDeduction;
import com.employee_managemet.payroll_service.model.PayrollLeaveDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PayrollMapperTest {

    private PayrollMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PayrollMapper(new PayrollDeductionMapper(), new PayrollLeaveDetailsMapper());
    }

    @Test
    void toResponse_shouldMapAllPayrollFieldsAndMaskBankAccount() {
        Payroll payroll = payroll();
        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setId(31L);
        deduction.setDeductionType(DeductionType.INCOME_TAX);
        deduction.setAmount(new BigDecimal("1500.00"));
        deduction.setDescription("Tax");
        payroll.addDeduction(deduction);

        PayrollLeaveDetails leave = new PayrollLeaveDetails();
        leave.setId(41L);
        leave.setLeaveType("CASUAL");
        leave.setTotalLeaves(2);
        leave.setAppliedDate(LocalDate.of(2026, 11, 1));
        payroll.addLeaveDetails(leave);

        var response = mapper.toResponse(payroll);

        assertEquals(1L, response.id());
        assertEquals(10L, response.employeeId());
        assertEquals("EMP-010", response.employeeCode());
        assertEquals("**********1234", response.maskedAccountNumber());
        assertEquals(PayrollStatus.DRAFT, response.paymentStatus());
        assertEquals(1, response.deductions().size());
        assertEquals(DeductionType.INCOME_TAX, response.deductions().getFirst().deductionType());
        assertEquals(1, response.leaveDetails().size());
        assertEquals("CASUAL", response.leaveDetails().getFirst().leaveType());
        assertNotEquals("12345678901234", response.maskedAccountNumber());
    }

    @Test
    void toResponse_shouldReturnEmptyCollectionsWhenCollectionsAreNull() {
        Payroll payroll = payroll();
        payroll.setDeductions(null);
        payroll.setLeaveDetails(null);

        var response = mapper.toResponse(payroll);

        assertNotNull(response.deductions());
        assertTrue(response.deductions().isEmpty());
        assertNotNull(response.leaveDetails());
        assertTrue(response.leaveDetails().isEmpty());
    }

    @Test
    void toResponse_shouldMaskNullAndShortAccountNumbers() {
        Payroll payroll = payroll();
        payroll.setBankAccountNumber(null);
        assertEquals("****", mapper.toResponse(payroll).maskedAccountNumber());

        payroll.setBankAccountNumber("1234");
        assertEquals("****", mapper.toResponse(payroll).maskedAccountNumber());
    }

    @Test
    void toResponse_shouldMaskLongAccountNumberKeepingLastFourDigits() {
        Payroll payroll = payroll();
        payroll.setBankAccountNumber("123456789012345678");
        assertEquals("**************5678", mapper.toResponse(payroll).maskedAccountNumber());
    }

    @Test
    void toSummaryResponse_shouldMapSummaryFieldsOnly() {
        var response = mapper.toSummaryResponse(payroll());

        assertEquals(1L, response.id());
        assertEquals(10L, response.employeeId());
        assertEquals("EMP-010", response.employeeCode());
        assertEquals("Test Employee", response.employeeName());
        assertEquals(11, response.payPeriodMonth());
        assertEquals(2026, response.payPeriodYear());
        assertEquals(new BigDecimal("60000.00"), response.grossSalary());
        assertEquals(PayrollStatus.DRAFT, response.paymentStatus());
    }

    @Test
    void deductionMapper_shouldMapRequestAndEntity() {
        PayrollDeductionMapper deductionMapper = new PayrollDeductionMapper();
        var entity = deductionMapper.toEntity(new com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest(
                DeductionType.OTHER, new BigDecimal("250.00"), "Other"));

        assertEquals(DeductionType.OTHER, entity.getDeductionType());
        assertEquals(new BigDecimal("250.00"), entity.getAmount());
        assertEquals("Other", entity.getDescription());

        entity.setId(9L);
        var response = deductionMapper.toResponse(entity);
        assertEquals(9L, response.id());
        assertEquals(DeductionType.OTHER, response.deductionType());
    }

    @Test
    void leaveDetailsMapper_shouldMapEntity() {
        PayrollLeaveDetailsMapper leaveMapper = new PayrollLeaveDetailsMapper();
        PayrollLeaveDetails leave = new PayrollLeaveDetails();
        leave.setId(2L);
        leave.setLeaveType("SICK");
        leave.setTotalLeaves(1);
        leave.setAppliedDate(LocalDate.of(2026, 10, 20));

        var response = leaveMapper.toResponse(leave);

        assertEquals(2L, response.id());
        assertEquals("SICK", response.leaveType());
        assertEquals(1, response.totalLeaves());
        assertEquals(LocalDate.of(2026, 10, 20), response.appliedDate());
    }

    private static Payroll payroll() {
        Payroll payroll = new Payroll();
        payroll.setId(1L);
        payroll.setEmployeeId(10L);
        payroll.setEmployeeCode("EMP-010");
        payroll.setEmployeeName("Test Employee");
        payroll.setEmploymentType("PERMANENT");
        payroll.setBankAccountNumber("12345678901234");
        payroll.setPayPeriodMonth(11);
        payroll.setPayPeriodYear(2026);
        payroll.setCtc(new BigDecimal("720000.00"));
        payroll.setBasicSalary(new BigDecimal("50000.00"));
        payroll.setHra(new BigDecimal("10000.00"));
        payroll.setFixedBonus(BigDecimal.ZERO);
        payroll.setVariableBonus(BigDecimal.ZERO);
        payroll.setOtherAllowances(BigDecimal.ZERO);
        payroll.setGrossSalary(new BigDecimal("60000.00"));
        payroll.setEmployerPf(BigDecimal.ZERO);
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(new BigDecimal("60000.00"));
        payroll.setPaymentStatus(PayrollStatus.DRAFT);
        payroll.setCreatedAt(LocalDateTime.now());
        payroll.setUpdatedAt(LocalDateTime.now());
        payroll.setDeductions(new ArrayList<>());
        payroll.setLeaveDetails(new ArrayList<>());
        return payroll;
    }
}
