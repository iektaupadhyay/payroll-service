
package com.employee_managemet.payroll_service.repository;

import com.employee_managemet.payroll_service.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByEmployeeId(Long employeeId);

    Optional<Payroll> findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(
            Long employeeId,
            Integer payPeriodMonth,
            Integer payPeriodYear
    );

    List<Payroll> findByPayPeriodMonthAndPayPeriodYear(
            Integer payPeriodMonth,
            Integer payPeriodYear
    );
}
