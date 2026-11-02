
package com.employee_managemet.payroll_service.model;

import com.employee_managemet.payroll_service.enums.PayrollStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employee information
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "employment_type")
    private String employmentType;

    // Bank information
    @Column(name = "bank_account_number", length = 18)
    private String bankAccountNumber;

    // Payroll period
    @Column(name = "pay_period_month", nullable = false)
    private Integer payPeriodMonth;

    @Column(name = "pay_period_year", nullable = false)
    private Integer payPeriodYear;

    // Annual compensation
    @Column(name = "ctc", precision = 15, scale = 2)
    private BigDecimal ctc;

    // Earnings
    @Column(name = "basic_salary", precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(precision = 15, scale = 2)
    private BigDecimal hra;

    @Column(name = "fixed_bonus", precision = 15, scale = 2)
    private BigDecimal fixedBonus;

    @Column(name = "variable_bonus", precision = 15, scale = 2)
    private BigDecimal variableBonus;

    @Column(name = "other_allowances", precision = 15, scale = 2)
    private BigDecimal otherAllowances;

    @Column(name = "gross_salary", precision = 15, scale = 2)
    private BigDecimal grossSalary;

    // Employer contribution
    @Column(name = "employer_pf", precision = 15, scale = 2)
    private BigDecimal employerPf;

    // Deductions and take-home
    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_salary", precision = 15, scale = 2)
    private BigDecimal netSalary;

    // Payment information
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PayrollStatus paymentStatus = PayrollStatus.DRAFT;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    // Audit information
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    @OneToMany(
            mappedBy = "payroll",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PayrollLeaveDetails> leaveDetails =
            new ArrayList<>();

    public void addLeaveDetails(PayrollLeaveDetails leave) {
        leaveDetails.add(leave);
        leave.setPayroll(this);
    }
}
