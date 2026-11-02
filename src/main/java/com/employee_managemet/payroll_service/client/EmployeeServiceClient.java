package com.employee_managemet.payroll_service.client;

import com.employee_managemet.payroll_service.dto.integration.EmployeeLeaveResponse;
import com.employee_managemet.payroll_service.dto.integration.EmployeeResponse;
import com.employee_managemet.payroll_service.exception.EmployeeNotFoundException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceException;
import com.employee_managemet.payroll_service.exception.EmployeeServiceUnavailableException;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmployeeServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceClient.class);

    private final RestClient employeeRestClient;

    public EmployeeResponse getEmployeeById(Long employeeId) {

        log.debug("Fetching employee from Employee Service | employeeId={}",
                employeeId);

        try {

            EmployeeResponse employee = employeeRestClient.get()
                    .uri("/api/v1/employees/getemployeebyid/{id}", employeeId)
                    .retrieve()
                    .body(EmployeeResponse.class);

            if (employee == null) {
                throw new EmployeeServiceException(
                        "Employee Service returned an empty employee response."
                );
            }

            return employee;

        } catch (HttpClientErrorException.NotFound exception) {

            log.warn("Employee not found in Employee Service | employeeId={}",
                    employeeId);

            throw new EmployeeNotFoundException(
                    "Employee not found with ID: " + employeeId
            );

        } catch (HttpServerErrorException exception) {

            log.warn(
                    "Employee Service returned a server error | employeeId={} | upstreamStatus={}",
                    employeeId,
                    exception.getStatusCode().value()
            );

            throw new EmployeeServiceUnavailableException(
                    "Employee Service returned a server error.",
                    exception
            );

        } catch (ResourceAccessException exception) {

            log.warn(
                    "Unable to connect to Employee Service | employeeId={}",
                    employeeId
            );

            throw new EmployeeServiceUnavailableException(
                    "Unable to connect to Employee Service.",
                    exception
            );

        } catch (RestClientResponseException exception) {

            log.warn(
                    "Unexpected Employee Service response | employeeId={} | upstreamStatus={}",
                    employeeId,
                    exception.getStatusCode().value()
            );

            throw new EmployeeServiceException(
                    "Unexpected response from Employee Service.",
                    exception
            );

        } catch (RestClientException exception) {

            log.warn(
                    "Employee Service communication error | employeeId={}",
                    employeeId
            );

            throw new EmployeeServiceException(
                    "Error communicating with Employee Service.",
                    exception
            );
        }
    }

    public List<EmployeeLeaveResponse> getEmployeeLeaves(Long employeeId) {

        log.debug("Fetching employee leave allocations | employeeId={}",
                employeeId);

        try {

            List<EmployeeLeaveResponse> leaves = employeeRestClient.get()
                    .uri(
                            "/api/v1/employee-leaves/getemployeeleaves/{employeeId}",
                            employeeId
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (leaves == null) {
                throw new EmployeeServiceException(
                        "Employee Service returned an empty leave response."
                );
            }

            return leaves;

        } catch (HttpServerErrorException exception) {

            log.warn(
                    "Employee Service returned a server error while fetching leave allocations | employeeId={}",
                    employeeId
            );

            throw new EmployeeServiceUnavailableException(
                    "Employee Service returned a server error.",
                    exception
            );

        } catch (ResourceAccessException exception) {

            log.warn(
                    "Unable to connect to Employee Service while fetching leave allocations | employeeId={}",
                    employeeId
            );

            throw new EmployeeServiceUnavailableException(
                    "Unable to connect to Employee Service.",
                    exception
            );

        } catch (RestClientResponseException exception) {

            log.warn(
                    "Unexpected Employee Service response while fetching leave allocations | employeeId={} | upstreamStatus={}",
                    employeeId,
                    exception.getStatusCode().value()
            );

            throw new EmployeeServiceException(
                    "Unexpected response while fetching employee leave allocations.",
                    exception
            );

        } catch (RestClientException exception) {

            log.warn(
                    "Employee Service communication error while fetching leave allocations | employeeId={}",
                    employeeId
            );

            throw new EmployeeServiceException(
                    "Error communicating with Employee Service.",
                    exception
            );
        }
    }
}