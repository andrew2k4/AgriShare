package com.gaming.agricul.repository;

import com.gaming.agricul.ProductionDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionDayRepository extends JpaRepository<ProductionDay, Long> {
}
