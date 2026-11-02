package com.employee_managemet.payroll_service.service.impl;

import com.employee_managemet.payroll_service.client.EmployeeServiceClient;
import com.employee_managemet.payroll_service.dto.integration.EmployeeResponse;
import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;
import com.employee_managemet.payroll_service.enums.PayrollStatus;
import com.employee_managemet.payroll_service.mapper.PayrollMapper;
import com.employee_managemet.payroll_service.model.Payroll;
import com.employee_managemet.payroll_service.model.PayrollDeduction;
import com.employee_managemet.payroll_service.repository.PayrollDeductionRepository;
import com.employee_managemet.payroll_service.repository.PayrollRepository;
import com.employee_managemet.payroll_service.service.PayrollService;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollDeductionRepository deductionRepository;

    private final EmployeeServiceClient employeeServiceClient;
    private final PayrollMapper payrollMapper;

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {

        // 1. Validate employee through Employee Service
        EmployeeResponse employee =
                employeeServiceClient.getEmployeeById(request.employeeId());

        if (employee == null) {
            throw new EntityNotFoundException(
                    "Employee not found with ID: " + request.employeeId()
            );
        }

        // 2. Check whether payroll already exists for this period
        payrollRepository
                .findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(
                        request.employeeId(),
                        request.payPeriodMonth(),
                        request.payPeriodYear()
                )
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
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

        // 4. Calculate initial salary totals
        BigDecimal grossSalary = calculateGrossSalary(request);

        payroll.setGrossSalary(grossSalary);

        // Employer PF is not included in employee deductions.
        // Statutory PF calculation will be implemented separately.
        payroll.setEmployerPf(BigDecimal.ZERO);

        // No deductions have been added at payroll creation.
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(grossSalary);

        // 5. Initial payroll status
        payroll.setPaymentStatus(PayrollStatus.DRAFT);

        // 6. Persist payroll
        Payroll savedPayroll = payrollRepository.save(payroll);

        return payrollMapper.toResponse(savedPayroll);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollById(Long payrollId) {

        Payroll payroll = findPayrollById(payrollId);

        return payrollMapper.toResponse(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollSummaryResponse> getEmployeePayrollHistory(
            Long employeeId
    ) {

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

        // 1. Find payroll
        Payroll payroll = findPayrollById(payrollId);

        // 2. Create deduction
        PayrollDeduction deduction = new PayrollDeduction();

        deduction.setPayroll(payroll);
        deduction.setDeductionType(request.deductionType());
        deduction.setAmount(request.amount());
        deduction.setDescription(request.description());

        // 3. Save deduction
        deductionRepository.save(deduction);

        // 4. Update deduction total
        BigDecimal currentDeductions =
                defaultAmount(payroll.getTotalDeductions());

        BigDecimal updatedDeductions =
                currentDeductions.add(request.amount());

        payroll.setTotalDeductions(updatedDeductions);

        // 5. Recalculate net salary
        BigDecimal grossSalary =
                defaultAmount(payroll.getGrossSalary());

        payroll.setNetSalary(
                grossSalary.subtract(updatedDeductions)
        );

        // 6. Save updated payroll
        Payroll updatedPayroll = payrollRepository.save(payroll);

        return payrollMapper.toResponse(updatedPayroll);
    }

    private Payroll findPayrollById(Long payrollId) {

        return payrollRepository.findById(payrollId)
                .orElseThrow(() -> new EntityNotFoundException(
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