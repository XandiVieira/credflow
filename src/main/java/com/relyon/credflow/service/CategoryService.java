package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final AccountService accountService;

    public List<Category> findAll() {
        return repository.findAll();
    }

    public Optional<Category> findById(Long id) {
        return repository.findById(id);
    }

    public Category create(Category category, AuthenticatedUser authenticatedUser) {
        var categoryName = category.getName().trim();
        var normalized = categoryName.toLowerCase();

        boolean exists = repository.findAll()
                .stream()
                .map(cat -> cat.getName().trim().toLowerCase())
                .anyMatch(existingName -> existingName.equals(normalized));

        if (exists) {
            throw new ResourceAlreadyExistsException("Category with name '" + categoryName + "' already exists.");
        }

        var account = accountService.findById(authenticatedUser.getUser().getId());

        var categoryToSave = Category.builder()
                .name(categoryName)
                .account(account)
                .build();

        return repository.save(categoryToSave);
    }

    public Category update(Long id, Category updated) {
        var newName = updated.getName().trim();

        var existingByName = repository.findByNameIgnoreCase(newName);
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResourceAlreadyExistsException("A category with name '" + newName + "' already exists.");
        }

        return repository.findById(id)
                .map(existing -> {
                    existing.setName(newName);
                    existing.setDefaultResponsible(updated.getDefaultResponsible());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Category with ID " + id + " not found.");
        }
        repository.deleteById(id);
    }
}