package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class DescriptionMappingService {

    private final DescriptionMappingRepository repository;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final LocalizedMessageTranslationService translationService;

    @Transactional
    public List<DescriptionMapping> createAll(List<DescriptionMapping> mappings, Long accountId) {
        log.info("Creating {} description mappings for account {}", mappings.size(), accountId);

        var savedMappings = mappings.stream()
                .filter(mapping -> isNewMappingForAccount(mapping.getOriginalDescription(), accountId))
                .map(mapping -> {
                    normalizeMapping(mapping);
                    mapping.setAccount(accountService.findById(accountId));
                    mapping.setCategory(categoryService.findById(mapping.getCategory().getId(), accountId));
                    return saveMappingWithAccount(mapping, accountId);
                })
                .toList();

        savedMappings.forEach(saved -> applyMappingToTransactions(accountId, saved));

        return savedMappings;
    }

    @Transactional
    public DescriptionMapping update(Long id, DescriptionMapping updated, Long accountId) {
        log.info("Updating description mapping ID {} for account {}", id, accountId);

        normalizeMapping(updated);

        return repository.findByIdAndAccountId(id, accountId)
                .map(existing -> applyChangesAndSave(existing, updated, accountId))
                .orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public Page<DescriptionMapping> findAll(Long accountId, Boolean onlyIncomplete, int page, int size) {
        log.info("Fetching {} description mappings for account {} (page={}, size={})",
                Boolean.TRUE.equals(onlyIncomplete) ? "incomplete" : "all",
                accountId, page, size);

        var pageable = PageRequest.of(page, size);

        if (Boolean.TRUE.equals(onlyIncomplete)) {
            return repository.findAllByAccountIdAndCategoryIsNull(accountId, pageable);
        }

        return repository.findAllByAccountId(accountId, pageable);
    }


    @Transactional(readOnly = true)
    public Optional<DescriptionMapping> findById(Long id, Long accountId) {
        log.info("Fetching description mapping ID {} for account {}", id, accountId);
        return repository.findByIdAndAccountId(id, accountId);
    }

    @Transactional(readOnly = true)
    public Optional<DescriptionMapping> findByNormalizedDescription(String description, Long accountId) {
        var normalized = NormalizationUtils.normalizeDescription(description);
        log.info("Searching for mapping by description '{}' (normalized '{}') for account {}",
                description, normalized, accountId);
        return repository.findByNormalizedDescriptionAndAccountId(normalized, accountId);
    }

    @Transactional
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
        existing.setCategory(categoryService.findById(updated.getCategory().getId(), accountId));
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
        return new ResourceNotFoundException("resource.mapping.notFound", id);
    }
}