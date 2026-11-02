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

        // 2. Check whether payroll already exists
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

        // 3. Create payroll entity
        Payroll payroll = new Payroll();

        payroll.setEmployeeId(employee.id());
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

        // 4. Calculate gross salary
        BigDecimal grossSalary = calculateGrossSalary(request);

        payroll.setGrossSalary(grossSalary);

        // Employer PF is maintained separately.
        // Statutory calculation will be implemented later.
        payroll.setEmployerPf(BigDecimal.ZERO);

        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(grossSalary);

        payroll.setPaymentStatus(PayrollStatus.DRAFT);

        // 5. Save payroll
        Payroll savedPayroll = payrollRepository.save(payroll);

        log.info(
                "Payroll created successfully | payrollId={} | employeeId={} | period={}/{}",
                savedPayroll.getId(),
                employee.id(),
                request.payPeriodMonth(),
                request.payPeriodYear()
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

        List<PayrollSummaryResponse> history =
                payrollRepository.findByEmployeeId(employeeId)
                        .stream()
                        .map(payrollMapper::toSummaryResponse)
                        .toList();

        log.debug(
                "Payroll history retrieved | employeeId={} | recordCount={}",
                employeeId,
                history.size()
        );

        return history;
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

        List<PayrollSummaryResponse> payrolls =
                payrollRepository
                        .findByPayPeriodMonthAndPayPeriodYear(month, year)
                        .stream()
                        .map(payrollMapper::toSummaryResponse)
                        .toList();

        log.debug(
                "Payroll records retrieved | month={} | year={} | recordCount={}",
                month,
                year,
                payrolls.size()
        );

        return payrolls;
    }

    @Override
    public PayrollResponse addDeduction(
            Long payrollId,
            PayrollDeductionRequest request
    ) {

        log.info(
                "Adding deduction | payrollId={} | deductionType={}",
                payrollId,
                request.deductionType()
        );

        // 1. Find payroll
        Payroll payroll = findPayrollById(payrollId);

        // 2. Calculate updated deductions
        BigDecimal currentDeductions =
                defaultAmount(payroll.getTotalDeductions());

        BigDecimal updatedDeductions =
                currentDeductions.add(request.amount());

        // 3. Calculate resulting net salary
        BigDecimal grossSalary =
                defaultAmount(payroll.getGrossSalary());

        BigDecimal updatedNetSalary =
                grossSalary.subtract(updatedDeductions);

        if (updatedNetSalary.compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessRuleViolationException(
                    "Total deductions cannot exceed gross salary."
            );
        }

        // 4. Create deduction
        PayrollDeduction deduction = new PayrollDeduction();

        deduction.setDeductionType(request.deductionType());
        deduction.setAmount(request.amount());
        deduction.setDescription(request.description());

        // Synchronize both sides of the relationship
        payroll.addDeduction(deduction);

        // 5. Update payroll totals
        payroll.setTotalDeductions(updatedDeductions);
        payroll.setNetSalary(updatedNetSalary);

        // 6. Cascade saves the deduction as well
        Payroll updatedPayroll = payrollRepository.save(payroll);

        log.info(
                "Deduction added successfully | payrollId={} | deductionId={}",
                updatedPayroll.getId(),
                deduction.getId()
        );

        return payrollMapper.toResponse(updatedPayroll);
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