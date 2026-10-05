package com.gaming.agricul.repository;

import com.gaming.agricul.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {

    List<Operation> findByProjectId(Long projectId);

    List<Operation> findByProjectIdAndDateBetween(Long projectId, LocalDate start, LocalDate end);
}
