package com.gaming.agricul.service;

import com.gaming.agricul.validation.InputRules;
import com.gaming.agricul.Investor;
import com.gaming.agricul.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvestorService {

    private final InvestorRepository investorRepository;

    @Transactional(readOnly = true)
    public List<Investor> findAll() {
        return investorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Investor findById(Long id) {
        return investorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investor not found with id: " + id));
    }

    public Investor save(Investor investor) {
        validate(investor);
        return investorRepository.save(investor);
    }

    public Investor update(Long id, Investor investor) {
        if (!investorRepository.existsById(id)) {
            throw new RuntimeException("Investor not found with id: " + id);
        }
        validate(investor);
        investor.setId(id);
        return investorRepository.save(investor);
    }

    private static void validate(Investor investor) {
        investor.setFullName(InputRules.text(investor.getFullName(), "fullName", InputRules.MAX_TEXT));
        // An investor can be registered before their first contribution
        InputRules.number(investor.getContribution(), "contribution", InputRules.MAX_AMOUNT.doubleValue(), true);
    }

    public void deleteById(Long id) {
        if (!investorRepository.existsById(id)) {
            throw new RuntimeException("Investor not found with id: " + id);
        }
        investorRepository.deleteById(id);
    }
}
