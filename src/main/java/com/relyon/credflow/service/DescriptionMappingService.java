package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class DescriptionMappingService {

    private final DescriptionMappingRepository repository;
    private final TransactionService transactionService;
    private final AccountService accountService;

    public List<DescriptionMapping> createAll(List<DescriptionMapping> mappings, Long accountId) {
        log.info("Creating {} description mappings for account {}", mappings.size(), accountId);

        var savedMappings = mappings.stream()
                .filter(mapping -> isNewMappingForAccount(mapping.getOriginalDescription(), accountId))
                .map(mapping -> {
                    normalizeMapping(mapping);
                    mapping.setAccount(accountService.findById(accountId));
                    return saveMappingWithAccount(mapping, accountId);
                })
                .toList();

        savedMappings.forEach(saved -> applyMappingToTransactions(accountId, saved));

        return savedMappings;
    }

    public DescriptionMapping update(Long id, DescriptionMapping updated, Long accountId) {
        log.info("Updating description mapping ID {} for account {}", id, accountId);

        normalizeMapping(updated);

        return repository.findByIdAndAccountId(id, accountId)
                .map(existing -> applyChangesAndSave(existing, updated, accountId))
                .orElseThrow(() -> notFound(id));
    }

    public List<DescriptionMapping> findAll(Long accountId, Boolean onlyIncomplete) {
        log.info("Fetching {} description mappings for account {}",
                Boolean.TRUE.equals(onlyIncomplete) ? "incomplete" : "all",
                accountId);

        var allMappings = repository.findAllByAccountId(accountId);

        if (Boolean.TRUE.equals(onlyIncomplete)) {
            return allMappings.stream()
                    .filter(DescriptionMapping::isIncomplete)
                    .toList();
        }

        return allMappings;
    }


    public Optional<DescriptionMapping> findById(Long id, Long accountId) {
        log.info("Fetching description mapping ID {} for account {}", id, accountId);
        return repository.findByIdAndAccountId(id, accountId);
    }

    public Optional<DescriptionMapping> findByNormalizedDescription(String description, Long accountId) {
        var normalized = NormalizationUtils.normalizeDescription(description);
        log.info("Searching for mapping by description '{}' (normalized '{}') for account {}",
                description, normalized, accountId);
        return repository.findByNormalizedDescriptionAndAccountId(normalized, accountId);
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting description mapping ID {} for account {}", id, accountId);

        var mapping = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> notFound(id));

        repository.delete(mapping);
        log.info("Successfully deleted mapping ID {}", id);
    }

    private boolean isNewMappingForAccount(String originalDescription, Long accountId) {
        var normalized = NormalizationUtils.normalizeDescription(originalDescription);
        var exists = repository.findByNormalizedDescriptionAndAccountId(normalized, accountId).isPresent();

        if (exists) {
            log.warn("Mapping already exists for '{}' in account {}", normalized, accountId);
        }

        return !exists;
    }

    private DescriptionMapping saveMappingWithAccount(DescriptionMapping mapping, Long accountId) {
        mapping.setAccount(accountService.findById(accountId));
        return repository.save(mapping);
    }

    private void applyMappingToTransactions(Long accountId, DescriptionMapping mapping) {
        transactionService.applyMappingToExistingTransactions(
                accountId,
                mapping.getOriginalDescription(),
                mapping.getSimplifiedDescription(),
                mapping.getCategory()
        );
    }

    private DescriptionMapping applyChangesAndSave(DescriptionMapping existing, DescriptionMapping updated, Long accountId) {
        existing.setOriginalDescription(updated.getOriginalDescription());
        existing.setSimplifiedDescription(updated.getSimplifiedDescription());
        existing.setCategory(updated.getCategory());
        existing.setAccount(accountService.findById(accountId));

        var saved = repository.save(existing);
        applyMappingToTransactions(accountId, saved);
        return saved;
    }

    private void normalizeMapping(DescriptionMapping mapping) {
        if (mapping.getOriginalDescription() != null) {
            mapping.setNormalizedDescription(
                    NormalizationUtils.normalizeDescription(mapping.getOriginalDescription())
            );
        }
    }

    private ResourceNotFoundException notFound(Long id) {
        log.error("Mapping not found with ID {}", id);
        return new ResourceNotFoundException("Mapping not found with ID " + id);
    }
}