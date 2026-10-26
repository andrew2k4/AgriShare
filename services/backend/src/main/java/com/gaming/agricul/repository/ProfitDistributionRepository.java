package com.gaming.agricul.repository;

import com.gaming.agricul.ProfitDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfitDistributionRepository extends JpaRepository<ProfitDistribution, Long> {

    List<ProfitDistribution> findByProjectId(Long projectId);
}
