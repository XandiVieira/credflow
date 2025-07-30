package com.relyon.credflow.controller;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryRequestDTO;
import com.relyon.credflow.model.category.CategoryResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;
    private final ModelMapper modelMapper;

    @GetMapping
    public List<CategoryResponseDTO> getAll() {
        return service.findAll().stream()
                .map(category -> modelMapper.map(category, CategoryResponseDTO.class)).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(category -> modelMapper.map(category, CategoryResponseDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @Valid @RequestBody CategoryRequestDTO request) {
        var category = modelMapper.map(request, Category.class);
        var created = service.create(category, authenticatedUser);
        return ResponseEntity.ok(modelMapper.map(created, CategoryResponseDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        var updatedEntity = modelMapper.map(request, Category.class);
        var updated = service.update(id, updatedEntity);
        return ResponseEntity.ok(modelMapper.map(updated, CategoryResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}