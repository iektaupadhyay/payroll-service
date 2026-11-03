package com.employee_managemet.payroll_service.service;

import com.employee_managemet.payroll_service.client.EmployeeServiceClient;
import com.employee_managemet.payroll_service.dto.integration.EmployeeResponse;
import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.enums.DeductionType;
import com.employee_managemet.payroll_service.enums.PayrollStatus;
import com.employee_managemet.payroll_service.exception.BusinessRuleViolationException;
import com.employee_managemet.payroll_service.exception.DuplicatePayrollException;
import com.employee_managemet.payroll_service.exception.InvalidPayrollStatusTransitionException;
import com.employee_managemet.payroll_service.exception.PayrollNotFoundException;
import com.employee_managemet.payroll_service.mapper.PayrollDeductionMapper;
import com.employee_managemet.payroll_service.mapper.PayrollLeaveDetailsMapper;
import com.employee_managemet.payroll_service.mapper.PayrollMapper;
import com.employee_managemet.payroll_service.model.Payroll;
import com.employee_managemet.payroll_service.model.PayrollDeduction;
import com.employee_managemet.payroll_service.repository.PayrollRepository;
import com.employee_managemet.payroll_service.service.impl.PayrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock private PayrollRepository payrollRepository;
    @Mock private EmployeeServiceClient employeeServiceClient;

    private PayrollServiceImpl payrollService;
    private PayrollMapper payrollMapper;

    @BeforeEach
    void setUp() {
        payrollMapper = new PayrollMapper(
                new PayrollDeductionMapper(), new PayrollLeaveDetailsMapper());
        payrollService = new PayrollServiceImpl(
                payrollRepository, employeeServiceClient, payrollMapper);
    }

    @Test
    void createPayroll_shouldFetchEmployeeCalculateGrossAndSaveDraft() {
        PayrollRequest request = request("50000", "10000", "2000", "3000", "1000");
        when(employeeServiceClient.getEmployeeById(10L)).thenReturn(employee());
        when(payrollRepository.findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(10L, 11, 2026))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(invocation -> {
            Payroll payroll = invocation.getArgument(0);
            payroll.setId(91L);
            return payroll;
        });

        PayrollResponse response = payrollService.createPayroll(request);

        assertEquals(91L, response.id());
        assertEquals(10L, response.employeeId());
        assertEquals("EMP-010", response.employeeCode());
        assertEquals("Test Employee", response.employeeName());
        assertEquals(new BigDecimal("66000"), response.grossSalary());
        assertEquals(new BigDecimal("66000"), response.netSalary());
        assertEquals(BigDecimal.ZERO, response.totalDeductions());
        assertEquals(PayrollStatus.DRAFT, response.paymentStatus());
        assertEquals("**********1234", response.maskedAccountNumber());

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getEmployeeId());
        assertEquals("EMP-010", captor.getValue().getEmployeeCode());
        assertEquals("PERMANENT", captor.getValue().getEmploymentType());
        assertEquals(BigDecimal.ZERO, captor.getValue().getEmployerPf());
    }

    @Test
    void createPayroll_shouldDefaultOptionalBonusAndAllowanceToZero() {
        PayrollRequest request = request("50000", "10000", null, null, null);
        when(employeeServiceClient.getEmployeeById(10L)).thenReturn(employee());
        when(payrollRepository.findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(10L, 11, 2026))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollResponse response = payrollService.createPayroll(request);

        assertEquals(new BigDecimal("60000"), response.grossSalary());
        assertEquals(BigDecimal.ZERO, response.fixedBonus());
        assertEquals(BigDecimal.ZERO, response.variableBonus());
        assertEquals(BigDecimal.ZERO, response.otherAllowances());
    }

    @Test
    void createPayroll_shouldRejectDuplicateEmployeeAndPeriod() {
        when(employeeServiceClient.getEmployeeById(10L)).thenReturn(employee());
        when(payrollRepository.findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(10L, 11, 2026))
                .thenReturn(Optional.of(payroll(1L, PayrollStatus.DRAFT)));

        assertThrows(DuplicatePayrollException.class,
                () -> payrollService.createPayroll(request("50000", "10000", null, null, null)));
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void createPayroll_shouldPropagateEmployeeServiceFailure() {
        when(employeeServiceClient.getEmployeeById(10L))
                .thenThrow(new RuntimeException("upstream unavailable"));

        assertThrows(RuntimeException.class,
                () -> payrollService.createPayroll(request("50000", "10000", null, null, null)));
        verifyNoInteractions(payrollRepository);
    }

    @Test
    void getPayrollById_shouldReturnMappedPayroll() {
        when(payrollRepository.findById(5L)).thenReturn(Optional.of(payroll(5L, PayrollStatus.DRAFT)));

        PayrollResponse response = payrollService.getPayrollById(5L);

        assertEquals(5L, response.id());
        assertEquals("EMP-010", response.employeeCode());
    }

    @Test
    void getPayrollById_shouldThrowWhenMissing() {
        when(payrollRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(PayrollNotFoundException.class, () -> payrollService.getPayrollById(404L));
    }

    @Test
    void getEmployeePayrollHistory_shouldMapRepositoryResults() {
        when(payrollRepository.findByEmployeeId(10L)).thenReturn(List.of(
                payroll(1L, PayrollStatus.CREDITED), payroll(2L, PayrollStatus.DRAFT)));

        var result = payrollService.getEmployeePayrollHistory(10L);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).employeeId());
        assertEquals(PayrollStatus.CREDITED, result.get(0).paymentStatus());
        verify(payrollRepository).findByEmployeeId(10L);
    }

    @Test
    void getPayrollByPeriod_shouldMapRepositoryResults() {
        when(payrollRepository.findByPayPeriodMonthAndPayPeriodYear(11, 2026))
                .thenReturn(List.of(payroll(1L, PayrollStatus.PENDING)));

        var result = payrollService.getPayrollByPeriod(11, 2026);

        assertEquals(1, result.size());
        assertEquals(11, result.getFirst().payPeriodMonth());
        assertEquals(2026, result.getFirst().payPeriodYear());
    }

    @Test
    void addDeduction_shouldUpdateTotalsNetAndBothSidesOfRelationship() {
        Payroll payroll = payroll(7L, PayrollStatus.DRAFT);
        payroll.setGrossSalary(new BigDecimal("66000"));
        payroll.setTotalDeductions(new BigDecimal("1000"));
        payroll.setNetSalary(new BigDecimal("65000"));
        when(payrollRepository.findById(7L)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollResponse response = payrollService.addDeduction(7L,
                new PayrollDeductionRequest(DeductionType.INCOME_TAX,
                        new BigDecimal("2000"), "Monthly tax"));

        assertEquals(new BigDecimal("3000"), response.totalDeductions());
        assertEquals(new BigDecimal("63000"), response.netSalary());
        assertEquals(1, response.deductions().size());
        assertEquals(DeductionType.INCOME_TAX, response.deductions().getFirst().deductionType());
        PayrollDeduction deduction = payroll.getDeductions().getFirst();
        assertSame(payroll, deduction.getPayroll());
        verify(payrollRepository).save(payroll);
    }

    @Test
    void addDeduction_shouldRejectDeductionThatMakesNetSalaryNegative() {
        Payroll payroll = payroll(7L, PayrollStatus.DRAFT);
        payroll.setGrossSalary(new BigDecimal("1000"));
        payroll.setTotalDeductions(BigDecimal.ZERO);
        when(payrollRepository.findById(7L)).thenReturn(Optional.of(payroll));

        assertThrows(BusinessRuleViolationException.class,
                () -> payrollService.addDeduction(7L,
                        new PayrollDeductionRequest(DeductionType.OTHER,
                                new BigDecimal("1000.01"), "Excess deduction")));
        verify(payrollRepository, never()).save(any());
        assertTrue(payroll.getDeductions().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("lockedStatuses")
    void addDeduction_shouldRejectLockedPayrollStatuses(PayrollStatus status) {
        when(payrollRepository.findById(7L)).thenReturn(Optional.of(payroll(7L, status)));

        assertThrows(BusinessRuleViolationException.class,
                () -> payrollService.addDeduction(7L,
                        new PayrollDeductionRequest(DeductionType.OTHER,
                                BigDecimal.ONE, "Test")));
        verify(payrollRepository, never()).save(any());
    }

    static Stream<PayrollStatus> lockedStatuses() {
        return Stream.of(PayrollStatus.PROCESSING, PayrollStatus.FAILED,
                PayrollStatus.CREDITED, PayrollStatus.CANCELLED);
    }

    @Test
    void addDeduction_shouldThrowWhenPayrollDoesNotExist() {
        when(payrollRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(PayrollNotFoundException.class,
                () -> payrollService.addDeduction(999L,
                        new PayrollDeductionRequest(DeductionType.OTHER, BigDecimal.ONE, null)));
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void updatePayrollStatus_shouldAllowValidTransitions(
            PayrollStatus current, PayrollStatus target) {
        Payroll payroll = payroll(8L, current);
        when(payrollRepository.findById(8L)).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollResponse response = payrollService.updatePayrollStatus(8L, target);

        assertEquals(target, response.paymentStatus());
        verify(payrollRepository).save(payroll);
        if (target == PayrollStatus.CREDITED) {
            assertNotNull(response.creditedAt());
            assertTrue(response.creditedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        } else {
            assertNull(response.creditedAt());
        }
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(PayrollStatus.DRAFT, PayrollStatus.PENDING),
                Arguments.of(PayrollStatus.DRAFT, PayrollStatus.CANCELLED),
                Arguments.of(PayrollStatus.PENDING, PayrollStatus.PROCESSING),
                Arguments.of(PayrollStatus.PENDING, PayrollStatus.CANCELLED),
                Arguments.of(PayrollStatus.PROCESSING, PayrollStatus.CREDITED),
                Arguments.of(PayrollStatus.PROCESSING, PayrollStatus.FAILED),
                Arguments.of(PayrollStatus.FAILED, PayrollStatus.PROCESSING)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void updatePayrollStatus_shouldRejectInvalidTransitions(
            PayrollStatus current, PayrollStatus target) {
        when(payrollRepository.findById(8L)).thenReturn(Optional.of(payroll(8L, current)));

        assertThrows(InvalidPayrollStatusTransitionException.class,
                () -> payrollService.updatePayrollStatus(8L, target));
        verify(payrollRepository, never()).save(any());
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(PayrollStatus.DRAFT, PayrollStatus.PROCESSING),
                Arguments.of(PayrollStatus.DRAFT, PayrollStatus.CREDITED),
                Arguments.of(PayrollStatus.PENDING, PayrollStatus.DRAFT),
                Arguments.of(PayrollStatus.PROCESSING, PayrollStatus.PENDING),
                Arguments.of(PayrollStatus.FAILED, PayrollStatus.CREDITED),
                Arguments.of(PayrollStatus.CREDITED, PayrollStatus.PROCESSING),
                Arguments.of(PayrollStatus.CANCELLED, PayrollStatus.PENDING)
        );
    }

    @Test
    void updatePayrollStatus_shouldRejectNullTargetStatus() {
        when(payrollRepository.findById(8L)).thenReturn(Optional.of(payroll(8L, PayrollStatus.DRAFT)));

        assertThrows(InvalidPayrollStatusTransitionException.class,
                () -> payrollService.updatePayrollStatus(8L, null));
        verify(payrollRepository, never()).save(any());
    }

    @Test
    void updatePayrollStatus_shouldThrowWhenPayrollDoesNotExist() {
        when(payrollRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(PayrollNotFoundException.class,
                () -> payrollService.updatePayrollStatus(999L, PayrollStatus.PENDING));
    }

    private static PayrollRequest request(String basic, String hra,
                                          String fixed, String variable, String allowance) {
        return new PayrollRequest(10L, 11, 2026, new BigDecimal("720000"),
                new BigDecimal(basic), new BigDecimal(hra), decimal(fixed),
                decimal(variable), decimal(allowance), "12345678901234");
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static EmployeeResponse employee() {
        return new EmployeeResponse(10L, "EMP-010", "Test Employee", "MALE",
                "PERMANENT", "ACTIVE", null, "9999999999", "test@example.com",
                "Engineer", 2L, "Engineering", null);
    }

    private static Payroll payroll(Long id, PayrollStatus status) {
        Payroll payroll = new Payroll();
        payroll.setId(id);
        payroll.setEmployeeId(10L);
        payroll.setEmployeeCode("EMP-010");
        payroll.setEmployeeName("Test Employee");
        payroll.setEmploymentType("PERMANENT");
        payroll.setBankAccountNumber("12345678901234");
        payroll.setPayPeriodMonth(11);
        payroll.setPayPeriodYear(2026);
        payroll.setCtc(new BigDecimal("720000"));
        payroll.setBasicSalary(new BigDecimal("50000"));
        payroll.setHra(new BigDecimal("10000"));
        payroll.setFixedBonus(BigDecimal.ZERO);
        payroll.setVariableBonus(BigDecimal.ZERO);
        payroll.setOtherAllowances(BigDecimal.ZERO);
        payroll.setGrossSalary(new BigDecimal("60000"));
        payroll.setEmployerPf(BigDecimal.ZERO);
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(new BigDecimal("60000"));
        payroll.setPaymentStatus(status);
        payroll.setCreatedAt(LocalDateTime.now());
        payroll.setUpdatedAt(LocalDateTime.now());
        return payroll;
    }
}
