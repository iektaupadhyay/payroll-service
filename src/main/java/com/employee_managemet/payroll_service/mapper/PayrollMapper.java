
package com.employee_managemet.payroll_service.mapper;

import com.employee_managemet.payroll_service.dto.response.PayrollDeductionResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollLeaveDetailsResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;
import com.employee_managemet.payroll_service.model.Payroll;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayrollMapper {

    private final PayrollDeductionMapper deductionMapper;
    private final PayrollLeaveDetailsMapper leaveDetailsMapper;

    public PayrollMapper(
            PayrollDeductionMapper deductionMapper,
            PayrollLeaveDetailsMapper leaveDetailsMapper) {

        this.deductionMapper = deductionMapper;
        this.leaveDetailsMapper = leaveDetailsMapper;
    }

    public PayrollResponse toResponse(Payroll payroll) {

        List<PayrollDeductionResponse> deductions =
                payroll.getDeductions() == null
                        ? List.of()
                        : payroll.getDeductions()
                        .stream()
                        .map(deductionMapper::toResponse)
                        .toList();

        List<PayrollLeaveDetailsResponse> leaveDetails =
                payroll.getLeaveDetails() == null
                        ? List.of()
                        : payroll.getLeaveDetails()
                        .stream()
                        .map(leaveDetailsMapper::toResponse)
                        .toList();

        return new PayrollResponse(
                payroll.getId(),
                payroll.getEmployeeId(),
                payroll.getEmployeeName(),
                payroll.getEmploymentType(),
                maskAccountNumber(payroll.getBankAccountNumber()),
                payroll.getPayPeriodMonth(),
                payroll.getPayPeriodYear(),
                payroll.getCtc(),
                payroll.getBasicSalary(),
                payroll.getHra(),
                payroll.getFixedBonus(),
                payroll.getVariableBonus(),
                payroll.getOtherAllowances(),
                payroll.getGrossSalary(),
                payroll.getEmployerPf(),
                payroll.getTotalDeductions(),
                payroll.getNetSalary(),
                payroll.getPaymentStatus(),
                payroll.getCreditedAt(),
                deductions,
                leaveDetails,
                payroll.getCreatedAt(),
                payroll.getUpdatedAt()
        );
    }

    public PayrollSummaryResponse toSummaryResponse(Payroll payroll) {

        return new PayrollSummaryResponse(
                payroll.getId(),
                payroll.getEmployeeId(),
                payroll.getEmployeeName(),
                payroll.getPayPeriodMonth(),
                payroll.getPayPeriodYear(),
                payroll.getGrossSalary(),
                payroll.getTotalDeductions(),
                payroll.getNetSalary(),
                payroll.getPaymentStatus()
        );
    }

    private String maskAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "*".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }
}
