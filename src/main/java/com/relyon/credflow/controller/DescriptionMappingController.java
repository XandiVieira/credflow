package com.relyon.credflow.controller;

import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.DescriptionMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                                      @Valid @RequestBody List<DescriptionMappingRequestDTO> requestDTOs
    ) {
        log.info("POST to create {} for account {}", requestDTOs.size(), user.getAccountId());

        var mappings = requestDTOs.stream()
                .map(dto -> modelMapper.map(dto, DescriptionMapping.class)).toList();

        var created = service.createAll(mappings, user.getAccountId());

        var response = created.stream()
                .map(mapping -> modelMapper.map(mapping, DescriptionMappingResponseDTO.class))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> findAll(@AuthenticationPrincipal AuthenticatedUser user, @RequestParam(required = false) Boolean onlyIncomplete) {
        log.info("GET to fetch all description mappings for account {}", user.getAccountId());
        var result = service.findAll(user.getAccountId(), onlyIncomplete).stream()
                .map(mapping -> modelMapper.map(mapping, DescriptionMappingResponseDTO.class))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("GET to fetch description mapping by ID {} for account {}", id, user.getAccountId());
        return service.findById(id, user.getAccountId())
                .map(mapping -> ResponseEntity.ok(modelMapper.map(mapping, DescriptionMappingResponseDTO.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-description")
    public ResponseEntity<DescriptionMappingResponseDTO> findByOriginalDescription(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String originalDescription
    ) {
        log.info("GET to fetch description mapping by original description '{}' for account {}",
                originalDescription, user.getAccountId());
        return service.findByNormalizedDescription(originalDescription, user.getAccountId())
                .map(mapping -> ResponseEntity.ok(modelMapper.map(mapping, DescriptionMappingResponseDTO.class)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody DescriptionMappingRequestDTO dto
    ) {
        log.info("PUT to update mapping ID {} for account {}", id, user.getAccountId());

        var updated = modelMapper.map(dto, DescriptionMapping.class);

        var saved = service.update(id, updated, user.getAccountId());
        return ResponseEntity.ok(modelMapper.map(saved, DescriptionMappingResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("DELETE for description mapping ID {} and account {}", id, user.getAccountId());
        service.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}