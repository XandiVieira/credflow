package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository repository;
    private final AccountService accountService;
    private final UserService userService;

    public List<Category> findAll(Long accountId) {
        log.info("Fetching all categories for account {}", accountId);
        return repository.findAllByAccountId(accountId);
    }

    public Category findById(Long id, Long accountId) {
        log.info("Fetching category ID {} for account {}", id, accountId);
        return repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));
    }

    @Transactional
    public Category create(Category category, Long accountId) {
        var name = category.getName().trim();
        var normalized = name.toLowerCase();

        if (repository.findByNameIgnoreCaseAndAccountId(normalized, accountId).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Category with name '" + name + "' already exists for this account."
            );
        }

        var account = accountService.findById(accountId);
        category.setAccount(account);

        category.setDefaultResponsibles(
                resolveResponsiblesForAccount(category.getDefaultResponsibles(), accountId)
        );

        return repository.save(category);
    }

    private Set<User> resolveResponsiblesForAccount(
            Set<User> stubs, Long accountId) {

        if (stubs == null || stubs.isEmpty()) return java.util.Collections.emptySet();

        var ids = stubs.stream().map(User::getId).toList();

        var users = new java.util.LinkedHashSet<User>();
        for (Long id : ids) {
            var u = userService.findById(id);
            if (!u.getAccount().getId().equals(accountId)) {
                throw new IllegalArgumentException(
                        "User " + id + " does not belong to this account."
                );
            }
            users.add(u);
        }
        return users;
    }

    @Transactional
    public Category update(Long id, Category updated, Long accountId) {
        var newName = updated.getName() != null ? updated.getName().trim() : "";
        if (newName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank.");
        }

        log.info("Updating category ID {} for accountId {}", id, accountId);

        repository.findByNameIgnoreCaseAndAccountId(newName.toLowerCase(), accountId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(_ -> {
                    throw new ResourceAlreadyExistsException("A category with name '" + newName + "' already exists.");
                });

        var category = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));

        category.setName(newName);
        category.setAccount(accountService.findById(accountId));

        var incoming = updated.getDefaultResponsibles();
        if (incoming != null) {
            var resolved = resolveResponsiblesForAccount(incoming, accountId);
            category.getDefaultResponsibles().clear();
            category.getDefaultResponsibles().addAll(resolved);
        }

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

    public List<Category> findAllByAccount(Long accountId) {
        log.info("Fetching all categories for account {}", accountId);
        return repository.findAllByAccountId(accountId);
    }
}