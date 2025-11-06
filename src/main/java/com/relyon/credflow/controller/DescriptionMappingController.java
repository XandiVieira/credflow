package com.relyon.credflow.controller;

import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingResponseDTO;
import com.relyon.credflow.model.mapper.DescriptionMappingMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.DescriptionMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/description-mappings")
@RequiredArgsConstructor
@Slf4j
public class DescriptionMappingController {

    private final DescriptionMappingService service;
    private final DescriptionMappingMapper descriptionMappingMapper;

    @PostMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody List<@Valid DescriptionMappingRequestDTO> requestDTOs
    ) {
        log.info("POST to create {} mappings for account {}", requestDTOs.size(), user.getAccountId());

        List<DescriptionMapping> entities = requestDTOs.stream()
                .map(descriptionMappingMapper::toEntity)
                .toList();

        var created = service.createAll(entities, user.getAccountId());

        var response = created.stream()
                .map(descriptionMappingMapper::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> findAll(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Boolean onlyIncomplete
    ) {
        log.info("GET all description mappings for account {} (onlyIncomplete={})",
                user.getAccountId(), onlyIncomplete);

        var result = service.findAll(user.getAccountId(), onlyIncomplete).stream()
                .map(descriptionMappingMapper::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET mapping by ID {} for account {}", id, user.getAccountId());

        return service.findById(id, user.getAccountId())
                .map(descriptionMappingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-description")
    public ResponseEntity<DescriptionMappingResponseDTO> findByOriginalDescription(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String originalDescription) {

        log.info("GET mapping by original description '{}' for account {}", originalDescription, user.getAccountId());

        return service.findByNormalizedDescription(originalDescription, user.getAccountId())
                .map(descriptionMappingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody DescriptionMappingRequestDTO dto
    ) {
        log.info("PUT update mapping ID {} for account {}", id, user.getAccountId());

        DescriptionMapping patch = descriptionMappingMapper.toEntity(dto);
        var saved = service.update(id, patch, user.getAccountId());

        return ResponseEntity.ok(descriptionMappingMapper.toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("DELETE mapping ID {} for account {}", id, user.getAccountId());
        service.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}