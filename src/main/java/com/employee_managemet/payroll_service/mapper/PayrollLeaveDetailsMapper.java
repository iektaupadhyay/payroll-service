
package com.employee_managemet.payroll_service.mapper;

import com.employee_managemet.payroll_service.dto.response.PayrollLeaveDetailsResponse;
import com.employee_managemet.payroll_service.model.PayrollLeaveDetails;
import org.springframework.stereotype.Component;

@Component
public class PayrollLeaveDetailsMapper {

    public PayrollLeaveDetailsResponse toResponse(
            PayrollLeaveDetails leaveDetails) {

        return new PayrollLeaveDetailsResponse(
                leaveDetails.getId(),
                leaveDetails.getLeaveType(),
                leaveDetails.getTotalLeaves(),
                leaveDetails.getAppliedDate()
        );
    }
}
