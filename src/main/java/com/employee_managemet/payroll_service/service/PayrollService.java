package com.employee_managemet.payroll_service.service;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;

import java.util.List;

public interface PayrollService {

    PayrollResponse createPayroll(PayrollRequest request);

    PayrollResponse getPayrollById(Long payrollId);

    List<PayrollSummaryResponse> getEmployeePayrollHistory(Long employeeId);

    List<PayrollSummaryResponse> getPayrollByPeriod(
            Integer month,
            Integer year
    );

    PayrollResponse addDeduction(
            Long payrollId,
            PayrollDeductionRequest request
    );
}