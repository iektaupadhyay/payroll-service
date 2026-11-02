package com.employee_managemet.payroll_service.exception;

public class DuplicatePayrollException extends RuntimeException {

    public DuplicatePayrollException(String message) {
        super(message);
    }
}