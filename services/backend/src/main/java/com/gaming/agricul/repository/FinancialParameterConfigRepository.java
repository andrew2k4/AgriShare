package com.gaming.agricul.repository;

import com.gaming.agricul.FinancialParameterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialParameterConfigRepository extends JpaRepository<FinancialParameterConfig, Long> {

    Optional<FinancialParameterConfig> findByParamKeyAndProjectId(String paramKey, Long projectId);

    Optional<FinancialParameterConfig> findByParamKeyAndProjectIdIsNull(String paramKey);

    List<FinancialParameterConfig> findByProjectIdIsNull();
}
