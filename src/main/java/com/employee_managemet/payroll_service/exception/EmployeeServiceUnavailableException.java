package com.employee_managemet.payroll_service.exception;

public class EmployeeServiceUnavailableException extends RuntimeException {

    public EmployeeServiceUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}