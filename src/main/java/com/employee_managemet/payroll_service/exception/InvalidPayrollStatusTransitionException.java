package com.employee_managemet.payroll_service.exception;

public class InvalidPayrollStatusTransitionException
        extends RuntimeException {

    public InvalidPayrollStatusTransitionException(String message) {
        super(message);
    }
}