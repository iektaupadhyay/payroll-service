package com.employee_managemet.payroll_service.repository;

import com.employee_managemet.payroll_service.enums.DeductionType;
import com.employee_managemet.payroll_service.enums.PayrollStatus;
import com.employee_managemet.payroll_service.model.Payroll;
import com.employee_managemet.payroll_service.model.PayrollDeduction;
import com.employee_managemet.payroll_service.model.PayrollLeaveDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payrolltest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PayrollRepositoryIntegrationTest {

    @Autowired private PayrollRepository payrollRepository;
    @Autowired private PayrollDeductionRepository deductionRepository;
    @Autowired private PayrollLeaveDetailsRepository leaveDetailsRepository;

    @Test
    void payrollRepository_shouldFindByEmployeeId() {
        payrollRepository.saveAndFlush(payroll(10L, 11, 2026));
        payrollRepository.saveAndFlush(payroll(10L, 10, 2026));
        payrollRepository.saveAndFlush(payroll(20L, 11, 2026));

        List<Payroll> results = payrollRepository.findByEmployeeId(10L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getEmployeeId().equals(10L)));
    }

    @Test
    void payrollRepository_shouldFindByEmployeeAndPeriod() {
        Payroll saved = payrollRepository.saveAndFlush(payroll(10L, 11, 2026));

        var found = payrollRepository.findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(10L, 11, 2026);
        var missing = payrollRepository.findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(10L, 12, 2026);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(missing.isEmpty());
    }

    @Test
    void payrollRepository_shouldFindByPeriod() {
        payrollRepository.saveAndFlush(payroll(10L, 11, 2026));
        payrollRepository.saveAndFlush(payroll(20L, 11, 2026));
        payrollRepository.saveAndFlush(payroll(30L, 10, 2026));

        List<Payroll> results = payrollRepository.findByPayPeriodMonthAndPayPeriodYear(11, 2026);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getPayPeriodMonth().equals(11)));
    }

    @Test
    void deductionRepository_shouldFindDeductionsByPayrollId() {
        Payroll payroll = payrollRepository.saveAndFlush(payroll(10L, 11, 2026));
        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setPayroll(payroll);
        deduction.setDeductionType(DeductionType.INCOME_TAX);
        deduction.setAmount(new BigDecimal("1500.00"));
        deduction.setDescription("Tax");
        deductionRepository.saveAndFlush(deduction);

        List<PayrollDeduction> results = deductionRepository.findByPayroll_Id(payroll.getId());

        assertEquals(1, results.size());
        assertEquals(DeductionType.INCOME_TAX, results.getFirst().getDeductionType());
    }

    @Test
    void leaveDetailsRepository_shouldFindLeaveDetailsByPayrollId() {
        Payroll payroll = payrollRepository.saveAndFlush(payroll(10L, 11, 2026));
        PayrollLeaveDetails leave = new PayrollLeaveDetails();
        leave.setPayroll(payroll);
        leave.setLeaveType("CASUAL");
        leave.setTotalLeaves(2);
        leave.setAppliedDate(LocalDate.of(2026, 11, 1));
        leaveDetailsRepository.saveAndFlush(leave);

        List<PayrollLeaveDetails> results = leaveDetailsRepository.findByPayroll_Id(payroll.getId());

        assertEquals(1, results.size());
        assertEquals("CASUAL", results.getFirst().getLeaveType());
    }

    @Test
    void payrollRepository_shouldCascadePersistChildrenAndDeleteOrphansWhenRemoved() {
        Payroll payroll = payroll(10L, 11, 2026);

        PayrollDeduction deduction = new PayrollDeduction();
        deduction.setDeductionType(DeductionType.OTHER);
        deduction.setAmount(new BigDecimal("100.00"));
        payroll.addDeduction(deduction);

        PayrollLeaveDetails leave = new PayrollLeaveDetails();
        leave.setLeaveType("SICK");
        leave.setTotalLeaves(1);
        leave.setAppliedDate(LocalDate.of(2026, 11, 2));
        payroll.addLeaveDetails(leave);

        Payroll saved = payrollRepository.saveAndFlush(payroll);

        assertNotNull(deduction.getId());
        assertNotNull(leave.getId());

        // Remove children from the managed entity.
        saved.getDeductions().clear();
        saved.getLeaveDetails().clear();

        // Flush the persistence context without merging the entity again.
        payrollRepository.flush();

        assertTrue(
                deductionRepository.findByPayroll_Id(saved.getId()).isEmpty()
        );

        assertTrue(
                leaveDetailsRepository.findByPayroll_Id(saved.getId()).isEmpty()
        );
    }

    private static Payroll payroll(Long employeeId, int month, int year) {
        Payroll payroll = new Payroll();
        payroll.setEmployeeId(employeeId);
        payroll.setEmployeeCode("EMP-" + employeeId);
        payroll.setEmployeeName("Employee " + employeeId);
        payroll.setEmploymentType("PERMANENT");
        payroll.setBankAccountNumber("12345678901234");
        payroll.setPayPeriodMonth(month);
        payroll.setPayPeriodYear(year);
        payroll.setCtc(new BigDecimal("720000.00"));
        payroll.setBasicSalary(new BigDecimal("50000.00"));
        payroll.setHra(new BigDecimal("10000.00"));
        payroll.setFixedBonus(BigDecimal.ZERO);
        payroll.setVariableBonus(BigDecimal.ZERO);
        payroll.setOtherAllowances(BigDecimal.ZERO);
        payroll.setGrossSalary(new BigDecimal("60000.00"));
        payroll.setEmployerPf(BigDecimal.ZERO);
        payroll.setTotalDeductions(BigDecimal.ZERO);
        payroll.setNetSalary(new BigDecimal("60000.00"));
        payroll.setPaymentStatus(PayrollStatus.DRAFT);
        return payroll;
    }
}
