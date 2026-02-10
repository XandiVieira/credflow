package com.relyon.credflow.controller;

import com.relyon.credflow.model.csv.CsvImportFormat;
import com.relyon.credflow.model.csv.CsvImportHistory;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/file-imports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Imports", description = "File import management and history endpoints (CSV, PDF)")
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping
    @Operation(
            summary = "Import files",
            description = "Imports transactions from one or more files. Supported formats: BANRISUL (CSV conta corrente), BANRISUL_CREDIT_CARD_CSV (CSV cartão de crédito - valores positivos são negados), BANRISUL_CREDIT_CARD_PDF (PDF)"
    )
    @ApiResponse(responseCode = "200", description = "Import completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or format")
    public ResponseEntity<List<CsvImportHistory>> importFiles(
            @Parameter(description = "Files to import (CSV or PDF)", required = true)
            @RequestParam("file") List<MultipartFile> files,
            @Parameter(description = "Import format: BANRISUL, BANRISUL_CREDIT_CARD_CSV, BANRISUL_CREDIT_CARD_PDF, GENERIC", required = true)
            @RequestParam("format") CsvImportFormat format,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /file-imports: {} file(s), format={}, account={}",
                files.size(), format, user.getAccountId());

        var results = new ArrayList<CsvImportHistory>();
        for (var file : files) {
            log.info("Processing file: {}", file.getOriginalFilename());
            var result = csvImportService.importCsv(file, user.getAccountId(), format);
            results.add(result);
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "Get import history", description = "Retrieves all import history for the authenticated account")
    @ApiResponse(responseCode = "200", description = "Import history retrieved successfully")
    public ResponseEntity<List<CsvImportHistory>> getImportHistory(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /file-imports for account {}", user.getAccountId());
        var history = csvImportService.getImportHistory(user.getAccountId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get import by ID", description = "Retrieves a specific import by ID")
    @ApiResponse(responseCode = "200", description = "Import found")
    @ApiResponse(responseCode = "404", description = "Import not found")
    public ResponseEntity<CsvImportHistory> getImportById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /file-imports/{} for account {}", id, user.getAccountId());
        var history = csvImportService.getImportById(id, user.getAccountId());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}/rollback")
    @Operation(summary = "Rollback import", description = "Rolls back an import by deleting all imported transactions")
    @ApiResponse(responseCode = "204", description = "Import successfully rolled back")
    @ApiResponse(responseCode = "404", description = "Import not found")
    public ResponseEntity<Void> rollbackImport(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /file-imports/{}/rollback for account {}", id, user.getAccountId());
        csvImportService.rollbackImport(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
