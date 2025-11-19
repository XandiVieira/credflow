package com.relyon.credflow.controller;

import com.relyon.credflow.model.report.CategoryReportDTO;
import com.relyon.credflow.model.report.CreditCardReportDTO;
import com.relyon.credflow.model.report.MonthComparisonDTO;
import com.relyon.credflow.model.report.UserReportDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.ReportService;
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
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Expense reports and analysis endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/category")
    @Operation(summary = "Get category expense report", description = "Returns expense breakdown by category with hierarchy rollup")
    @ApiResponse(responseCode = "200", description = "Category report retrieved successfully")
    public ResponseEntity<CategoryReportDTO> getCategoryReport(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /reports/category for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var report = reportService.getCategoryReport(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/user")
    @Operation(summary = "Get user expense report", description = "Returns expense breakdown by responsible user")
    @ApiResponse(responseCode = "200", description = "User report retrieved successfully")
    public ResponseEntity<UserReportDTO> getUserReport(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /reports/user for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var report = reportService.getUserReport(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/credit-card")
    @Operation(summary = "Get credit card expense report", description = "Returns expense breakdown by credit card")
    @ApiResponse(responseCode = "200", description = "Credit card report retrieved successfully")
    public ResponseEntity<CreditCardReportDTO> getCreditCardReport(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /reports/credit-card for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var report = reportService.getCreditCardReport(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/month-comparison")
    @Operation(summary = "Get month-over-month comparison", description = "Returns monthly income/expense comparison with averages and trends")
    @ApiResponse(responseCode = "200", description = "Month comparison retrieved successfully")
    public ResponseEntity<MonthComparisonDTO> getMonthComparison(
            @Parameter(description = "Start date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for analysis")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /reports/month-comparison for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var report = reportService.getMonthComparison(user.getAccountId(), startDate, endDate);
        return ResponseEntity.ok(report);
    }
}
