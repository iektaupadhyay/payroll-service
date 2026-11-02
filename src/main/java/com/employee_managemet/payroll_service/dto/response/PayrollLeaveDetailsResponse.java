package com.employee_managemet.payroll_service.dto.response;

import java.time.LocalDate;

public record PayrollLeaveDetailsResponse(
        Long id,
        String leaveType,
        Integer totalLeaves,
        LocalDate appliedDate
) {}