package com.relyon.credflow.controller;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryRequestDTO;
import com.relyon.credflow.model.category.CategoryResponseDTO;
import com.relyon.credflow.model.category.CategorySelectDTO;
import com.relyon.credflow.model.mapper.CategoryMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService service;
    private final CategoryMapper categoryMapper;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(
            @Valid @RequestBody CategoryRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST create category '{}' for account {}", dto.getName(), user.getAccountId());
        Category entity = categoryMapper.toEntity(dto);
        Category saved = service.create(entity, user.getAccountId());
        return ResponseEntity.ok(categoryMapper.toDto(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET category ID {} for account {}", id, user.getAccountId());
        var category = service.findById(id, user.getAccountId());
        return ResponseEntity.ok(categoryMapper.toDto(category));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<CategoryResponseDTO>> getAllByAccount(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET all categories for account {} (page={}, size={})", user.getAccountId(), page, size);
        var response = service.findAllByAccountHierarchical(user.getAccountId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/select")
    public ResponseEntity<List<CategorySelectDTO>> getAllSelect(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET select category list for account {}", user.getAccountId());
        var response = service.findAllSelectByAccount(user.getAccountId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CategoryRequestDTO dto) {

        log.info("PUT update category ID {} for account {}", id, user.getAccountId());
        Category patch = categoryMapper.toEntity(dto);
        Category updated = service.update(id, patch, user.getAccountId());
        return ResponseEntity.ok(categoryMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE category ID {} for account {}", id, user.getAccountId());
        service.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}