package com.relyon.credflow.controller;

import com.relyon.credflow.model.report.CategoryReportDTO;
import com.relyon.credflow.model.report.CreditCardReportDTO;
import com.relyon.credflow.model.report.MonthComparisonDTO;
import com.relyon.credflow.model.report.UserReportDTO;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Expense reports and analysis endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/category")
    @Operation(summary = "Get category expense report", description = "Returns expense breakdown by category with hierarchy rollup. Supports optional filtering by users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Category report retrieved successfully")
    public ResponseEntity<CategoryReportDTO> getCategoryReport(
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

        log.info("GET /reports/category for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var report = reportService.getCategoryReport(filter);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/user")
    @Operation(summary = "Get user expense report", description = "Returns expense breakdown by responsible user. Supports optional filtering by categories, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "User report retrieved successfully")
    public ResponseEntity<UserReportDTO> getUserReport(
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

        log.info("GET /reports/user for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var report = reportService.getUserReport(filter);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/credit-card")
    @Operation(summary = "Get credit card expense report", description = "Returns expense breakdown by credit card. Supports optional filtering by categories, users, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Credit card report retrieved successfully")
    public ResponseEntity<CreditCardReportDTO> getCreditCardReport(
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

        log.info("GET /reports/credit-card for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var report = reportService.getCreditCardReport(filter);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/month-comparison")
    @Operation(summary = "Get month-over-month comparison", description = "Returns monthly income/expense comparison with averages and trends. Supports optional filtering by categories, users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "Month comparison retrieved successfully")
    public ResponseEntity<MonthComparisonDTO> getMonthComparison(
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

        log.info("GET /reports/month-comparison for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var report = reportService.getMonthComparison(filter);
        return ResponseEntity.ok(report);
    }
}
