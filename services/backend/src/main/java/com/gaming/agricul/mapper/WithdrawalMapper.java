package com.gaming.agricul.mapper;

import com.gaming.agricul.Withdrawal;
import com.gaming.agricul.dto.WithdrawalDto;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalMapper {

    public WithdrawalDto toDto(Withdrawal withdrawal) {
        return new WithdrawalDto(
                withdrawal.getId(),
                withdrawal.getContributionId(),
                withdrawal.getProjectId(),
                withdrawal.getInvestorId(),
                withdrawal.getRequestedAmount(),
                withdrawal.getPenaltyRate(),
                withdrawal.getResultingLoss(),
                withdrawal.getNetAmountPaid(),
                withdrawal.getStatus(),
                withdrawal.getBlockReason(),
                withdrawal.getRequestedBy(),
                withdrawal.getRequestedAt(),
                withdrawal.getValidatedBy(),
                withdrawal.getValidatedAt()
        );
    }
}
