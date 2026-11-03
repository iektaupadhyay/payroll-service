# Payroll Service

A Spring Boot microservice responsible for creating and managing monthly employee payroll records. It integrates with the Employee Service over REST and stores payroll-specific data in its own PostgreSQL database.

> **Project status:** Core payroll creation, retrieval, deductions, status lifecycle, Employee Service integration, API error handling, correlation IDs, and automated tests have been implemented. Payment execution, statutory payroll calculations, authentication/authorization, and bank-account encryption are not implemented yet. See [Known limitations and next steps](#known-limitations-and-next-steps).

## Contents

- [Overview](#overview)
- [Technology stack](#technology-stack)
- [Architecture and service boundaries](#architecture-and-service-boundaries)
- [Project structure](#project-structure)
- [Domain model](#domain-model)
- [Payroll calculations currently implemented](#payroll-calculations-currently-implemented)
- [Payroll status lifecycle](#payroll-status-lifecycle)
- [Employee Service integration](#employee-service-integration)
- [REST API](#rest-api)
- [Request and response examples](#request-and-response-examples)
- [Validation and error handling](#validation-and-error-handling)
- [Logging and correlation IDs](#logging-and-correlation-ids)
- [Database configuration](#database-configuration)
- [Run locally](#run-locally)
- [API documentation](#api-documentation)
- [Automated tests](#automated-tests)
- [Known limitations and next steps](#known-limitations-and-next-steps)

## Overview

The Payroll Service manages payroll records for employees for a given month and year. A payroll record contains:

- Employee identity snapshots received from Employee Service.
- Payroll period (month and year).
- Compensation components: CTC, basic salary, HRA, fixed bonus, variable bonus, and other allowances.
- Bank account number (masked in API responses).
- Gross salary, employer PF, total deductions, and net salary.
- Payment status and credit timestamp.
- Child records for payroll deductions and payroll leave details.
- Creation/update timestamps and an optimistic-locking version.

The service uses a separate database. It does **not** access the Employee Service database directly.

## Technology stack

| Technology | Purpose |
|---|---|
| Java 25 | Application language/runtime target |
| Spring Boot 4.0.0 | Application framework |
| Spring Web MVC | REST endpoints and HTTP handling |
| Spring Data JPA / Hibernate | Persistence and ORM |
| PostgreSQL | Main application database |
| Jakarta Validation | Request and parameter validation |
| Spring `RestClient` | Employee Service integration |
| Lombok | Boilerplate reduction in entities and components |
| Springdoc OpenAPI 3.0.3 | Swagger UI / OpenAPI documentation |
| JUnit 5, Mockito, Spring Boot Test, MockMvc | Automated testing |
| H2 | In-memory database for repository integration tests |
| Maven | Build and dependency management |

## Architecture and service boundaries

```text
                         HTTP/JSON
 Client / API Consumer ----------------------+
                                              |
                                              v
                                  +------------------------+
                                  |     Payroll Service    |
                                  |                        |
                                  | PayrollController      |
                                  |         |              |
                                  |         v              |
                                  | PayrollService         |
                                  |         |              |
                                  |    +----+-------+      |
                                  |    |            |      |
                                  |    v            v      |
                                  | Repositories  EmployeeServiceClient
                                  |    |            |      |
                                  +----|------------|------+
                                       |            |
                                       v            | REST
                                  PostgreSQL        v
                                           +-------------------+
                                           |  Employee Service |
                                           |  Employee API     |
                                           +-------------------+
```

### Important boundary decisions

1. **Separate database:** Payroll Service owns payroll tables. It never reads Employee Service tables directly.
2. **REST integration:** Employee information is retrieved from Employee Service using `RestClient`.
3. **No cross-service JPA relationship:** `Payroll.employeeId` and `Payroll.employeeCode` are scalar snapshot fields, not `@ManyToOne` relationships to Employee.
4. **Employee identity snapshot:** Employee ID, employee code, employee name, and employment type are copied into the payroll record at creation time. This preserves the employee identity details used for that payroll even if the employee's profile changes later.
5. **Employee ID vs employee code:** `employeeId` is the Employee Service database ID and is used for integration/querying. `employeeCode` is the employee's business identifier and is useful for display and reference. They are not interchangeable.

## Project structure

Base package: `com.employee_managemet.payroll_service`

```text
src/main/java/com/employee_managemet/payroll_service/
├── PayrollServiceApplication.java
├── client/
│   └── EmployeeServiceClient.java
├── config/
│   └── RestClientConfig.java
├── controller/
│   └── PayrollController.java
├── dto/
│   ├── integration/
│   │   ├── EmployeeResponse.java
│   │   └── EmployeeLeaveResponse.java
│   ├── request/
│   │   ├── PayrollRequest.java
│   │   ├── PayrollDeductionRequest.java
│   │   └── PayrollStatusUpdateRequest.java
│   └── response/
│       ├── PayrollResponse.java
│       ├── PayrollSummaryResponse.java
│       ├── PayrollDeductionResponse.java
│       ├── PayrollLeaveDetailsResponse.java
│       └── ApiErrorResponse.java
├── enums/
│   ├── PayrollStatus.java
│   └── DeductionType.java
├── exception/
│   ├── PayrollNotFoundException.java
│   ├── EmployeeNotFoundException.java
│   ├── DuplicatePayrollException.java
│   ├── BusinessRuleViolationException.java
│   ├── EmployeeServiceException.java
│   ├── EmployeeServiceUnavailableException.java
│   ├── InvalidPayrollStatusTransitionException.java
│   └── handler/
│       └── GlobalExceptionHandler.java
├── filter/
│   └── CorrelationIdFilter.java
├── mapper/
│   ├── PayrollMapper.java
│   ├── PayrollDeductionMapper.java
│   └── PayrollLeaveDetailsMapper.java
├── model/
│   ├── Payroll.java
│   ├── PayrollDeduction.java
│   └── PayrollLeaveDetails.java
├── repository/
│   ├── PayrollRepository.java
│   ├── PayrollDeductionRepository.java
│   └── PayrollLeaveDetailsRepository.java
└── service/
    ├── PayrollService.java
    └── impl/
        └── PayrollServiceImpl.java

src/main/resources/
└── application.properties

src/test/java/com/employee_managemet/payroll_service/
├── PayrollServiceApplicationTests.java
├── client/EmployeeServiceClientTest.java
├── controller/PayrollControllerTest.java
├── dto/PayrollRequestValidationTest.java
├── filter/CorrelationIdFilterTest.java
├── mapper/PayrollMapperTest.java
├── repository/PayrollRepositoryIntegrationTest.java
└── service/PayrollServiceImplTest.java
```

## Domain model

### 1. `Payroll`

Main aggregate/root entity mapped to the `payroll` table.

| Field | Meaning |
|---|---|
| `id` | Payroll database primary key |
| `employeeId` | Employee Service database ID snapshot |
| `employeeCode` | Employee business code snapshot |
| `employeeName` | Employee name snapshot |
| `employmentType` | Employment type snapshot, stored as String to avoid sharing the Employee Service enum |
| `bankAccountNumber` | Bank account number used for payroll; masked in responses |
| `payPeriodMonth`, `payPeriodYear` | Payroll month and year |
| `ctc` | Annual cost-to-company value supplied with the payroll request |
| `basicSalary`, `hra` | Earnings components |
| `fixedBonus`, `variableBonus` | Bonus components; nullable request values default to zero |
| `otherAllowances` | Other earnings; nullable request value defaults to zero |
| `grossSalary` | Current gross earnings total |
| `employerPf` | Employer PF amount; currently initialized to zero |
| `totalDeductions` | Sum of deductions added to this payroll |
| `netSalary` | Gross salary minus total deductions |
| `paymentStatus` | Current payroll lifecycle status |
| `creditedAt` | Timestamp set when status changes to `CREDITED` |
| `createdAt`, `updatedAt` | Audit timestamps managed by JPA lifecycle callbacks |
| `version` | JPA `@Version` field for optimistic locking |

Relationships:

- One `Payroll` has many `PayrollDeduction` records.
- One `Payroll` has many `PayrollLeaveDetails` records.
- Both relationships use `cascade = CascadeType.ALL` and `orphanRemoval = true`.
- Helper methods `addDeduction(...)` and `addLeaveDetails(...)` maintain both sides of the relationship.

### 2. `PayrollDeduction`

Mapped to `payroll_deductions`. Each row belongs to one payroll and contains a deduction type, amount, and optional description.

`DeductionType` values:

- `EMPLOYEE_PF`
- `PROFESSIONAL_TAX`
- `INCOME_TAX`
- `LEAVE_DEDUCTION`
- `OTHER`

The current API supports adding deductions to a payroll. It updates `totalDeductions` and recalculates `netSalary`.

### 3. `PayrollLeaveDetails`

Mapped to `payroll_leave_details`. Each row belongs to one payroll and stores:

- `leaveType`
- `totalLeaves`
- `appliedDate`

The entity, repository, response DTO, and mapper are present. A complete leave deduction/allocation calculation workflow is **not** implemented in the current payroll service. Employee leave allocation data must not be interpreted as actual leave taken without a separate business rule and source of actual leave usage.

### Entity relationship overview

```text
Payroll (1)
  |
  +------ (many) PayrollDeduction
  |
  +------ (many) PayrollLeaveDetails
```

There is intentionally no JPA relationship from Payroll to Employee because Employee is managed by a different microservice.

## Payroll calculations currently implemented

The current implementation calculates gross salary as:

```text
Gross Salary = Basic Salary
             + HRA
             + Fixed Bonus
             + Variable Bonus
             + Other Allowances
```

Null optional bonus/allowance values are treated as zero.

Initial payroll values:

```text
Employer PF      = 0
Total Deductions = 0
Net Salary       = Gross Salary
Status           = DRAFT
```

When a deduction is added:

```text
Updated Total Deductions = Previous Total Deductions + New Deduction
Updated Net Salary       = Gross Salary - Updated Total Deductions
```

A deduction is rejected if it would make net salary negative. The service currently does not derive gross salary from CTC and does not calculate statutory tax, employee PF, employer PF, or professional tax automatically. These formulas require agreed payroll business rules before implementation.

## Payroll status lifecycle

`PayrollStatus` values:

- `DRAFT`
- `PENDING`
- `PROCESSING`
- `CREDITED`
- `FAILED`
- `CANCELLED`

Allowed transitions:

```text
DRAFT ---------> PENDING ---------> PROCESSING ---------> CREDITED
  |                                  |                      (terminal)
  |                                  +-------> FAILED
  |                                              |
  +---------> CANCELLED                           +-----> PROCESSING
               (terminal)                               (retry transition)

PENDING ---------------------------> CANCELLED (terminal)
```

Transition table:

| Current status | Allowed next status |
|---|---|
| `DRAFT` | `PENDING`, `CANCELLED` |
| `PENDING` | `PROCESSING`, `CANCELLED` |
| `PROCESSING` | `CREDITED`, `FAILED` |
| `FAILED` | `PROCESSING` |
| `CREDITED` | None |
| `CANCELLED` | None |

Additional rules:

- Deductions can be modified only while status is `DRAFT` or `PENDING`.
- `PROCESSING`, `FAILED`, `CREDITED`, and `CANCELLED` payrolls are not editable through the deduction operation.
- `creditedAt` is populated when the target status is `CREDITED`.
- Invalid transitions throw `InvalidPayrollStatusTransitionException` and are returned as HTTP 422.
- `FAILED -> PROCESSING` changes the lifecycle state only. It does not execute a payment retry because no payment gateway/processor is integrated yet.
- Setting status to `CREDITED` is currently a state update, not proof that a bank transfer actually happened. A trusted payment integration and authorization controls are required before production use.

## Employee Service integration

`EmployeeServiceClient` uses the configured `RestClient` bean to call Employee Service.

### Employee lookup

```http
GET {EMPLOYEE_SERVICE_URL}/api/v1/employees/getemployeebyid/{id}
```

The Employee Service response is mapped to `EmployeeResponse`. The Payroll Service integration DTO uses String values for employee enum-like fields so the two services do not need to share Java enum classes.

During payroll creation, Employee Service supplies the employee ID, employee code, employee name, and employment type that are stored as snapshots in Payroll.

### Employee leave allocation lookup

The client also defines:

```http
GET {EMPLOYEE_SERVICE_URL}/api/v1/employee-leaves/getemployeeleaves/{employeeId}
```

The response maps to `EmployeeLeaveResponse`. The DTO describes leave allocation data. It is not, by itself, proof of leave taken and is not currently used to calculate payroll deductions.

### Upstream error mapping

| Employee Service outcome | Payroll Service exception / HTTP response |
|---|---|
| Employee endpoint returns 404 | `EmployeeNotFoundException` / 404 |
| Employee Service returns 5xx | `EmployeeServiceUnavailableException` / 503 |
| Connection failure/timeout | `EmployeeServiceUnavailableException` / 503 |
| Other unexpected upstream response | `EmployeeServiceException` / 502 |

## REST API

Base path: `/api/v1/payroll`

| Method | Endpoint | Description | Success response |
|---|---|---|---|
| `POST` | `/create` | Create a payroll record | `201 Created` with `PayrollResponse` |
| `GET` | `/{payrollId}` | Retrieve payroll details | `200 OK` with `PayrollResponse` |
| `GET` | `/employee/{employeeId}` | Retrieve payroll history for an employee | `200 OK` with a list of `PayrollSummaryResponse` |
| `GET` | `/period?month={month}&year={year}` | Retrieve payroll records for a month/year | `200 OK` with a list of `PayrollSummaryResponse` |
| `POST` | `/{payrollId}/deductions` | Add a deduction | `200 OK` with updated `PayrollResponse` |
| `PATCH` | `/{payrollId}/status` | Change payroll status if transition is allowed | `200 OK` with updated `PayrollResponse` |

### Endpoint validation

- IDs must be positive.
- Month must be between 1 and 12.
- Year must be 2000 or later.
- Payroll request requires employee ID, month, year, CTC, basic salary, HRA, and bank account number.
- Bank account number must contain 9–18 digits.
- Monetary request fields must be non-negative and support up to 2 fractional digits, with up to 13 integer digits.
- Deduction type and amount are required; description is limited to 255 characters.
- Status is required and must deserialize to a valid `PayrollStatus` enum value.

## Request and response examples

### 1. Create payroll

```http
POST /api/v1/payroll/create
Content-Type: application/json
```

```json
{
  "employeeId": 101,
  "payPeriodMonth": 11,
  "payPeriodYear": 2026,
  "ctc": 1200000.00,
  "basicSalary": 40000.00,
  "hra": 16000.00,
  "fixedBonus": 2000.00,
  "variableBonus": 3000.00,
  "otherAllowances": 1000.00,
  "bankAccountNumber": "123456789012"
}
```

`employeeCode`, employee name, and employment type are not accepted from the caller. They are retrieved from Employee Service using `employeeId`.

Example response (illustrative IDs and timestamps):

```json
{
  "id": 501,
  "employeeId": 101,
  "employeeCode": "EMP-00101",
  "employeeName": "Example Employee",
  "employmentType": "PERMANENT",
  "maskedAccountNumber": "********9012",
  "payPeriodMonth": 11,
  "payPeriodYear": 2026,
  "ctc": 1200000.00,
  "basicSalary": 40000.00,
  "hra": 16000.00,
  "fixedBonus": 2000.00,
  "variableBonus": 3000.00,
  "otherAllowances": 1000.00,
  "grossSalary": 62000.00,
  "employerPf": 0.00,
  "totalDeductions": 0.00,
  "netSalary": 62000.00,
  "paymentStatus": "DRAFT",
  "creditedAt": null,
  "deductions": [],
  "leaveDetails": [],
  "createdAt": "2026-11-03T10:00:00",
  "updatedAt": "2026-11-03T10:00:00"
}
```

### 2. Add a deduction

```http
POST /api/v1/payroll/501/deductions
Content-Type: application/json
```

```json
{
  "deductionType": "PROFESSIONAL_TAX",
  "amount": 200.00,
  "description": "Monthly professional tax"
}
```

The example payroll gross salary is `62000.00`. After this deduction:

```text
Total deductions = 200.00
Net salary       = 61800.00
```

### 3. Update payroll status

```http
PATCH /api/v1/payroll/501/status
Content-Type: application/json
```

```json
{
  "status": "PENDING"
}
```

A subsequent valid request can move the payroll to `PROCESSING`. The service validates every transition against the lifecycle table above.

### 4. Get payroll by ID

```http
GET /api/v1/payroll/501
```

Returns the full payroll response, including deductions and leave detail records.

### 5. Get employee payroll history

```http
GET /api/v1/payroll/employee/101
```

Returns summary records for the specified employee.

### 6. Get payroll for a period

```http
GET /api/v1/payroll/period?month=11&year=2026
```

Returns summary records for all payrolls matching the specified month and year.

## Validation and error handling

`GlobalExceptionHandler` centralizes exception-to-HTTP mapping and returns `ApiErrorResponse`:

```json
{
  "timestamp": "2026-11-03T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Payroll not found with ID: 999",
  "path": "/api/v1/payroll/999",
  "correlationId": "b2e1f9a8-7a24-4d0d-a3c1-6b6b7e8f9a10",
  "validationErrors": {}
}
```

HTTP status mapping:

| Status | Cases |
|---|---|
| `400 Bad Request` | Bean validation, invalid path/query parameter, malformed JSON, invalid enum value, missing query parameter |
| `404 Not Found` | Payroll not found, employee not found, missing resource/endpoint |
| `405 Method Not Allowed` | Unsupported HTTP method |
| `409 Conflict` | Duplicate payroll, optimistic-lock conflict, database integrity conflict |
| `422 Unprocessable Entity` | Business rule violation and invalid payroll status transition |
| `502 Bad Gateway` | Unexpected Employee Service response |
| `503 Service Unavailable` | Employee Service unavailable or unreachable |
| `500 Internal Server Error` | Unexpected application or database failure |

The global handler also maps unknown controller endpoints (`NoHandlerFoundException`) and missing static resources (`NoResourceFoundException`) to 404. This prevents an unknown URL from being reported as a generic 500.

## Logging and correlation IDs

`CorrelationIdFilter` runs at highest precedence for each HTTP request.

- Reads the `X-Correlation-ID` request header.
- Accepts values matching `^[a-zA-Z0-9._-]{1,64}$`.
- Generates a UUID when the header is missing or invalid.
- Stores the ID in SLF4J MDC under `correlationId`.
- Adds the ID to the response's `X-Correlation-ID` header.
- Logs HTTP method, request path, response status, and duration.
- Removes the MDC value in a `finally` block to avoid leaking it between requests/threads.

The console log pattern includes the correlation ID. Request bodies and full bank account numbers should not be logged.

## Database configuration

The current `application.properties` expects these environment variables:

| Variable | Required | Example |
|---|---|---|
| `DATABASE_URL` | Yes | `jdbc:postgresql://localhost:5432/payroll_db` |
| `DATABASE_USERNAME` | Yes | `postgres` |
| `DATABASE_PASSWORD` | Yes | Set your local PostgreSQL password |
| `EMPLOYEE_SERVICE_URL` | No | `http://localhost:8081` (default) |

Example PowerShell configuration for a local development session:

```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/payroll_db"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="your-local-password"
$env:EMPLOYEE_SERVICE_URL="http://localhost:8081"
```

Create the PostgreSQL database before starting the application. Confirm the actual Employee Service port and set `EMPLOYEE_SERVICE_URL` accordingly.

JPA settings currently include:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
```

`ddl-auto=update` is convenient during development. For production, use controlled schema migrations (for example, Flyway or Liquibase) and review changes before deployment. If `employee_code` is added as a non-null column to a populated table, existing rows need a backfill/migration before the constraint can be safely applied.

## Run locally

### Prerequisites

- JDK 25
- Maven (or the included Maven Wrapper)
- PostgreSQL
- Employee Service running and reachable at the configured URL

### Start the application

From the project root:

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

If using a locally installed Maven instead:

```bash
mvn spring-boot:run
```

### Build the project

```bash
mvn clean package
```

### Run all tests

Windows:

```powershell
.\mvnw.cmd clean test
```

macOS/Linux:

```bash
./mvnw clean test
```

The application targets Java 25. Ensure `JAVA_HOME` and the IDE's project SDK/compiler are configured to JDK 25.

## API documentation

Springdoc OpenAPI is included. When the application is running, the usual Springdoc endpoints are:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Replace port `8080` if the application is configured to use a different port.

## Automated tests

The test suite is organized by application layer:

| Test class | Main coverage |
|---|---|
| `PayrollServiceApplicationTests` | Spring application context starts successfully |
| `PayrollServiceImplTest` | Payroll creation, calculations, duplicate handling, retrieval, deduction rules, status transitions and invalid transitions |
| `PayrollControllerTest` | REST endpoint behaviour, request/parameter validation, HTTP status codes and unknown endpoint handling |
| `EmployeeServiceClientTest` | Employee Service REST success and error scenarios |
| `PayrollMapperTest` | Entity-to-response mapping, summary mapping, nested child mapping, bank account masking |
| `PayrollRequestValidationTest` | Payroll request field constraints |
| `CorrelationIdFilterTest` | Correlation ID acceptance/generation, response header and MDC cleanup |
| `PayrollRepositoryIntegrationTest` | Repository queries, JPA persistence, child cascade and orphan removal using the test database |

Testing libraries are provided through `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, and H2 test scope dependencies. Service/client tests use mocks where appropriate; repository integration tests exercise JPA against an in-memory database.

Run tests with `clean test` to avoid stale compiled test classes. Review the Surefire reports under `target/surefire-reports` if a test fails.

## Known limitations and next steps

The following items are intentionally not represented as completed functionality:

1. **Payment gateway integration:** No bank/payment provider is connected. `CREDITED` is currently a status transition only.
2. **Real retry processing:** `FAILED -> PROCESSING` is a lifecycle transition, not a payment retry operation. Payment attempts, provider references, idempotency, and retry rules are not implemented.
3. **Payroll status audit history:** There is no separate persistent status-history entity recording every transition, actor, reason, and timestamp.
4. **Statutory calculations:** Income tax, employee PF, employer PF, professional tax, and other country-specific rules are not calculated automatically. Employer PF is currently set to zero.
5. **Leave calculation:** Leave allocation response/entity support exists, but actual leave taken and leave-based salary deduction rules are not implemented.
6. **Bank account encryption:** API responses mask the account number, but the entity currently stores the supplied account number as-is. Add encryption at rest and key management before production use. Never log the full number.
7. **Authentication and authorization:** There is no security layer shown in the current implementation. Status changes and access to payroll/bank information must be restricted before production use.
8. **Duplicate creation race:** The service checks for an existing employee/month/year payroll before saving, but there is no database unique constraint. Two concurrent requests could pass the check simultaneously. Add a database-level uniqueness strategy or an appropriate concurrency-safe design if one payroll per employee per period is a firm rule.
9. **Schema migration strategy:** `ddl-auto=update` is currently used for development. Production should use versioned migrations.
10. **Operational resilience:** Timeouts, circuit breaking, and retry policy for Employee Service calls should be reviewed and configured for deployment needs.

### Suggested implementation order

1. Decide payroll statutory/business formulas and rounding rules.
2. Add authentication and role-based authorization.
3. Add encrypted bank-account storage and controlled access to payment data.
4. Add payroll status history in the same transaction as status updates.
5. Add payment-attempt tracking and provider-neutral payment integration contracts.
6. Implement actual payment processing with idempotency and reconciliation.
7. Implement leave usage/adjustment rules using an authoritative source of leave taken.
8. Add database migrations and concurrency-safe duplicate prevention.
9. Add health checks, metrics, tracing, and deployment configuration.

---

**Note:** This document describes the current implementation and distinguishes implemented behaviour from future work. Update it whenever API contracts, entity fields, lifecycle rules, or payroll calculation rules change.
