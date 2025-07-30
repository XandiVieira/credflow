package com.relyon.credflow.controller;

import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.AccountService;
import com.relyon.credflow.service.DescriptionMappingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/description-mappings")
@RequiredArgsConstructor
@Slf4j
public class DescriptionMappingController {

    private final DescriptionMappingService service;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> create(
            @AuthenticationPrincipal AuthenticatedUser authenticated,
            @RequestParam Long accountId,
            @Valid @RequestBody List<DescriptionMappingRequestDTO> requestDTOs
    ) {
        log.info("Received POST to create {} description mappings by user {} for account {}", requestDTOs.size(), authenticated.getUser().getId(), accountId);

        var account = accountService.findById(accountId);

        List<DescriptionMapping> entities = requestDTOs.stream()
                .map(dto -> {
                    var mapping = modelMapper.map(dto, DescriptionMapping.class);
                    mapping.setAccount(account);
                    return mapping;
                }).toList();

        var created = service.createAll(entities, authenticated);

        var response = created.stream()
                .map(mapping -> modelMapper.map(mapping, DescriptionMappingResponseDTO.class)).toList();

        log.info("Successfully created {} description mappings", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DescriptionMappingResponseDTO>> findAll() {
        log.info("Received GET to fetch all description mappings");
        var result = service.findAll().stream()
                .map(mapping -> modelMapper.map(mapping, DescriptionMappingResponseDTO.class)).toList();
        log.info("Returning {} description mappings", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> findById(@PathVariable Long id) {
        log.info("Received GET to fetch description mapping by ID {}", id);
        return service.findById(id)
                .map(mapping -> {
                    log.info("Found description mapping for ID {}", id);
                    return ResponseEntity.ok(modelMapper.map(mapping, DescriptionMappingResponseDTO.class));
                })
                .orElseGet(() -> {
                    log.warn("Description mapping not found for ID {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/by-description")
    public ResponseEntity<DescriptionMappingResponseDTO> findByOriginalDescription(@RequestParam String originalDescription) {
        log.info("Received GET to fetch description mapping by original description '{}'", originalDescription);
        return service.findByOriginalDescription(originalDescription)
                .map(mapping -> {
                    log.info("Found description mapping for '{}'", originalDescription);
                    return ResponseEntity.ok(modelMapper.map(mapping, DescriptionMappingResponseDTO.class));
                })
                .orElseGet(() -> {
                    log.warn("Description mapping not found for '{}'", originalDescription);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<DescriptionMappingResponseDTO> update(
            @AuthenticationPrincipal AuthenticatedUser authenticated,
            @PathVariable Long id,
            @RequestParam Long accountId,
            @Valid @RequestBody DescriptionMappingRequestDTO dto
    ) {
        log.info("Received PUT to update description mapping ID {} by user {} for account {}", id, authenticated.getUser().getId(), accountId);

        var updated = modelMapper.map(dto, DescriptionMapping.class);
        updated.setAccount(accountService.findById(accountId));

        var saved = service.update(id, updated, authenticated);

        log.info("Successfully updated description mapping ID {}", id);
        return ResponseEntity.ok(modelMapper.map(saved, DescriptionMappingResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Received DELETE for description mapping ID {}", id);
        service.delete(id);
        log.info("Successfully deleted description mapping ID {}", id);
        return ResponseEntity.noContent().build();
    }
}