package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.DistributionStatus;
import com.gaming.agricul.ProfitDistribution;
import com.gaming.agricul.ProfitDistributionLine;
import com.gaming.agricul.Withdrawal;
import com.gaming.agricul.WithdrawalStatus;
import com.gaming.agricul.dto.RoiDto;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.ProfitDistributionRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoiServiceTest {

    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private ProfitDistributionRepository profitDistributionRepository;
    @Mock
    private WithdrawalRepository withdrawalRepository;

    private RoiService roiService;

    @BeforeEach
    void setUp() {
        roiService = new RoiService(contributionRepository, profitDistributionRepository, withdrawalRepository);
    }

    @Test
    void compute_rejectsInvestorWithNoEquityLikeContribution() {
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> roiService.compute(1L, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void compute_excludesLoanContributionsFromTotalInvested() {
        Contribution equity = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.now().minusDays(365), BigDecimal.ZERO, null, null);
        Contribution loan = new Contribution(2L, 1L, 10L, ContributionType.LOAN,
                new BigDecimal("5000000"), LocalDate.now(), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(equity, loan));
        when(profitDistributionRepository.findByProjectId(1L)).thenReturn(List.of());
        when(withdrawalRepository.findByProjectIdAndInvestorId(1L, 10L)).thenReturn(List.of());

        RoiDto roi = roiService.compute(1L, 10L);

        assertThat(roi.totalInvested()).isEqualByComparingTo("1000000");
    }

    @Test
    void compute_positiveRoi_whenTotalReceivedExceedsInvestment() {
        Contribution equity = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.now().minusDays(365), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(equity));

        ProfitDistributionLine line = new ProfitDistributionLine(1L, 10L, BigDecimal.ONE, new BigDecimal("100"), new BigDecimal("1200000"));
        ProfitDistribution distribution = new ProfitDistribution(1L, 1L, LocalDate.now().minusDays(30), LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1200000"),
                DistributionStatus.VALIDATED, 1L, java.time.Instant.now(), 2L, java.time.Instant.now(), List.of(line), null);
        when(profitDistributionRepository.findByProjectId(1L)).thenReturn(List.of(distribution));
        when(withdrawalRepository.findByProjectIdAndInvestorId(1L, 10L)).thenReturn(List.of());

        RoiDto roi = roiService.compute(1L, 10L);

        // Received 1 200 000 on 1 000 000 invested -> ROI = (1 200 000 - 1 000 000) / 1 000 000 * 100 = 20%
        assertThat(roi.totalReceived()).isEqualByComparingTo("1200000");
        assertThat(roi.roiPercent()).isEqualByComparingTo("20.0000");
        assertThat(roi.annualizedRoiPercent()).isNotNull();
    }

    @Test
    void compute_includesValidatedWithdrawalsInTotalReceived() {
        Contribution equity = new Contribution(1L, 1L, 10L, ContributionType.EQUITY,
                new BigDecimal("1000000"), LocalDate.now().minusDays(100), BigDecimal.ZERO, null, null);
        when(contributionRepository.findByProjectId(1L)).thenReturn(List.of(equity));
        when(profitDistributionRepository.findByProjectId(1L)).thenReturn(List.of());

        Withdrawal validated = new Withdrawal(1L, 1L, 1L, 10L, new BigDecimal("100000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"), WithdrawalStatus.VALIDATED, null,
                2L, java.time.Instant.now(), 3L, java.time.Instant.now(), null);
        Withdrawal rejected = new Withdrawal(2L, 1L, 1L, 10L, new BigDecimal("50000"),
                null, null, null, WithdrawalStatus.REJECTED, null, 2L, java.time.Instant.now(), 3L, java.time.Instant.now(), null);
        when(withdrawalRepository.findByProjectIdAndInvestorId(1L, 10L)).thenReturn(List.of(validated, rejected));

        RoiDto roi = roiService.compute(1L, 10L);

        assertThat(roi.totalReceived()).isEqualByComparingTo("100000");
    }
}
