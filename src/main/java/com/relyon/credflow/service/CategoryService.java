package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository repository;
    private final AccountService accountService;

    public List<Category> findAll(Long accountId) {
        log.info("Fetching all categories for account {}", accountId);
        return repository.findAllByAccountId(accountId);
    }

    public Optional<Category> findById(Long id, Long accountId) {
        log.info("Fetching category ID {} for account {}", id, accountId);
        return repository.findByIdAndAccountId(id, accountId);
    }

    public Category create(Category category, Long accountId) {
        var categoryName = category.getName().trim();
        var normalized = categoryName.toLowerCase();

        log.info("Creating category '{}' for account {}", categoryName, accountId);

        var exists = repository.findByNameIgnoreCaseAndAccountId(normalized, accountId).isPresent();
        if (exists) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryName + "' already exists fpr this account.");
        }

        category.setAccount(accountService.findById(accountId));

        var saved = repository.save(category);
        log.info("Category '{}' created with ID {}", saved.getName(), saved.getId());
        return saved;
    }

    public Category update(Long id, Category updated, Long accountId) {
        var newName = updated.getName().trim();
        log.info("Updating category ID {} for accountId {}", id, accountId);

        repository.findByNameIgnoreCaseAndAccountId(newName.toLowerCase(), accountId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistsException("A category with name '" + newName + "' already exists.");
                });

        var category = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));

        category.setName(newName);
        category.setDefaultResponsible(updated.getDefaultResponsible());
        category.setAccount(accountService.findById(accountId));

        var saved = repository.save(category);
        log.info("Category ID {} updated", saved.getId());
        return saved;
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting category ID {} for account {}", id, accountId);

        var category = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));

        repository.delete(category);
        log.info("Category ID {} deleted", id);
    }
}