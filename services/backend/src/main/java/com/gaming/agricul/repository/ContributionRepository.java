package com.gaming.agricul.repository;

import com.gaming.agricul.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByProjectId(Long projectId);

    List<Contribution> findByInvestorId(Long investorId);
}
