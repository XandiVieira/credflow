package com.relyon.credflow.controller;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryRequestDTO;
import com.relyon.credflow.model.category.CategoryResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAll(@AuthenticationPrincipal AuthenticatedUser user) {
        log.info("GET all categories for account {}", user.getAccountId());

        var categories = service.findAll(user.getAccountId()).stream()
                .map(category -> modelMapper.map(category, CategoryResponseDTO.class))
                .toList();

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getById(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("GET category ID {} for account {}", id, user.getAccountId());

        return service.findById(id, user.getAccountId())
                .map(category -> ResponseEntity.ok(modelMapper.map(category, CategoryResponseDTO.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        log.info("POST create category '{}' for account {}", request.getName(), user.getAccountId());

        var category = modelMapper.map(request, Category.class);

        var created = service.create(category, user.getAccountId());
        return ResponseEntity.ok(modelMapper.map(created, CategoryResponseDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        log.info("PUT update category ID {} for account {}", id, user.getAccountId());

        var category = modelMapper.map(request, Category.class);
        var updated = service.update(id, category, user.getAccountId());
        return ResponseEntity.ok(modelMapper.map(updated, CategoryResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("DELETE category ID {} for account {}", id, user.getAccountId());

        service.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}