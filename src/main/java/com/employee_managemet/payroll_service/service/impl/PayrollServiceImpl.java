package com.employee_managemet.payroll_service.service.impl;

import com.employee_managemet.payroll_service.client.EmployeeServiceClient;

import com.employee_managemet.payroll_service.dto.integration.EmployeeResponse;
import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;

import com.employee_managemet.payroll_service.enums.PayrollStatus;

import com.employee_managemet.payroll_service.exception.BusinessRuleViolationException;
import com.employee_managemet.payroll_service.exception.DuplicatePayrollException;
import com.employee_managemet.payroll_service.exception.InvalidPayrollStatusTransitionException;
import com.employee_managemet.payroll_service.exception.PayrollNotFoundException;

import com.employee_managemet.payroll_service.mapper.PayrollMapper;

import com.employee_managemet.payroll_service.model.Payroll;
import com.employee_managemet.payroll_service.model.PayrollDeduction;

import com.employee_managemet.payroll_service.repository.PayrollRepository;

import com.employee_managemet.payroll_service.service.PayrollService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private static final Logger log =
            LoggerFactory.getLogger(PayrollServiceImpl.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeServiceClient employeeServiceClient;
    private final PayrollMapper payrollMapper;

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {

        log.info(
                "Creating payroll | employeeId={} | period={}/{}",
                request.employeeId(),
                request.payPeriodMonth(),
                request.payPeriodYear()
        );

        // 1. Fetch employee from Employee Service
        EmployeeResponse employee =
                employeeServiceClient.getEmployeeById(request.employeeId());

        // 2. Check for duplicate payroll
        payrollRepository
                .findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(
                        request.employeeId(),
                        request.payPeriodMonth(),
                        request.payPeriodYear()
                )
                .ifPresent(existing -> {
                    throw new DuplicatePayrollException(
                            "Payroll already exists for employee ID "
                                    + request.employeeId()
                                    + " for "
                                    + request.payPeriodMonth()
                                    + "/"
                                    + request.payPeriodYear()
                    );
                });

        // 3. Create payroll
        Payroll payroll = new Payroll();

        payroll.setEmployeeId(employee.id());
        payroll.setEmployeeCode(employee.employeeCode());
        payroll.setEmployeeName(employee.name());
        payroll.setEmploymentType(employee.employmentType());

        payroll.setBankAccountNumber(request.bankAccountNumber());

        payroll.setPayPeriodMonth(request.payPeriodMonth());
        payroll.setPayPeriodYear(request.payPeriodYear());

        payroll.setCtc(request.ctc());
        payroll.setBasicSalary(request.basicSalary());
        payroll.setHra(request.hra());

        payroll.setFixedBonus(defaultAmount(request.fixedBonus()));
        payroll.setVariableBonus(defaultAmount(request.variableBonus()));
        payroll.setOtherAllowances(defaultAmount(request.otherAllowances()));

        // 4. Calculate initial salary
        BigDecimal grossSalary = calculateGrossSalary(request);

        payroll.setGrossSalary(grossSalary);
        payroll.setEmployerPf(BigDecimal.ZERO);
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(grossSalary);

        // 5. Initial status
        payroll.setPaymentStatus(PayrollStatus.DRAFT);

        // 6. Persist
        Payroll savedPayroll = payrollRepository.save(payroll);

        log.info(
                "Payroll created | payrollId={} | employeeId={} | status={}",
                savedPayroll.getId(),
                employee.id(),
                savedPayroll.getPaymentStatus()
        );

        return payrollMapper.toResponse(savedPayroll);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollById(Long payrollId) {

        log.debug("Fetching payroll | payrollId={}", payrollId);

        Payroll payroll = findPayrollById(payrollId);

        return payrollMapper.toResponse(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getEmployeePayrollHistory(
            Long employeeId
    ) {

        log.debug("Fetching payroll history | employeeId={}", employeeId);

        return payrollRepository.findByEmployeeId(employeeId)
                .stream()
                .map(payrollMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getPayrollByPeriod(
            Integer month,
            Integer year
    ) {

        log.debug(
                "Fetching payroll by period | month={} | year={}",
                month,
                year
        );

        return payrollRepository
                .findByPayPeriodMonthAndPayPeriodYear(month, year)
                .stream()
                .map(payrollMapper::toSummaryResponse)
                .toList();
    }

    @Override
    public PayrollResponse addDeduction(
            Long payrollId,
            PayrollDeductionRequest request
    ) {

        log.info(
                "Adding deduction | payrollId={} | type={}",
                payrollId,
                request.deductionType()
        );

        Payroll payroll = findPayrollById(payrollId);

        // Deductions can only be modified before processing.
        ensurePayrollIsEditable(payroll);

        BigDecimal currentDeductions =
                defaultAmount(payroll.getTotalDeductions());

        BigDecimal updatedDeductions =
                currentDeductions.add(request.amount());

        BigDecimal grossSalary =
                defaultAmount(payroll.getGrossSalary());

        BigDecimal updatedNetSalary =
                grossSalary.subtract(updatedDeductions);

        if (updatedNetSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "Total deductions cannot exceed gross salary."
            );
        }

        PayrollDeduction deduction = new PayrollDeduction();

        deduction.setDeductionType(request.deductionType());
        deduction.setAmount(request.amount());
        deduction.setDescription(request.description());

        payroll.addDeduction(deduction);

        payroll.setTotalDeductions(updatedDeductions);
        payroll.setNetSalary(updatedNetSalary);

        Payroll updatedPayroll = payrollRepository.save(payroll);

        log.info(
                "Deduction added | payrollId={} | deductionId={}",
                updatedPayroll.getId(),
                deduction.getId()
        );

        return payrollMapper.toResponse(updatedPayroll);
    }

    @Override
    public PayrollResponse updatePayrollStatus(
            Long payrollId,
            PayrollStatus newStatus
    ) {

        log.info(
                "Updating payroll status | payrollId={} | targetStatus={}",
                payrollId,
                newStatus
        );

        // 1. Fetch payroll
        Payroll payroll = findPayrollById(payrollId);

        // 2. Read current status
        PayrollStatus currentStatus = payroll.getPaymentStatus();

        // 3. Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {

            log.warn(
                    "Invalid payroll status transition | payrollId={} | from={} | to={}",
                    payrollId,
                    currentStatus,
                    newStatus
            );

            throw new InvalidPayrollStatusTransitionException(
                    "Cannot change payroll status from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }

        // 4. Update status
        payroll.setPaymentStatus(newStatus);

        // 5. Maintain credit timestamp
        if (newStatus == PayrollStatus.CREDITED) {
            payroll.setCreditedAt(LocalDateTime.now());
        } else {
            payroll.setCreditedAt(null);
        }

        // 6. Persist
        Payroll updatedPayroll = payrollRepository.save(payroll);

        log.info(
                "Payroll status updated successfully | payrollId={} | from={} | to={}",
                payrollId,
                currentStatus,
                newStatus
        );

        return payrollMapper.toResponse(updatedPayroll);
    }

    private boolean isValidTransition(
            PayrollStatus currentStatus,
            PayrollStatus newStatus
    ) {

        if (currentStatus == null || newStatus == null) {
            return false;
        }

        return switch (currentStatus) {

            case DRAFT ->
                    newStatus == PayrollStatus.PENDING
                            || newStatus == PayrollStatus.CANCELLED;

            case PENDING ->
                    newStatus == PayrollStatus.PROCESSING
                            || newStatus == PayrollStatus.CANCELLED;

            case PROCESSING ->
                    newStatus == PayrollStatus.CREDITED
                            || newStatus == PayrollStatus.FAILED;

            case FAILED ->
                    newStatus == PayrollStatus.PROCESSING;

            case CREDITED, CANCELLED -> false;
        };
    }

    private void ensurePayrollIsEditable(Payroll payroll) {

        PayrollStatus status = payroll.getPaymentStatus();

        if (status != PayrollStatus.DRAFT
                && status != PayrollStatus.PENDING) {

            throw new BusinessRuleViolationException(
                    "Payroll cannot be modified when its status is " + status
            );
        }
    }

    private Payroll findPayrollById(Long payrollId) {

        return payrollRepository.findById(payrollId)
                .orElseThrow(() -> new PayrollNotFoundException(
                        "Payroll not found with ID: " + payrollId
                ));
    }

    private BigDecimal calculateGrossSalary(PayrollRequest request) {

        return defaultAmount(request.basicSalary())
                .add(defaultAmount(request.hra()))
                .add(defaultAmount(request.fixedBonus()))
                .add(defaultAmount(request.variableBonus()))
                .add(defaultAmount(request.otherAllowances()));
    }

    private BigDecimal defaultAmount(BigDecimal amount) {

        return amount == null ? BigDecimal.ZERO : amount;
    }
}