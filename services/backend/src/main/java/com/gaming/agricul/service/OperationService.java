package com.gaming.agricul.service;

import com.gaming.agricul.Operation;
import com.gaming.agricul.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationService {

    private final OperationRepository operationRepository;

    @Transactional(readOnly = true)
    public List<Operation> findAll() {
        return operationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Operation findById(Long id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operation not found with id: " + id));
    }

    public Operation save(Operation operation) {
        return operationRepository.save(operation);
    }

    public Operation update(Long id, Operation operation) {
        if (!operationRepository.existsById(id)) {
            throw new RuntimeException("Operation not found with id: " + id);
        }
        operation.setId(id);
        return operationRepository.save(operation);
    }

    public void deleteById(Long id) {
        if (!operationRepository.existsById(id)) {
            throw new RuntimeException("Operation not found with id: " + id);
        }
        operationRepository.deleteById(id);
    }
}
