package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryResponseDTO;
import com.relyon.credflow.model.category.CategorySelectDTO;
import com.relyon.credflow.model.mapper.CategoryMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository repository;
    private final AccountService accountService;
    private final UserService userService;
    private final CategoryMapper categoryMapper;
    private final LocalizedMessageTranslationService translationService;

    public List<Category> findAll(Long accountId) {
        log.info("Fetching all categories for account {}", accountId);
        return repository.findAllByAccountId(accountId);
    }

    public Category findById(Long id, Long accountId) {
        log.info("Fetching category ID {} for account {}", id, accountId);
        return repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound", id));
    }

    @Transactional
    public Category create(Category category, Long accountId) {
        var name = category.getName().trim();
        var normalized = name.toLowerCase();

        if (repository.findByNameIgnoreCaseAndAccountId(normalized, accountId).isPresent()) {
            throw new ResourceAlreadyExistsException("category.duplicate", name);
        }

        var account = accountService.findById(accountId);
        category.setAccount(account);

        category.setDefaultResponsibles(
                resolveResponsiblesForAccount(category.getDefaultResponsibles(), accountId)
        );

        if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
            var parentCategory = validateAndResolveParentCategory(
                    category.getParentCategory().getId(),
                    accountId
            );
            category.setParentCategory(parentCategory);
        }

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
                throw new IllegalArgumentException(translationService.translateMessage("user.accountMismatch", id));
            }
            users.add(u);
        }
        return users;
    }

    @Transactional
    public Category update(Long id, Category updated, Long accountId) {
        var newName = updated.getName() != null ? updated.getName().trim() : "";
        if (newName.isBlank()) {
            throw new IllegalArgumentException(translationService.translateMessage("category.nameBlank"));
        }

        log.info("Updating category ID {} for accountId {}", id, accountId);

        repository.findByNameIgnoreCaseAndAccountId(newName.toLowerCase(), accountId)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(_ -> {
                    throw new ResourceAlreadyExistsException("category.duplicate", newName);
                });

        var category = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound", id));

        category.setName(newName);
        category.setAccount(accountService.findById(accountId));

        var incoming = updated.getDefaultResponsibles();
        if (incoming != null) {
            var resolved = resolveResponsiblesForAccount(incoming, accountId);
            category.getDefaultResponsibles().clear();
            category.getDefaultResponsibles().addAll(resolved);
        }

        if (updated.getParentCategory() != null) {
            Long parentId = updated.getParentCategory().getId();
            if (parentId != null) {
                var parentCategory = validateAndResolveParentCategory(parentId, accountId, id);
                category.setParentCategory(parentCategory);
            } else {
                category.setParentCategory(null);
            }
        }

        var saved = repository.save(category);
        log.info("Category ID {} updated", saved.getId());
        return saved;
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting category ID {} for account {}", id, accountId);

        var category = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound", id));

        repository.delete(category);
        log.info("Category ID {} deleted", id);
    }

    public Page<CategoryResponseDTO> findAllByAccountHierarchical(Long accountId, int page, int size) {
        log.info("Fetching categories hierarchically for account {} (page={}, size={})", accountId, page, size);

        var allCategories = repository.findAllByAccountId(accountId);

        var allDtos = allCategories.stream()
                .map(categoryMapper::toDto)
                .toList();

        Map<Long, List<CategoryResponseDTO>> childrenByParentId = allDtos.stream()
                .filter(dto -> dto.getParentCategoryId() != null)
                .collect(Collectors.groupingBy(CategoryResponseDTO::getParentCategoryId));

        var parentCategories = allDtos.stream()
                .filter(dto -> dto.getParentCategoryId() == null)
                .peek(parent -> {
                    List<CategoryResponseDTO> children = childrenByParentId.getOrDefault(parent.getId(), new ArrayList<>());
                    parent.setChildCategories(children);
                })
                .toList();

        var start = page * size;
        var end = Math.min(start + size, parentCategories.size());

        if (start > parentCategories.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), parentCategories.size());
        }

        var pageContent = parentCategories.subList(start, end);
        return new PageImpl<>(pageContent, PageRequest.of(page, size), parentCategories.size());
    }

    public List<CategorySelectDTO> findAllSelectByAccount(Long accountId) {
        log.info("Fetching select category list for account {}", accountId);
        return repository.findAllByAccountId(accountId).stream()
                .map(category -> new CategorySelectDTO(category.getId(), category.getName()))
                .collect(Collectors.toList());
    }

    private Category validateAndResolveParentCategory(Long parentId, Long accountId) {
        return validateAndResolveParentCategory(parentId, accountId, null);
    }

    private Category validateAndResolveParentCategory(Long parentId, Long accountId, Long currentCategoryId) {
        if (parentId.equals(currentCategoryId)) {
            throw new IllegalArgumentException(translationService.translateMessage("category.selfParent"));
        }

        var parentCategory = repository.findByIdAndAccountId(parentId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("category.parentNotFound", parentId));

        if (parentCategory.getParentCategory() != null) {
            throw new IllegalArgumentException(
                    "Category '" + parentCategory.getName() + "' is already a child category. " +
                    "Only two levels of hierarchy are allowed (parent and child)."
            );
        }

        return parentCategory;
    }
}