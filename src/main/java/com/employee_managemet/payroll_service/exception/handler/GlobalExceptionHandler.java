package com.employee_managemet.payroll_service.exception.handler;

import com.employee_managemet.payroll_service.dto.response.ApiErrorResponse;
import com.employee_managemet.payroll_service.exception.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CORRELATION_ID_MDC = "correlationId";

    /*
     * 404 - Resource not found
     */
    @ExceptionHandler({
            PayrollNotFoundException.class,
            EmployeeNotFoundException.class,
            EntityNotFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            Exception exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Resource not found | path={} | message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                Map.of()
        );
    }

    /*
     * 409 - Duplicate payroll
     */
    @ExceptionHandler(DuplicatePayrollException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePayroll(
            DuplicatePayrollException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Duplicate payroll request | path={} | message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request,
                Map.of()
        );
    }

    /*
     * 422 - Business rule violation
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleViolation(
            BusinessRuleViolationException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Business rule violation | path={} | message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request,
                Map.of()
        );
    }

    /*
     * 503 - Employee Service unavailable
     */
    @ExceptionHandler(EmployeeServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeServiceUnavailable(
            EmployeeServiceUnavailableException exception,
            HttpServletRequest request
    ) {

        log.error(
                "Employee Service unavailable | path={}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Employee Service is currently unavailable. Please try again later.",
                request,
                Map.of()
        );
    }

    /*
     * 502 - Unexpected Employee Service response
     */
    @ExceptionHandler(EmployeeServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeServiceError(
            EmployeeServiceException exception,
            HttpServletRequest request
    ) {

        log.error(
                "Employee Service integration error | path={}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.BAD_GATEWAY,
                "An unexpected response was received from Employee Service.",
                request,
                Map.of()
        );
    }

    /*
     * 400 - Request body validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        log.warn(
                "Request validation failed | path={} | errorCount={}",
                request.getRequestURI(),
                errors.size()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request,
                errors
        );
    }

    /*
     * 400 - Method-level validation errors
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Method parameter validation failed | path={}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "One or more request parameters are invalid.",
                request,
                Map.of()
        );
    }

    /*
     * 400 - Constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violation ->
                        errors.putIfAbsent(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        )
                );

        log.warn(
                "Constraint validation failed | path={} | errorCount={}",
                request.getRequestURI(),
                errors.size()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request,
                errors
        );
    }

    /*
     * 400 - Malformed JSON or invalid enum value
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Invalid request body | path={}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is malformed or contains invalid values.",
                request,
                Map.of()
        );
    }

    /*
     * 400 - Missing query parameter
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Required request parameter is missing: "
                        + exception.getParameterName(),
                request,
                Map.of()
        );
    }

    /*
     * 400 - Incorrect parameter type
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value supplied for parameter: "
                        + exception.getName(),
                request,
                Map.of()
        );
    }

    /*
     * 400 - General invalid arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Invalid argument | path={} | message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request,
                Map.of()
        );
    }

    /*
     * 405 - Unsupported HTTP method
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method is not supported for this endpoint.",
                request,
                Map.of()
        );
    }

    /*
     * 409 - Optimistic locking conflict
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Concurrent data modification detected | path={}",
                request.getRequestURI()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "The record was modified by another request. Please refresh and retry.",
                request,
                Map.of()
        );
    }

    /*
     * 409 - Database integrity conflict
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {

        log.error(
                "Database integrity violation | path={}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data.",
                request,
                Map.of()
        );
    }

    /*
     * 500 - Other database failures
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseError(
            DataAccessException exception,
            HttpServletRequest request
    ) {

        log.error(
                "Database operation failed | path={}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database error occurred while processing the request.",
                request,
                Map.of()
        );
    }

    /*
     * 500 - Unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unexpected application error | path={}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists.",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                MDC.get(CORRELATION_ID_MDC),
                validationErrors
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
    @ExceptionHandler(InvalidPayrollStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidStatusTransition(
            InvalidPayrollStatusTransitionException exception,
            HttpServletRequest request
    ) {

        log.warn(
                "Invalid payroll status transition | path={} | message={}",
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request,
                Map.of()
        );
    }
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        log.warn("Endpoint not found | method={} | path={}",
                request.getMethod(),
                request.getRequestURI());

        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "The requested endpoint does not exist",
                request.getRequestURI(),
                MDC.get("correlationId"),
                Map.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}