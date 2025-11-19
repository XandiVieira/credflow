package com.relyon.credflow.controller;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/export")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Export", description = "Transaction export endpoints in various formats")
public class ExportController {

    private final ExportService exportService;
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @GetMapping("/csv")
    @Operation(summary = "Export transactions to CSV", description = "Returns transactions in CSV format for the specified date range")
    @ApiResponse(responseCode = "200", description = "CSV file generated successfully")
    public ResponseEntity<byte[]> exportCsv(
            @Parameter(description = "Start date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /export/csv for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var csv = exportService.exportToCsv(user.getAccountId(), startDate, endDate);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/pdf")
    @Operation(summary = "Export transactions to PDF", description = "Returns transactions in PDF format for the specified date range")
    @ApiResponse(responseCode = "200", description = "PDF file generated successfully")
    public ResponseEntity<byte[]> exportPdf(
            @Parameter(description = "Start date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        log.info("GET /export/pdf for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var pdf = exportService.exportToPdf(user.getAccountId(), startDate, endDate);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    @Operation(summary = "Export transactions to Excel", description = "Returns transactions in Excel format for the specified date range")
    @ApiResponse(responseCode = "200", description = "Excel file generated successfully")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "Start date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for export")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal AuthenticatedUser user) throws IOException {

        log.info("GET /export/excel for account {} from {} to {}", user.getAccountId(), startDate, endDate);
        var excel = exportService.exportToExcel(user.getAccountId(), startDate, endDate);

        var filename = "transactions_" + startDate.format(FILE_DATE_FORMATTER) + "_" +
                endDate.format(FILE_DATE_FORMATTER) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
