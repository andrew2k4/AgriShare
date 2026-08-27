package com.gaming.agricul.service;

import com.gaming.agricul.ProductionDay;
import com.gaming.agricul.repository.ProductionDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductionDayService {

    private final ProductionDayRepository productionDayRepository;

    @Transactional(readOnly = true)
    public List<ProductionDay> findAll() {
        return productionDayRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ProductionDay findById(Long id) {
        return productionDayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductionDay not found with id: " + id));
    }

    public ProductionDay save(ProductionDay productionDay) {
        return productionDayRepository.save(productionDay);
    }

    public ProductionDay update(Long id, ProductionDay productionDay) {
        if (!productionDayRepository.existsById(id)) {
            throw new RuntimeException("ProductionDay not found with id: " + id);
        }
        productionDay.setId(id);
        return productionDayRepository.save(productionDay);
    }

    public void deleteById(Long id) {
        if (!productionDayRepository.existsById(id)) {
            throw new RuntimeException("ProductionDay not found with id: " + id);
        }
        productionDayRepository.deleteById(id);
    }
}
