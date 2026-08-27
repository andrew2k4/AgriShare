package com.gaming.agricul.service;

import com.gaming.agricul.StockInventory;
import com.gaming.agricul.repository.StockInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StockInventoryService {

    private final StockInventoryRepository stockInventoryRepository;

    @Transactional(readOnly = true)
    public List<StockInventory> findAll() {
        return stockInventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public StockInventory findById(Long id) {
        return stockInventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockInventory not found with id: " + id));
    }

    public StockInventory save(StockInventory stockInventory) {
        return stockInventoryRepository.save(stockInventory);
    }

    public StockInventory update(Long id, StockInventory stockInventory) {
        if (!stockInventoryRepository.existsById(id)) {
            throw new RuntimeException("StockInventory not found with id: " + id);
        }
        stockInventory.setId(id);
        return stockInventoryRepository.save(stockInventory);
    }

    public void deleteById(Long id) {
        if (!stockInventoryRepository.existsById(id)) {
            throw new RuntimeException("StockInventory not found with id: " + id);
        }
        stockInventoryRepository.deleteById(id);
    }
}
