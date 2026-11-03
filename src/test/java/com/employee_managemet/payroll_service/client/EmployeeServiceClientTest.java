package com.employee_managemet.payroll_service.client;

import com.employee_managemet.payroll_service.exception.EmployeeNotFoundException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EmployeeServiceClientTest {

    private MockRestServiceServer server;
    private EmployeeServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://employee-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new EmployeeServiceClient(builder.build());
    }

    @Test
    void getEmployeeById_shouldReturnEmployeeWhenUpstreamReturns200() {
        server.expect(requestTo("http://employee-service/api/v1/employees/getemployeebyid/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 10,
                          "employeeCode": "EMP-010",
                          "name": "Test Employee",
                          "gender": "MALE",
                          "employmentType": "PERMANENT",
                          "employmentStatus": "ACTIVE",
                          "email": "test@example.com"
                        }
                        """, MediaType.APPLICATION_JSON));

        var employee = client.getEmployeeById(10L);

        assertEquals(10L, employee.id());
        assertEquals("EMP-010", employee.employeeCode());
        assertEquals("Test Employee", employee.name());
        server.verify();
    }

    @Test
    void getEmployeeById_shouldTranslate404ToEmployeeNotFound() {
        server.expect(requestTo("http://employee-service/api/v1/employees/getemployeebyid/404"))
                .andRespond(withResourceNotFound());

        assertThrows(EmployeeNotFoundException.class, () -> client.getEmployeeById(404L));
        server.verify();
    }

    @Test
    void getEmployeeById_shouldTranslate5xxToUnavailable() {
        server.expect(requestTo("http://employee-service/api/v1/employees/getemployeebyid/10"))
                .andRespond(withServerError());

        assertThrows(EmployeeServiceUnavailableException.class, () -> client.getEmployeeById(10L));
        server.verify();
    }

    @Test
    void getEmployeeById_shouldTranslateUnexpected4xxToIntegrationException() {
        server.expect(requestTo("http://employee-service/api/v1/employees/getemployeebyid/10"))
                .andRespond(withBadRequest());

        assertThrows(EmployeeServiceException.class, () -> client.getEmployeeById(10L));
        server.verify();
    }

    @Test
    void getEmployeeById_shouldRejectEmptyResponseBody() {
        server.expect(requestTo("http://employee-service/api/v1/employees/getemployeebyid/10"))
                .andRespond(withNoContent());

        assertThrows(EmployeeServiceException.class, () -> client.getEmployeeById(10L));
        server.verify();
    }

    @Test
    void getEmployeeLeaves_shouldReturnLeaveAllocations() {
        server.expect(requestTo("http://employee-service/api/v1/employee-leaves/getemployeeleaves/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 4,
                            "employeeId": 10,
                            "employeeCode": "EMP-010",
                            "employeeName": "Test Employee",
                            "leaveType": "CASUAL",
                            "totalLeaves": 12
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var leaves = client.getEmployeeLeaves(10L);

        assertEquals(1, leaves.size());
        assertEquals("CASUAL", leaves.getFirst().leaveType());
        assertEquals(12, leaves.getFirst().totalLeaves());
        server.verify();
    }

    @Test
    void getEmployeeLeaves_shouldTranslate5xxToUnavailable() {
        server.expect(requestTo("http://employee-service/api/v1/employee-leaves/getemployeeleaves/10"))
                .andRespond(withServerError());

        assertThrows(EmployeeServiceUnavailableException.class, () -> client.getEmployeeLeaves(10L));
        server.verify();
    }

    @Test
    void getEmployeeLeaves_shouldTranslate4xxToIntegrationException() {
        server.expect(requestTo("http://employee-service/api/v1/employee-leaves/getemployeeleaves/10"))
                .andRespond(withBadRequest());

        assertThrows(EmployeeServiceException.class, () -> client.getEmployeeLeaves(10L));
        server.verify();
    }

    @Test
    void getEmployeeLeaves_shouldRejectEmptyResponseBody() {
        server.expect(requestTo("http://employee-service/api/v1/employee-leaves/getemployeeleaves/10"))
                .andRespond(withNoContent());

        assertThrows(EmployeeServiceException.class, () -> client.getEmployeeLeaves(10L));
        server.verify();
    }
}
