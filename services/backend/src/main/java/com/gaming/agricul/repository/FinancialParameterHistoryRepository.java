package com.gaming.agricul.repository;

import com.gaming.agricul.FinancialParameterHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialParameterHistoryRepository extends JpaRepository<FinancialParameterHistory, Long> {
}
