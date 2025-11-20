package com.relyon.credflow.controller;

import com.relyon.credflow.model.dashboard.CategoryDistributionDTO;
import com.relyon.credflow.model.dashboard.DashboardSummaryDTO;
import com.relyon.credflow.model.dashboard.TimeSeriesDataDTO;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard summary and analytics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns comprehensive dashboard with income, expenses, balance trends, top categories, and upcoming bills. Supports optional filtering by categories, users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(
            @Parameter(description = "Start date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by category IDs (optional)")
            @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Filter by responsible user IDs (optional)")
            @RequestParam(required = false) List<Long> responsibleUserIds,
            @Parameter(description = "Filter by credit card IDs (optional)")
            @RequestParam(required = false) List<Long> creditCardIds,
            @Parameter(description = "Filter by transaction types (optional)")
            @RequestParam(required = false) List<TransactionType> transactionTypes,
            @Parameter(description = "Filter by transaction sources (optional)")
            @RequestParam(required = false) List<TransactionSource> transactionSources,
            @Parameter(description = "Minimum amount (optional)")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum amount (optional)")
            @RequestParam(required = false) BigDecimal maxAmount,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/summary for account {} from {} to {}", user.getAccountId(), startDate, endDate);

        var filter = new TransactionFilter(
                user.getAccountId(),
                startDate,
                endDate,
                null,
                null,
                minAmount,
                maxAmount,
                responsibleUserIds,
                categoryIds,
                creditCardIds,
                transactionTypes,
                transactionSources,
                false
        );

        var summary = dashboardService.getDashboardSummary(filter);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/visualization/expense-trend")
    @Operation(summary = "Get expense trend time series", description = "Returns time series data for expense trends over the specified period. Supports optional filtering by categories, users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Expense trend data retrieved successfully")
    public ResponseEntity<TimeSeriesDataDTO> getExpenseTrend(
            @Parameter(description = "Start date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by category IDs (optional)")
            @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Filter by responsible user IDs (optional)")
            @RequestParam(required = false) List<Long> responsibleUserIds,
            @Parameter(description = "Filter by credit card IDs (optional)")
            @RequestParam(required = false) List<Long> creditCardIds,
            @Parameter(description = "Filter by transaction types (optional)")
            @RequestParam(required = false) List<TransactionType> transactionTypes,
            @Parameter(description = "Filter by transaction sources (optional)")
            @RequestParam(required = false) List<TransactionSource> transactionSources,
            @Parameter(description = "Minimum amount (optional)")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum amount (optional)")
            @RequestParam(required = false) BigDecimal maxAmount,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/visualization/expense-trend for account {} from {} to {}", user.getAccountId(), startDate, endDate);

        var filter = new TransactionFilter(
                user.getAccountId(),
                startDate,
                endDate,
                null,
                null,
                minAmount,
                maxAmount,
                responsibleUserIds,
                categoryIds,
                creditCardIds,
                transactionTypes,
                transactionSources,
                false
        );

        var trend = dashboardService.getExpenseTrend(filter);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/visualization/category-distribution")
    @Operation(summary = "Get category distribution", description = "Returns category distribution data for pie/donut charts. Supports optional filtering by users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Category distribution retrieved successfully")
    public ResponseEntity<CategoryDistributionDTO> getCategoryDistribution(
            @Parameter(description = "Start date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Filter by category IDs (optional)")
            @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "Filter by responsible user IDs (optional)")
            @RequestParam(required = false) List<Long> responsibleUserIds,
            @Parameter(description = "Filter by credit card IDs (optional)")
            @RequestParam(required = false) List<Long> creditCardIds,
            @Parameter(description = "Filter by transaction types (optional)")
            @RequestParam(required = false) List<TransactionType> transactionTypes,
            @Parameter(description = "Filter by transaction sources (optional)")
            @RequestParam(required = false) List<TransactionSource> transactionSources,
            @Parameter(description = "Minimum amount (optional)")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum amount (optional)")
            @RequestParam(required = false) BigDecimal maxAmount,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/visualization/category-distribution for account {} from {} to {}", user.getAccountId(), startDate, endDate);

        var filter = new TransactionFilter(
                user.getAccountId(),
                startDate,
                endDate,
                null,
                null,
                minAmount,
                maxAmount,
                responsibleUserIds,
                categoryIds,
                creditCardIds,
                transactionTypes,
                transactionSources,
                false
        );

        var distribution = dashboardService.getCategoryDistribution(filter);
        return ResponseEntity.ok(distribution);
    }
}
