package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.csv.CsvImportFormat;
import com.relyon.credflow.model.csv.CsvImportHistory;
import com.relyon.credflow.model.csv.CsvImportStatus;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.CsvImportHistoryRepository;
import com.relyon.credflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

    private final CsvImportHistoryRepository csvImportHistoryRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final LocalizedMessageTranslationService translationService;

    @Transactional
    public CsvImportHistory importCsv(MultipartFile file, Long accountId, CsvImportFormat format) {
        log.info("Starting CSV import: file={}, format={}, account={}",
                file.getOriginalFilename(), format, accountId);

        var history = CsvImportHistory.builder()
                .account(com.relyon.credflow.model.account.Account.builder().id(accountId).build())
                .fileName(file.getOriginalFilename())
                .format(format)
                .status(CsvImportStatus.SUCCESS)
                .totalRows(0)
                .importedRows(0)
                .skippedRows(0)
                .build();

        try {
            List<Transaction> imported;
            if (format == CsvImportFormat.BANRISUL) {
                imported = transactionService.importFromBanrisulCSV(file, accountId);
            } else {
                throw new IllegalArgumentException("Format not yet implemented: " + format);
            }

            history = csvImportHistoryRepository.save(history);

            var finalHistoryId = history.getId();
            imported.forEach(t -> {
                t.setCsvImportHistory(CsvImportHistory.builder().id(finalHistoryId).build());
                transactionRepository.save(t);
            });

            history.setImportedRows(imported.size());
            history.setTotalRows(imported.size());
            history.setStatus(CsvImportStatus.SUCCESS);

            log.info("CSV import completed successfully. {} transactions imported", imported.size());
            return csvImportHistoryRepository.save(history);

        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage(), e);
            history.setStatus(CsvImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            return csvImportHistoryRepository.save(history);
        }
    }

    @Transactional
    public void rollbackImport(Long importHistoryId, Long accountId) {
        log.info("Rolling back CSV import ID: {}", importHistoryId);

        var history = csvImportHistoryRepository.findById(importHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException("csv.import.notFound", importHistoryId));

        if (!history.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException(translationService.translateMessage("csv.import.accountMismatch"));
        }

        if (history.getStatus() == CsvImportStatus.ROLLED_BACK) {
            log.warn("Import {} already rolled back", importHistoryId);
            return;
        }

        var transactions = transactionRepository.findByCsvImportHistoryId(importHistoryId);
        log.info("Found {} transactions to delete for import {}", transactions.size(), importHistoryId);

        transactions.forEach(t -> transactionRepository.deleteById(t.getId()));

        history.setStatus(CsvImportStatus.ROLLED_BACK);
        csvImportHistoryRepository.save(history);

        log.info("Successfully rolled back import ID: {}. Deleted {} transactions",
                importHistoryId, transactions.size());
    }

    public List<CsvImportHistory> getImportHistory(Long accountId) {
        log.info("Fetching import history for account {}", accountId);
        return csvImportHistoryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public CsvImportHistory getImportById(Long id, Long accountId) {
        log.info("Fetching import {} for account {}", id, accountId);
        var history = csvImportHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("csv.import.notFound", id));

        if (!history.getAccount().getId().equals(accountId)) {
            throw new IllegalArgumentException(translationService.translateMessage("csv.import.accountMismatch"));
        }

        return history;
    }
}
