
package com.employee_managemet.payroll_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_leave_details")
public class PayrollLeaveDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_id", nullable = false)
    private Payroll payroll;

    @Column(name = "leave_type", nullable = false)
    private String leaveType;

    @Column(name = "total_leaves", nullable = false)
    private Integer totalLeaves;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;
}
