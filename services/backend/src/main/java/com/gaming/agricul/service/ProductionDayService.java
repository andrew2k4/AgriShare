package com.gaming.agricul.service;

import com.gaming.agricul.exception.InvalidInputException;
import com.gaming.agricul.validation.InputRules;
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

    private static final int MAX_QUANTITY = 1_000_000;

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
        validate(productionDay);
        return productionDayRepository.save(productionDay);
    }

    public ProductionDay update(Long id, ProductionDay productionDay) {
        if (!productionDayRepository.existsById(id)) {
            throw new RuntimeException("ProductionDay not found with id: " + id);
        }
        validate(productionDay);
        productionDay.setId(id);
        return productionDayRepository.save(productionDay);
    }

    private static void validate(ProductionDay productionDay) {
        InputRules.pastOrToday(productionDay.getDate(), "date");
        // A day without production is a valid entry
        if (productionDay.getQuantity() < 0 || productionDay.getQuantity() > MAX_QUANTITY) {
            throw new InvalidInputException("quantity must be between 0 and " + MAX_QUANTITY);
        }
    }

    public void deleteById(Long id) {
        if (!productionDayRepository.existsById(id)) {
            throw new RuntimeException("ProductionDay not found with id: " + id);
        }
        productionDayRepository.deleteById(id);
    }
}
