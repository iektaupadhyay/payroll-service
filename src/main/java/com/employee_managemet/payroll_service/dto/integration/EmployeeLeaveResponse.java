
package com.employee_managemet.payroll_service.dto.integration;

import java.time.LocalDate;

public record EmployeeLeaveResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        String leaveType,
        Integer totalLeaves,
        LocalDate appliedDate
) {}
