package com.relyon.credflow.controller;

import com.relyon.credflow.model.budget.BudgetPreferencesRequestDTO;
import com.relyon.credflow.model.budget.BudgetPreferencesResponseDTO;
import com.relyon.credflow.model.mapper.BudgetMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.BudgetPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/budget-preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budget Preferences", description = "Budget preferences management endpoints")
public class BudgetPreferencesController {

    private final BudgetPreferencesService budgetPreferencesService;
    private final BudgetMapper budgetMapper;

    @PostMapping
    @Operation(summary = "Create or update budget preferences",
            description = "Creates or updates budget preferences for account or user level")
    @ApiResponse(responseCode = "200", description = "Preferences saved successfully")
    public ResponseEntity<BudgetPreferencesResponseDTO> createOrUpdate(
            @Parameter(description = "Budget preferences data", required = true)
            @Valid @RequestBody BudgetPreferencesRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /budget-preferences for account {}", user.getAccountId());
        var preferences = budgetMapper.toEntity(dto);
        var saved = budgetPreferencesService.createOrUpdate(preferences, user.getAccountId());
        return ResponseEntity.ok(budgetMapper.toDto(saved));
    }

    @GetMapping
    @Operation(summary = "Get budget preferences",
            description = "Retrieves budget preferences for account or specific user (falls back to account-level if user-level not found)")
    @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully")
    public ResponseEntity<BudgetPreferencesResponseDTO> getPreferences(
            @Parameter(description = "User ID (optional, null for account-level)")
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /budget-preferences?userId={} for account {}", userId, user.getAccountId());
        var preferences = budgetPreferencesService.getPreferencesForAccount(user.getAccountId(), userId);
        return ResponseEntity.ok(preferences);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget preferences", description = "Deletes budget preferences")
    @ApiResponse(responseCode = "204", description = "Preferences deleted successfully")
    @ApiResponse(responseCode = "404", description = "Preferences not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Preferences ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /budget-preferences/{} for account {}", id, user.getAccountId());
        budgetPreferencesService.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
