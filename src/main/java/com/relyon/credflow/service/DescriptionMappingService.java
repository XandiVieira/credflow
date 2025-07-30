package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.utils.DescriptionNormalizer;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class DescriptionMappingService {

    private final DescriptionMappingRepository repository;
    private final TransactionService transactionService;

    public List<DescriptionMapping> createAll(List<DescriptionMapping> mappings, AuthenticatedUser authenticatedUser) {
        var account = authenticatedUser.getUser().getAccount();
        log.info("Creating {} description mappings in batch", mappings.size());

        var savedMappings = mappings.stream()
                .filter(mapping -> isNewMapping(mapping.getOriginalDescription()))
                .map(mapping -> {
                    mapping.setOriginalDescription(DescriptionNormalizer.normalize(mapping.getOriginalDescription()));
                    mapping.setSimplifiedDescription(DescriptionNormalizer.normalize(mapping.getSimplifiedDescription()));
                    return saveMappingWithAccount(mapping, account);
                })
                .toList();

        savedMappings.forEach(saved ->
                transactionService.applyMappingToExistingTransactions(
                        account,
                        saved.getOriginalDescription(),
                        saved.getSimplifiedDescription(),
                        saved.getCategory()
                )
        );

        return savedMappings;
    }

    public DescriptionMapping update(Long id, DescriptionMapping updated, AuthenticatedUser authenticatedUser) {
        var account = authenticatedUser.getUser().getAccount();
        log.info("Updating DescriptionMapping ID {}", id);

        normalizeMapping(updated);

        return repository.findById(id)
                .map(existing -> applyChangesAndSave(existing, updated, account))
                .orElseThrow(() -> notFound(id));
    }

    public List<DescriptionMapping> findAll() {
        log.debug("Fetching all DescriptionMappings");
        return repository.findAll();
    }

    public Optional<DescriptionMapping> findById(Long id) {
        log.debug("Searching for DescriptionMapping by ID: {}", id);
        return repository.findById(id);
    }

    public Optional<DescriptionMapping> findByOriginalDescription(String description) {
        log.debug("Searching for DescriptionMapping by original description: {}", description);
        return repository.findByOriginalDescriptionIgnoreCase(description);
    }

    public void delete(Long id) {
        log.info("Deleting DescriptionMapping ID {}", id);
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    private boolean isNewMapping(String originalDescription) {
        var normalized = DescriptionNormalizer.normalize(originalDescription);
        var exists = repository.existsByOriginalDescriptionIgnoreCase(normalized);
        if (exists) {
            log.warn("Mapping already exists for '{}', skipping", normalized);
        }
        return !exists;
    }

    private DescriptionMapping saveMappingWithAccount(DescriptionMapping mapping, Account account) {
        mapping.setAccount(account);
        return repository.save(mapping);
    }

    private void updateExistingTransactions(Account account, DescriptionMapping mapping) {
        transactionService.applyMappingToExistingTransactions(
                account,
                mapping.getOriginalDescription(),
                mapping.getSimplifiedDescription(),
                mapping.getCategory()
        );
    }

    private DescriptionMapping applyChangesAndSave(DescriptionMapping existing, DescriptionMapping updated, Account account) {
        existing.setOriginalDescription(DescriptionNormalizer.normalize(updated.getOriginalDescription()));
        existing.setSimplifiedDescription(DescriptionNormalizer.normalize(updated.getSimplifiedDescription()));
        existing.setCategory(updated.getCategory());

        var saved = repository.save(existing);
        updateExistingTransactions(account, saved);
        return saved;
    }

    private void normalizeMapping(DescriptionMapping mapping) {
        if (mapping.getOriginalDescription() != null) {
            mapping.setOriginalDescription(DescriptionNormalizer.normalize((mapping.getOriginalDescription())));
        }
        if (mapping.getSimplifiedDescription() != null) {
            mapping.setSimplifiedDescription(DescriptionNormalizer.normalize((mapping.getSimplifiedDescription())));
        }
    }

    private ResourceNotFoundException notFound(Long id) {
        log.error("Mapping not found for ID {}", id);
        return new ResourceNotFoundException("Mapping not found with ID " + id);
    }
}