package com.relyon.credflow.controller;

import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.AdvancedExcelExportService;
import com.relyon.credflow.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/export")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export", description = "Transaction export endpoints in various formats")
public class ExportController {

    private final ExportService exportService;
    private final AdvancedExcelExportService advancedExcelExportService;
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GetMapping("/csv")
    @Operation(summary = "Export transactions to CSV", description = "Returns transactions in CSV format for the specified date range. Supports optional filtering by categories, users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "Start date for export (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export (required)")
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

        log.info("GET /export/csv for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var csv = exportService.exportToCsv(filter);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/pdf")
    @Operation(summary = "Export transactions to PDF", description = "Returns transactions in PDF format for the specified date range. Supports optional filtering by categories, users, credit cards, transaction types, and more.")
    @ApiResponse(responseCode = "200", description = "PDF file generated successfully")
    public ResponseEntity<byte[]> exportPdf(
            @Parameter(description = "Start date for export (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export (required)")
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
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        log.info("GET /export/pdf for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var pdf = exportService.exportToPdf(filter);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    @Operation(
            summary = "Export transactions to Excel (Advanced)",
            description = """
                    Generates a comprehensive Excel report with multiple sheets:
                    
                    **Sheets included:**
                    - **Dashboard**: KPIs, top categories summary, and pie chart
                    - **Transações**: Full transaction list with auto-filters
                    - **Por Categoria**: Summary by category with bar chart
                    - **Por Mês**: Monthly summary with line chart showing trends
                    - **Por Cartão**: Summary by credit card
                    - **Por Responsável**: Summary by responsible user
                    - **Tendência Diária**: Daily trend with accumulated balance chart
                    
                    **Features:**
                    - Auto-filters on all data tables
                    - Dynamic charts (pie, bar, line)
                    - Color-coded values (green for income, red for expenses)
                    - Currency formatting (R$)
                    - Percentage calculations
                    - Accumulated balance tracking
                    
                    **Dates are optional** - if not provided, exports all transactions.
                    
                    Supports filtering by categories, users, credit cards, transaction types, sources, and amount range.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Excel file generated successfully")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "Start date for export (optional - if not provided, exports from the beginning)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export (optional - if not provided, exports until today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
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
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        var effectiveStartDate = startDate != null ? startDate : LocalDate.of(1900, 1, 1);
        var effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        log.info("GET /export/excel (advanced) for account {} from {} to {}", user.getAccountId(), effectiveStartDate, effectiveEndDate);

        var filter = new TransactionFilter(
                user.getAccountId(),
                effectiveStartDate,
                effectiveEndDate,
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

        var excel = advancedExcelExportService.exportToExcel(filter);

        var filename = startDate == null && endDate == null
                ? "credflow_report_completo.xlsx"
                : "credflow_report_" + effectiveStartDate.format(FILE_DATE_FORMATTER) + "_" +
                effectiveEndDate.format(FILE_DATE_FORMATTER) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/excel/simple")
    @Operation(summary = "Export transactions to Excel (Simple)", description = "Returns a simple Excel file with transactions and basic summary. Use /excel for the full report with charts.")
    @ApiResponse(responseCode = "200", description = "Excel file generated successfully")
    public ResponseEntity<byte[]> exportExcelSimple(
            @Parameter(description = "Start date for export (required)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export (required)")
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
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        log.info("GET /export/excel/simple for account {} from {} to {}", user.getAccountId(), startDate, endDate);

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

        var excel = exportService.exportToExcel(filter);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
