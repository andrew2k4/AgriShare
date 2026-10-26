package com.gaming.agricul.repository;

import com.gaming.agricul.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByProjectId(Long projectId);

    List<Withdrawal> findByProjectIdAndInvestorId(Long projectId, Long investorId);

    @Query("SELECT COALESCE(SUM(w.requestedAmount), 0) FROM Withdrawal w " +
            "WHERE w.contributionId = :contributionId AND w.status = 'VALIDATED'")
    BigDecimal sumValidatedAmountByContributionId(@Param("contributionId") Long contributionId);

    @Query("SELECT COALESCE(SUM(w.requestedAmount), 0) FROM Withdrawal w " +
            "WHERE w.projectId = :projectId AND w.status = 'VALIDATED'")
    BigDecimal sumValidatedAmountByProjectId(@Param("projectId") Long projectId);
}
