package com.relyon.credflow.controller;

import com.relyon.credflow.model.dashboard.CategoryDistributionDTO;
import com.relyon.credflow.model.dashboard.DashboardSummaryDTO;
import com.relyon.credflow.model.dashboard.TimeSeriesDataDTO;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard summary and analytics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns comprehensive dashboard with income, expenses, balance trends, top categories, and upcoming bills")
    @ApiResponse(responseCode = "200", description = "Dashboard summary retrieved successfully")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/summary for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var summary = dashboardService.getDashboardSummary(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/visualization/expense-trend")
    @Operation(summary = "Get expense trend time series", description = "Returns time series data for expense trends over the specified period")
    @ApiResponse(responseCode = "200", description = "Expense trend data retrieved successfully")
    public ResponseEntity<TimeSeriesDataDTO> getExpenseTrend(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/visualization/expense-trend for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var trend = dashboardService.getExpenseTrend(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/visualization/category-distribution")
    @Operation(summary = "Get category distribution", description = "Returns category distribution data for pie/donut charts")
    @ApiResponse(responseCode = "200", description = "Category distribution retrieved successfully")
    public ResponseEntity<CategoryDistributionDTO> getCategoryDistribution(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /dashboard/visualization/category-distribution for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var distribution = dashboardService.getCategoryDistribution(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(distribution);
    }
}
