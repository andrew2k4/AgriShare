package com.gaming.agricul.service;

import com.gaming.agricul.Contribution;
import com.gaming.agricul.ContributionType;
import com.gaming.agricul.Repayment;
import com.gaming.agricul.RepaymentStatus;
import com.gaming.agricul.exception.BusinessRuleException;
import com.gaming.agricul.repository.ContributionRepository;
import com.gaming.agricul.repository.OperationRepository;
import com.gaming.agricul.repository.RepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    private RepaymentRepository repaymentRepository;
    @Mock
    private ContributionRepository contributionRepository;
    @Mock
    private OperationRepository operationRepository;

    private RepaymentService repaymentService;

    @BeforeEach
    void setUp() {
        repaymentService = new RepaymentService(repaymentRepository, contributionRepository, operationRepository);
    }

    private Contribution loan() {
        return new Contribution(1L, 100L, 10L, ContributionType.LOAN, new BigDecimal("2000000"), LocalDate.now(), BigDecimal.ZERO, null, null);
    }

    @Test
    void schedule_rejectsEquityLikeContribution() {
        Contribution equity = new Contribution(1L, 100L, 10L, ContributionType.EQUITY, BigDecimal.TEN, LocalDate.now(), BigDecimal.ZERO, null, null);
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(equity));

        assertThatThrownBy(() -> repaymentService.schedule(1L, new BigDecimal("2000000"), BigDecimal.ZERO, LocalDate.now()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void schedule_succeedsForLoan() {
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(loan()));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Repayment repayment = repaymentService.schedule(1L, new BigDecimal("2000000"), new BigDecimal("100000"), LocalDate.now());

        assertThat(repayment.getStatus()).isEqualTo(RepaymentStatus.PENDING);
    }

    @Test
    void markPaid_postsExpenseOperationForPrincipalPlusInterest() {
        Repayment pending = new Repayment(1L, 1L, new BigDecimal("2000000"), new BigDecimal("100000"), LocalDate.now(), RepaymentStatus.PENDING, null);
        when(repaymentRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(contributionRepository.findById(1L)).thenReturn(Optional.of(loan()));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<com.gaming.agricul.Operation> captor = ArgumentCaptor.forClass(com.gaming.agricul.Operation.class);

        Repayment result = repaymentService.markPaid(1L);

        assertThat(result.getStatus()).isEqualTo(RepaymentStatus.PAID);
        verify(operationRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(2100000.0);
    }

    @Test
    void markPaid_rejectsAlreadyPaidRepayment() {
        Repayment paid = new Repayment(1L, 1L, new BigDecimal("2000000"), BigDecimal.ZERO, LocalDate.now(), RepaymentStatus.PAID, java.time.Instant.now());
        when(repaymentRepository.findById(1L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> repaymentService.markPaid(1L)).isInstanceOf(BusinessRuleException.class);
    }
}
