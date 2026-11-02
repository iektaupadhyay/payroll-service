
package com.employee_managemet.payroll_service.repository;

import com.employee_managemet.payroll_service.model.PayrollLeaveDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollLeaveDetailsRepository extends JpaRepository<PayrollLeaveDetails, Long> {

    List<PayrollLeaveDetails> findByPayroll_Id(Long payrollId);
}
