package com.employee_managemet.payroll_service.controller;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;

import com.employee_managemet.payroll_service.service.PayrollService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@Validated
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/create")
    public ResponseEntity<PayrollResponse> createPayroll(
            @Valid @RequestBody PayrollRequest request
    ) {

        PayrollResponse response =
                payrollService.createPayroll(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{payrollId}")
    public ResponseEntity<PayrollResponse> getPayrollById(
            @PathVariable("payrollId") @Positive Long payrollId
    ) {

        return ResponseEntity.ok(
                payrollService.getPayrollById(payrollId)
        );
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollSummaryResponse>>
    getEmployeePayrollHistory(
            @PathVariable("employeeId") @Positive Long employeeId
    ) {

        return ResponseEntity.ok(
                payrollService.getEmployeePayrollHistory(employeeId)
        );
    }

    @GetMapping("/period")
    public ResponseEntity<List<PayrollSummaryResponse>> getPayrollByPeriod(
            @RequestParam("month")
            @Min(1) @Max(12) Integer month,

            @RequestParam("year")
            @Min(2000) Integer year
    ) {

        return ResponseEntity.ok(
                payrollService.getPayrollByPeriod(month, year)
        );
    }

    @PostMapping("/{payrollId}/deductions")
    public ResponseEntity<PayrollResponse> addDeduction(
            @PathVariable("payrollId") @Positive Long payrollId,

            @Valid @RequestBody PayrollDeductionRequest request
    ) {

        return ResponseEntity.ok(
                payrollService.addDeduction(payrollId, request)
        );
    }
}