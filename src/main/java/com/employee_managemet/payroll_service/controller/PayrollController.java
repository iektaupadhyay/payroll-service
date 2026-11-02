package com.employee_managemet.payroll_service.controller;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;
import com.employee_managemet.payroll_service.service.PayrollService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    /**
     * Create payroll for an employee for a specific month.
     */
    @PostMapping("/create")
    public ResponseEntity<PayrollResponse> createPayroll(
            @Valid @RequestBody PayrollRequest request
    ) {
        PayrollResponse response = payrollService.createPayroll(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retrieve payroll by payroll ID.
     */
    @GetMapping("/{payrollId}")
    public ResponseEntity<PayrollResponse> getPayrollById(
            @PathVariable Long payrollId
    ) {
        return ResponseEntity.ok(
                payrollService.getPayrollById(payrollId)
        );
    }

    /**
     * Retrieve all payroll records for an employee.
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollSummaryResponse>> getEmployeePayrollHistory(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(
                payrollService.getEmployeePayrollHistory(employeeId)
        );
    }

    /**
     * Retrieve payroll records for a specific month and year.
     */
    @GetMapping("/period")
    public ResponseEntity<List<PayrollSummaryResponse>> getPayrollByPeriod(
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        return ResponseEntity.ok(
                payrollService.getPayrollByPeriod(month, year)
        );
    }

    /**
     * Add a deduction to an existing payroll record.
     */
    @PostMapping("/{payrollId}/deductions")
    public ResponseEntity<PayrollResponse> addDeduction(
            @PathVariable Long payrollId,
            @Valid @RequestBody PayrollDeductionRequest request
    ) {
        return ResponseEntity.ok(
                payrollService.addDeduction(payrollId, request)
        );
    }
}