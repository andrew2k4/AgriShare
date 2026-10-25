package com.gaming.agricul.mapper;

import com.gaming.agricul.Repayment;
import com.gaming.agricul.dto.RepaymentDto;
import org.springframework.stereotype.Component;

@Component
public class RepaymentMapper {

    public RepaymentDto toDto(Repayment repayment) {
        return new RepaymentDto(
                repayment.getId(),
                repayment.getContributionId(),
                repayment.getPrincipalAmount(),
                repayment.getInterestAmount(),
                repayment.getDueDate(),
                repayment.getStatus(),
                repayment.getPaidAt()
        );
    }
}
