
package com.employee_managemet.payroll_service.client;

import com.employee_managemet.payroll_service.dto.integration.EmployeeLeaveResponse;
import com.employee_managemet.payroll_service.dto.integration.EmployeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmployeeServiceClient {

    private final RestClient employeeRestClient;

    public EmployeeResponse getEmployeeById(Long employeeId) {

        return employeeRestClient.get()
                .uri("/api/v1/employees/getemployeebyid/{id}", employeeId)
                .retrieve()
                .body(EmployeeResponse.class);
    }
//ParameterizedTypeReference:-Because Java erases generic types at runtime. When deserializing a list, we need to preserve the element
    public List<EmployeeLeaveResponse> getEmployeeLeaves(Long employeeId) {

        return employeeRestClient.get()
                .uri("/api/v1/employee-leaves/getemployeeleaves/{employeeId}",
                        employeeId)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        List<EmployeeLeaveResponse>>() {});
    }
}
