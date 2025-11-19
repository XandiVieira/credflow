package com.relyon.credflow.controller;

import com.relyon.credflow.model.csv.CsvImportFormat;
import com.relyon.credflow.model.csv.CsvImportHistory;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CsvImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/csv-imports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CSV Imports", description = "CSV import management and history endpoints")
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping
    @Operation(summary = "Import CSV file", description = "Imports transactions from a CSV file in specified format")
    @ApiResponse(responseCode = "200", description = "Import completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or format")
    public ResponseEntity<CsvImportHistory> importCsv(
            @Parameter(description = "CSV file to import", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "CSV format", required = true)
            @RequestParam("format") CsvImportFormat format,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /csv-imports: file={}, format={}, account={}",
                file.getOriginalFilename(), format, user.getAccountId());

        var result = csvImportService.importCsv(file, user.getAccountId(), format);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @Operation(summary = "Get import history", description = "Retrieves all CSV import history for the authenticated account")
    @ApiResponse(responseCode = "200", description = "Import history retrieved successfully")
    public ResponseEntity<List<CsvImportHistory>> getImportHistory(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /csv-imports for account {}", user.getAccountId());
        var history = csvImportService.getImportHistory(user.getAccountId());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get import by ID", description = "Retrieves a specific CSV import by ID")
    @ApiResponse(responseCode = "200", description = "Import found")
    @ApiResponse(responseCode = "404", description = "Import not found")
    public ResponseEntity<CsvImportHistory> getImportById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /csv-imports/{} for account {}", id, user.getAccountId());
        var history = csvImportService.getImportById(id, user.getAccountId());
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}/rollback")
    @Operation(summary = "Rollback import", description = "Rolls back a CSV import by deleting all imported transactions")
    @ApiResponse(responseCode = "204", description = "Import successfully rolled back")
    @ApiResponse(responseCode = "404", description = "Import not found")
    public ResponseEntity<Void> rollbackImport(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /csv-imports/{}/rollback for account {}", id, user.getAccountId());
        csvImportService.rollbackImport(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
