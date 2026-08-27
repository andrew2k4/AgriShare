package com.gaming.agricul.repository;

import com.gaming.agricul.CapitalDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CapitalDistributionRepository extends JpaRepository<CapitalDistribution, Long> {
}
