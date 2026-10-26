package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.Withdrawal;
import com.gaming.agricul.WithdrawalStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.exception.NotFoundException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.ProjectRepository;
import com.gaming.agricul.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;
    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private ContributionService contributionService;
    @Mock
    private FinancialParameterService financialParameterService;

    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        withdrawalService = new WithdrawalService(
                withdrawalRepository, contributionRepository, projectRepository, operationRepository,
                contributionService, financialParameterService);
        lenient().when(withdrawalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Contribution contribution(Long id, Long projectId, BigDecimal amount) {
        return new Contribution(id, projectId, 10L, ContributionType.EQUITY, amount, LocalDate.now(), BigDecimal.ZERO, null, null);
    }

    @Test
    void request_isAcceptedWhenProjectStaysAboveMinimumValue() {
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(contribution(1L, 100L, new BigDecimal("1000000"))));
        when(withdrawalRepository.sumValidatedAmountByContributionId(1L)).thenReturn(BigDecimal.ZERO);
        when(contributionService.currentProjectValue(100L)).thenReturn(new BigDecimal("5000000"));
        when(financialParameterService.resolveOrDefault(FinancialParameterService.MIN_PROJECT_VALUE_KEY, 100L, BigDecimal.ZERO))
                .thenReturn(BigDecimal.ZERO);

        Withdrawal result = withdrawalService.request(1L, new BigDecimal("500000"), 1L);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.REQUESTED);
    }

    @Test
    void request_isBlockedWhenItWouldBreachTheMinimumProjectValue() {
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(contribution(1L, 100L, new BigDecimal("1000000"))));
        when(withdrawalRepository.sumValidatedAmountByContributionId(1L)).thenReturn(BigDecimal.ZERO);
        when(contributionService.currentProjectValue(100L)).thenReturn(new BigDecimal("1000000"));
        when(financialParameterService.resolveOrDefault(FinancialParameterService.MIN_PROJECT_VALUE_KEY, 100L, BigDecimal.ZERO))
                .thenReturn(new BigDecimal("800000"));

        // Withdrawing 500 000 from a project worth 1 000 000 with a 800 000 floor -> projected 500 000 < 800 000 -> blocked
        Withdrawal result = withdrawalService.request(1L, new BigDecimal("500000"), 1L);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.BLOCKED);
        assertThat(result.getBlockReason()).isNotBlank();
    }

    @Test
    void request_rejectsAmountExceedingRemainingPosition() {
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(contribution(1L, 100L, new BigDecimal("1000000"))));
        when(withdrawalRepository.sumValidatedAmountByContributionId(1L)).thenReturn(new BigDecimal("600000"));

        assertThatThrownBy(() -> withdrawalService.request(1L, new BigDecimal("500000"), 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void request_rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> withdrawalService.request(1L, BigDecimal.ZERO, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void request_rejectsUnknownContribution() {
        when(contributionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.request(99L, BigDecimal.TEN, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validate_appliesPenaltyRateAndPostsAnExpenseOperation() {
        Withdrawal requested = requestedWithdrawal(1L, new BigDecimal("500000"));
        when(withdrawalRepository.findById(200L)).thenReturn(Optional.of(requested));
        when(financialParameterService.resolveOrDefault(FinancialParameterService.WITHDRAWAL_PENALTY_RATE_KEY, 100L, BigDecimal.ZERO))
                .thenReturn(new BigDecimal("0.10"));

        Withdrawal result = withdrawalService.validate(200L, 2L);

        assertThat(result.getStatus()).isEqualTo(WithdrawalStatus.VALIDATED);
        assertThat(result.getResultingLoss()).isEqualByComparingTo("50000.0000");
        assertThat(result.getNetAmountPaid()).isEqualByComparingTo("450000.0000");
        verify(operationRepository).save(any());
    }

    @Test
    void validate_requiresADifferentPersonThanTheRequester() {
        Withdrawal requested = requestedWithdrawal(1L, new BigDecimal("500000"));
        when(withdrawalRepository.findById(200L)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> withdrawalService.validate(200L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void validate_rejectsBlockedWithdrawal() {
        Withdrawal blocked = requestedWithdrawal(1L, new BigDecimal("500000"));
        blocked.setStatus(WithdrawalStatus.BLOCKED);
        when(withdrawalRepository.findById(200L)).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> withdrawalService.validate(200L, 2L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reject_requiresADifferentPersonThanTheRequester() {
        Withdrawal requested = requestedWithdrawal(1L, new BigDecimal("500000"));
        when(withdrawalRepository.findById(200L)).thenReturn(Optional.of(requested));

        assertThatThrownBy(() -> withdrawalService.reject(200L, 1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Withdrawal requestedWithdrawal(Long requestedBy, BigDecimal amount) {
        return new Withdrawal(200L, 1L, 100L, 10L, amount,
                null, null, null, WithdrawalStatus.REQUESTED, null,
                requestedBy, Instant.now(), null, null, null);
    }
}
