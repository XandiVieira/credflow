package com.relyon.credflow.controller;

import com.relyon.credflow.model.budget.BudgetTrackingDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.BudgetTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/v1/budget-tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budget Tracking", description = "Budget tracking and projection endpoints")
public class BudgetTrackingController {

    private final BudgetTrackingService budgetTrackingService;

    @GetMapping
    @Operation(summary = "Track all budgets", description = "Returns tracking data for all budgets in a period with projections and warnings")
    @ApiResponse(responseCode = "200", description = "Budget tracking data retrieved successfully")
    public ResponseEntity<List<BudgetTrackingDTO>> trackBudgets(
            @Parameter(description = "Period in YYYY-MM format", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /budget-tracking?period={} for account {}", period, user.getAccountId());
        var tracking = budgetTrackingService.trackBudgets(period, user.getAccountId());
        return ResponseEntity.ok(tracking);
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "Track specific budget", description = "Returns tracking data for a specific budget with projections and warnings")
    @ApiResponse(responseCode = "200", description = "Budget tracking data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Budget not found")
    public ResponseEntity<BudgetTrackingDTO> trackBudget(
            @Parameter(description = "Budget ID", required = true)
            @PathVariable Long budgetId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /budget-tracking/{} for account {}", budgetId, user.getAccountId());
        var tracking = budgetTrackingService.trackBudget(budgetId, user.getAccountId());
        return ResponseEntity.ok(tracking);
    }

    @PostMapping("/rollover")
    @Operation(summary = "Process budget rollover", description = "Processes rollover of unused budget amounts to the next period")
    @ApiResponse(responseCode = "200", description = "Rollover processed successfully")
    public ResponseEntity<Void> processRollover(
            @Parameter(description = "Period to rollover from in YYYY-MM format", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth fromPeriod,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /budget-tracking/rollover?fromPeriod={} for account {}", fromPeriod, user.getAccountId());
        budgetTrackingService.processRollover(fromPeriod, user.getAccountId());
        return ResponseEntity.ok().build();
    }
}
