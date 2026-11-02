
package com.employee_managemet.payroll_service.dto.integration;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String gender,
        String employmentType,
        String employmentStatus,
        LocalDate joiningDate,
        String mobileNumber,
        String email,
        String position,
        Long departmentId,
        String departmentName,
        AddressResponse address
) {
    public record AddressResponse(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String country,
            String postalCode
    ) {}
}
