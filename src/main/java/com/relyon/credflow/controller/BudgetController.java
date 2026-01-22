package com.relyon.credflow.controller;

import com.relyon.credflow.model.budget.BudgetRequestDTO;
import com.relyon.credflow.model.budget.BudgetResponseDTO;
import com.relyon.credflow.model.mapper.BudgetMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budgets", description = "Budget management endpoints")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;

    @PostMapping
    @Operation(summary = "Create budget", description = "Creates a new budget for account, category, or user")
    @ApiResponse(responseCode = "200", description = "Budget created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid budget data or budget already exists")
    public ResponseEntity<BudgetResponseDTO> create(
            @Parameter(description = "Budget data", required = true)
            @Valid @RequestBody BudgetRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /budgets for account {}", user.getAccountId());
        var budget = budgetMapper.toEntity(dto);
        var created = budgetService.create(budget, user.getAccountId());
        return ResponseEntity.ok(budgetMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget", description = "Updates an existing budget")
    @ApiResponse(responseCode = "200", description = "Budget updated successfully")
    @ApiResponse(responseCode = "404", description = "Budget not found")
    public ResponseEntity<BudgetResponseDTO> update(
            @Parameter(description = "Budget ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated budget data", required = true)
            @Valid @RequestBody BudgetRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("PUT /budgets/{} for account {}", id, user.getAccountId());
        var budget = budgetMapper.toEntity(dto);
        var updated = budgetService.update(id, budget, user.getAccountId());
        return ResponseEntity.ok(budgetMapper.toDto(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID", description = "Retrieves a specific budget")
    @ApiResponse(responseCode = "200", description = "Budget retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Budget not found")
    public ResponseEntity<BudgetResponseDTO> findById(
            @Parameter(description = "Budget ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /budgets/{} for account {}", id, user.getAccountId());
        var budget = budgetService.findById(id, user.getAccountId());
        return ResponseEntity.ok(budgetMapper.toDto(budget));
    }

    @GetMapping
    @Operation(summary = "Get budgets by period", description = "Retrieves all budgets for a specific period")
    @ApiResponse(responseCode = "200", description = "Budgets retrieved successfully")
    public ResponseEntity<List<BudgetResponseDTO>> findByPeriod(
            @Parameter(description = "Period in YYYY-MM format", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /budgets?period={} for account {}", period, user.getAccountId());
        var budgets = budgetService.findByPeriod(period, user.getAccountId());
        var response = budgets.stream().map(budgetMapper::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete budget", description = "Deletes a budget")
    @ApiResponse(responseCode = "204", description = "Budget deleted successfully")
    @ApiResponse(responseCode = "404", description = "Budget not found")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Budget ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /budgets/{} for account {}", id, user.getAccountId());
        budgetService.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
