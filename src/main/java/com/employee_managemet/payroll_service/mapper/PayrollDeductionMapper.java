
package com.employee_managemet.payroll_service.mapper;

import com.employee_managemet.payroll_service.dto.request.PayrollDeductionRequest;
import com.employee_managemet.payroll_service.dto.response.PayrollDeductionResponse;
import com.employee_managemet.payroll_service.model.PayrollDeduction;
import org.springframework.stereotype.Component;

@Component
public class PayrollDeductionMapper {

    public PayrollDeduction toEntity(PayrollDeductionRequest request) {

        PayrollDeduction deduction = new PayrollDeduction();

        deduction.setDeductionType(request.deductionType());
        deduction.setAmount(request.amount());
        deduction.setDescription(request.description());

        return deduction;
    }

    public PayrollDeductionResponse toResponse(PayrollDeduction deduction) {

        return new PayrollDeductionResponse(
                deduction.getId(),
                deduction.getDeductionType(),
                deduction.getAmount(),
                deduction.getDescription()
        );
    }
}
