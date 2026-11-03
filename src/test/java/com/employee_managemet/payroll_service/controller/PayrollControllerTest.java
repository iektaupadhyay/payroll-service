package com.employee_managemet.payroll_service.controller;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.request.PayrollRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollResponse;
import com.employee_managemet.payroll_service.dto.response.PayrollSummaryResponse;
import com.employee_managemet.payroll_service.enums.DeductionType;
import com.employee_managemet.payroll_service.enums.PayrollStatus;
import com.employee_managemet.payroll_service.exception.BusinessRuleViolationException;
import com.employee_managemet.payroll_service.exception.DuplicatePayrollException;
import com.employee_managemet.payroll_service.exception.EmployeeNotFoundException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceUnavailableException;
import com.employee_managemet.payroll_service.exception.InvalidPayrollStatusTransitionException;
import com.employee_managemet.payroll_service.exception.PayrollNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import com.employee_managemet.payroll_service.exception.handler.GlobalExceptionHandler;
import com.employee_managemet.payroll_service.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PayrollControllerTest {

    private PayrollService payrollService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        payrollService = mock(PayrollService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PayrollController(payrollService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPayroll_shouldReturn201() throws Exception {
        when(payrollService.createPayroll(any(PayrollRequest.class))).thenReturn(payrollResponse());

        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content(validPayrollRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeCode").value("EMP-010"))
                .andExpect(jsonPath("$.paymentStatus").value("DRAFT"));

        verify(payrollService).createPayroll(any(PayrollRequest.class));
    }

    @Test
    void createPayroll_shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content("""
                                {
                                  "employeeId": -1,
                                  "payPeriodMonth": 13,
                                  "payPeriodYear": 1999,
                                  "ctc": -10,
                                  "basicSalary": -1,
                                  "hra": 0,
                                  "bankAccountNumber": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").isMap());
        verifyNoInteractions(payrollService);
    }

    @Test
    void getPayrollById_shouldReturn200() throws Exception {
        when(payrollService.getPayrollById(1L)).thenReturn(payrollResponse());

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getEmployeePayrollHistory_shouldReturn200AndList() throws Exception {
        when(payrollService.getEmployeePayrollHistory(10L)).thenReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/v1/payroll/employee/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(10))
                .andExpect(jsonPath("$[0].employeeCode").value("EMP-010"));
    }

    @Test
    void getPayrollByPeriod_shouldPassMonthAndYear() throws Exception {
        when(payrollService.getPayrollByPeriod(11, 2026)).thenReturn(List.of(summaryResponse()));

        mockMvc.perform(get("/api/v1/payroll/period")
                        .param("month", "11")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payPeriodMonth").value(11));

        verify(payrollService).getPayrollByPeriod(11, 2026);
    }

    @Test
    void addDeduction_shouldReturn200() throws Exception {
        when(payrollService.addDeduction(eq(1L), any(PayrollDeductionRequest.class)))
                .thenReturn(payrollResponse());

        mockMvc.perform(post("/api/v1/payroll/1/deductions")
                        .contentType("application/json")
                        .content("""
                                {"deductionType":"INCOME_TAX","amount":1500.00,"description":"Tax"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(payrollService).addDeduction(eq(1L), any(PayrollDeductionRequest.class));
    }

    @Test
    void addDeduction_shouldReturn400ForInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/1/deductions")
                        .contentType("application/json")
                        .content("""
                                {"deductionType":"INCOME_TAX","amount":-10,"description":"Tax"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payrollService);
    }

    @Test
    void updatePayrollStatus_shouldReturn200() throws Exception {
        when(payrollService.updatePayrollStatus(1L, PayrollStatus.PENDING)).thenReturn(payrollResponse());

        mockMvc.perform(patch("/api/v1/payroll/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(payrollService).updatePayrollStatus(1L, PayrollStatus.PENDING);
    }

    @Test
    void updatePayrollStatus_shouldReturn400ForInvalidEnum() throws Exception {
        mockMvc.perform(patch("/api/v1/payroll/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payrollService);
    }

    @Test
    void updatePayrollStatus_shouldReturn400WhenStatusMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/payroll/1/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payrollService);
    }

    @Test
    void getPayrollByPeriod_shouldReturn400WhenQueryParameterMissing() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/period").param("month", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request parameter is missing: year"));
    }

    @Test
    void getPayrollById_shouldMapPayrollNotFoundTo404() throws Exception {
        when(payrollService.getPayrollById(404L)).thenThrow(new PayrollNotFoundException("Payroll not found"));

        mockMvc.perform(get("/api/v1/payroll/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payroll not found"));
    }

    @Test
    void createPayroll_shouldMapEmployeeNotFoundTo404() throws Exception {
        when(payrollService.createPayroll(any())).thenThrow(new EmployeeNotFoundException("Employee missing"));

        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content(validPayrollRequest()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPayroll_shouldMapDuplicateTo409() throws Exception {
        when(payrollService.createPayroll(any())).thenThrow(new DuplicatePayrollException("Already exists"));

        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content(validPayrollRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    void addDeduction_shouldMapBusinessRuleViolationTo422() throws Exception {
        when(payrollService.addDeduction(eq(1L), any())).thenThrow(new BusinessRuleViolationException("Exceeds gross"));

        mockMvc.perform(post("/api/v1/payroll/1/deductions")
                        .contentType("application/json")
                        .content("{\"deductionType\":\"OTHER\",\"amount\":10}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateStatus_shouldMapInvalidTransitionTo422() throws Exception {
        when(payrollService.updatePayrollStatus(1L, PayrollStatus.CREDITED))
                .thenThrow(new InvalidPayrollStatusTransitionException("Invalid transition"));

        mockMvc.perform(patch("/api/v1/payroll/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"CREDITED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createPayroll_shouldMapEmployeeServiceUnavailableTo503() throws Exception {
        when(payrollService.createPayroll(any())).thenThrow(
                new EmployeeServiceUnavailableException("Unavailable", new RuntimeException()));

        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content(validPayrollRequest()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void createPayroll_shouldMapEmployeeServiceErrorTo502() throws Exception {
        when(payrollService.createPayroll(any())).thenThrow(new EmployeeServiceException("Bad upstream"));

        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content(validPayrollRequest()))
                .andExpect(status().isBadGateway());
    }


    @Test
    void getPayrollById_shouldMapOptimisticLockConflictTo409() throws Exception {
        when(payrollService.getPayrollById(1L))
                .thenThrow(new OptimisticLockingFailureException("Concurrent update"));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getPayrollById_shouldMapDataIntegrityViolationTo409() throws Exception {
        when(payrollService.getPayrollById(1L))
                .thenThrow(new DataIntegrityViolationException("Constraint failure"));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void getPayrollById_shouldMapDatabaseFailureTo500() throws Exception {
        when(payrollService.getPayrollById(1L))
                .thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void getPayrollById_shouldMapIllegalArgumentTo400() throws Exception {
        when(payrollService.getPayrollById(1L)).thenThrow(new IllegalArgumentException("Invalid argument"));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayrollById_shouldMapUnexpectedExceptionTo500() throws Exception {
        when(payrollService.getPayrollById(1L)).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support if the problem persists."));
    }

    @Test
    void createPayroll_shouldMapMalformedJsonTo400() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/create")
                        .contentType("application/json")
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payrollService);
    }

    @Test
    void getPayrollByPeriod_shouldMapWrongParameterTypeTo400() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/period")
                        .param("month", "November")
                        .param("year", "2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedHttpMethod_shouldReturn405() throws Exception {
        mockMvc.perform(put("/api/v1/payroll/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownEndpoint_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/unknown/resource"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPayrollById_shouldMapConstraintViolationTo400() throws Exception {
        when(payrollService.getPayrollById(1L))
                .thenThrow(new ConstraintViolationException("Invalid value", java.util.Set.of()));

        mockMvc.perform(get("/api/v1/payroll/1"))
                .andExpect(status().isBadRequest());
    }

    private static String validPayrollRequest() {
        return """
                {
                  "employeeId": 10,
                  "payPeriodMonth": 11,
                  "payPeriodYear": 2026,
                  "ctc": 720000,
                  "basicSalary": 50000,
                  "hra": 10000,
                  "fixedBonus": 2000,
                  "variableBonus": 3000,
                  "otherAllowances": 1000,
                  "bankAccountNumber": "12345678901234"
                }
                """;
    }

    private static PayrollResponse payrollResponse() {
        return new PayrollResponse(1L, 10L, "EMP-010", "Test Employee", "PERMANENT",
                "**********1234", 11, 2026, new BigDecimal("720000"),
                new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("2000"),
                new BigDecimal("3000"), new BigDecimal("1000"), new BigDecimal("66000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("66000"), PayrollStatus.DRAFT,
                null, List.of(), List.of(), null, null);
    }

    private static PayrollSummaryResponse summaryResponse() {
        return new PayrollSummaryResponse(1L, 10L, "EMP-010", "Test Employee", 11, 2026,
                new BigDecimal("66000"), BigDecimal.ZERO, new BigDecimal("66000"), PayrollStatus.DRAFT);
    }
}
