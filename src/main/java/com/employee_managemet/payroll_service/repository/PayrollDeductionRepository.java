
package com.employee_managemet.payroll_service.repository;

import com.employee_managemet.payroll_service.model.PayrollDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollDeductionRepository extends JpaRepository<PayrollDeduction, Long> {

    List<PayrollDeduction> findByPayroll_Id(Long payrollId);
}
