package com.gaming.agricul.service;

import com.gaming.agricul.CapitalDistribution;
import com.gaming.agricul.repository.CapitalDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CapitalDistributionService {

    private final CapitalDistributionRepository capitalDistributionRepository;

    @Transactional(readOnly = true)
    public List<CapitalDistribution> findAll() {
        return capitalDistributionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CapitalDistribution findById(Long id) {
        return capitalDistributionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CapitalDistribution not found with id: " + id));
    }

    public CapitalDistribution save(CapitalDistribution capitalDistribution) {
        return capitalDistributionRepository.save(capitalDistribution);
    }

    public CapitalDistribution update(Long id, CapitalDistribution capitalDistribution) {
        if (!capitalDistributionRepository.existsById(id)) {
            throw new RuntimeException("CapitalDistribution not found with id: " + id);
        }
        capitalDistribution.setId(id);
        return capitalDistributionRepository.save(capitalDistribution);
    }

    public void deleteById(Long id) {
        if (!capitalDistributionRepository.existsById(id)) {
            throw new RuntimeException("CapitalDistribution not found with id: " + id);
        }
        capitalDistributionRepository.deleteById(id);
    }
}
