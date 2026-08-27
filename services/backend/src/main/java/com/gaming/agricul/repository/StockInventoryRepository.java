package com.gaming.agricul.repository;

import com.gaming.agricul.StockInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockInventoryRepository extends JpaRepository<StockInventory, Long> {
}
