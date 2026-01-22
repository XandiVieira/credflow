package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.csv.CsvImportHistory;
import com.relyon.credflow.model.csv.CsvImportStatus;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.CsvImportHistoryRepository;
import com.relyon.credflow.repository.TransactionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private CsvImportHistoryRepository csvImportHistoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private CsvImportService csvImportService;

    @Test
    void rollbackImport_whenImportExists_shouldDeleteTransactionsAndUpdateStatus() {
        var importHistoryId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();

        var history = CsvImportHistory.builder()
                .id(importHistoryId)
                .account(account)
                .status(CsvImportStatus.SUCCESS)
                .build();

        var transaction1 = Transaction.builder().id(1L).build();
        var transaction2 = Transaction.builder().id(2L).build();
        var transactions = List.of(transaction1, transaction2);

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.of(history));
        when(transactionRepository.findByCsvImportHistoryId(importHistoryId)).thenReturn(transactions);

        csvImportService.rollbackImport(importHistoryId, accountId);

        verify(transactionRepository).deleteById(1L);
        verify(transactionRepository).deleteById(2L);
        assertThat(history.getStatus()).isEqualTo(CsvImportStatus.ROLLED_BACK);
        verify(csvImportHistoryRepository).save(history);
    }

    @Test
    void rollbackImport_whenImportNotFound_shouldThrowException() {
        var importHistoryId = 999L;
        var accountId = 10L;

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> csvImportService.rollbackImport(importHistoryId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(csvImportHistoryRepository).findById(importHistoryId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rollbackImport_whenAccountMismatch_shouldThrowException() {
        var importHistoryId = 1L;
        var accountId = 10L;
        var differentAccountId = 20L;

        var account = Account.builder().id(differentAccountId).build();
        var history = CsvImportHistory.builder()
                .id(importHistoryId)
                .account(account)
                .status(CsvImportStatus.SUCCESS)
                .build();

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.of(history));
        when(translationService.translateMessage("file.import.accountMismatch"))
                .thenReturn("CSV import does not belong to this account");

        assertThatThrownBy(() -> csvImportService.rollbackImport(importHistoryId, accountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV import does not belong to this account");

        verify(csvImportHistoryRepository).findById(importHistoryId);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rollbackImport_whenAlreadyRolledBack_shouldReturnWithoutChanges() {
        var importHistoryId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();

        var history = CsvImportHistory.builder()
                .id(importHistoryId)
                .account(account)
                .status(CsvImportStatus.ROLLED_BACK)
                .build();

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.of(history));

        csvImportService.rollbackImport(importHistoryId, accountId);

        verify(csvImportHistoryRepository).findById(importHistoryId);
        verifyNoMoreInteractions(csvImportHistoryRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void getImportHistory_whenHistoryExists_shouldReturnList() {
        var accountId = 10L;
        var history1 = CsvImportHistory.builder().id(1L).build();
        var history2 = CsvImportHistory.builder().id(2L).build();
        var historyList = List.of(history1, history2);

        when(csvImportHistoryRepository.findByAccountIdOrderByCreatedAtDesc(accountId))
                .thenReturn(historyList);

        var result = csvImportService.getImportHistory(accountId);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(history1, history2);
        verify(csvImportHistoryRepository).findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Test
    void getImportById_whenImportExists_shouldReturnHistory() {
        var importHistoryId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();
        var history = CsvImportHistory.builder()
                .id(importHistoryId)
                .account(account)
                .build();

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.of(history));

        var result = csvImportService.getImportById(importHistoryId, accountId);

        assertThat(result).isEqualTo(history);
        verify(csvImportHistoryRepository).findById(importHistoryId);
    }

    @Test
    void getImportById_whenImportNotFound_shouldThrowException() {
        var importHistoryId = 999L;
        var accountId = 10L;

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> csvImportService.getImportById(importHistoryId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(csvImportHistoryRepository).findById(importHistoryId);
    }

    @Test
    void getImportById_whenAccountMismatch_shouldThrowException() {
        var importHistoryId = 1L;
        var accountId = 10L;
        var differentAccountId = 20L;

        var account = Account.builder().id(differentAccountId).build();
        var history = CsvImportHistory.builder()
                .id(importHistoryId)
                .account(account)
                .build();

        when(csvImportHistoryRepository.findById(importHistoryId)).thenReturn(Optional.of(history));
        when(translationService.translateMessage("file.import.accountMismatch"))
                .thenReturn("CSV import does not belong to this account");

        assertThatThrownBy(() -> csvImportService.getImportById(importHistoryId, accountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV import does not belong to this account");

        verify(csvImportHistoryRepository).findById(importHistoryId);
    }
}
