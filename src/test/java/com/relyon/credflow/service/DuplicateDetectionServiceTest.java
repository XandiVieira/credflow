package com.relyon.credflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DuplicateDetectionService service;

    @Test
    void findPotentialDuplicatesForManualEntry_whenNoCsvTransactionsExist_returnsEmptyList() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-100);

        var manualTransaction = createTransaction(1L, value, date, TransactionSource.MANUAL);

        when(transactionRepository.findPotentialDuplicates(eq(accountId), any(), any(), eq(value)))
                .thenReturn(List.of(manualTransaction));

        var result = service.findPotentialDuplicatesForManualEntry(accountId, date, value);

        assertTrue(result.isEmpty());
    }

    @Test
    void findPotentialDuplicatesForManualEntry_whenCsvTransactionExists_returnsDuplicates() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-100);

        var csvTransaction = createTransaction(1L, value, date, TransactionSource.CSV_IMPORT);
        csvTransaction.setDescription("SUPERMERCADO X LTDA");

        when(transactionRepository.findPotentialDuplicates(eq(accountId), any(), any(), eq(value)))
                .thenReturn(List.of(csvTransaction));

        var result = service.findPotentialDuplicatesForManualEntry(accountId, date, value);

        assertEquals(1, result.size());
        assertEquals(csvTransaction.getId(), result.get(0).getId());
        assertEquals(csvTransaction.getDescription(), result.get(0).getDescription());
        assertEquals(TransactionSource.CSV_IMPORT, result.get(0).getSource());
    }

    @Test
    void findPotentialDuplicatesForManualEntry_searchesWithin3DaysWindow() {
        var accountId = 1L;
        var date = LocalDate.of(2025, 1, 15);
        var value = BigDecimal.valueOf(-50);

        when(transactionRepository.findPotentialDuplicates(
                eq(accountId),
                eq(LocalDate.of(2025, 1, 12)),
                eq(LocalDate.of(2025, 1, 18)),
                eq(value)
        )).thenReturn(List.of());

        service.findPotentialDuplicatesForManualEntry(accountId, date, value);

        verify(transactionRepository).findPotentialDuplicates(
                eq(accountId),
                eq(LocalDate.of(2025, 1, 12)),
                eq(LocalDate.of(2025, 1, 18)),
                eq(value)
        );
    }

    @Test
    void findPotentialDuplicatesForManualEntry_includesCategoryAndCreditCard() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-200);

        var category = new Category();
        category.setId(10L);
        category.setName("Alimentação");

        var creditCard = new CreditCard();
        creditCard.setId(5L);
        creditCard.setNickname("Nubank");

        var csvTransaction = createTransaction(1L, value, date, TransactionSource.CSV_IMPORT);
        csvTransaction.setCategory(category);
        csvTransaction.setCreditCard(creditCard);

        when(transactionRepository.findPotentialDuplicates(eq(accountId), any(), any(), eq(value)))
                .thenReturn(List.of(csvTransaction));

        var result = service.findPotentialDuplicatesForManualEntry(accountId, date, value);

        assertEquals(1, result.size());
        assertEquals("Alimentação", result.get(0).getCategoryName());
        assertEquals("Nubank", result.get(0).getCreditCardNickname());
    }

    @Test
    void findAllPotentialDuplicates_whenNoMixedSources_returnsEmptyList() {
        var accountId = 1L;

        var csv1 = createTransaction(1L, BigDecimal.valueOf(-100), LocalDate.now(), TransactionSource.CSV_IMPORT);
        var csv2 = createTransaction(2L, BigDecimal.valueOf(-100), LocalDate.now(), TransactionSource.CSV_IMPORT);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(List.of(csv1, csv2));

        var result = service.findAllPotentialDuplicates(accountId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllPotentialDuplicates_whenMixedSourcesExist_returnsDuplicateGroups() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-150);

        var csvTransaction = createTransaction(1L, value, date, TransactionSource.CSV_IMPORT);
        csvTransaction.setDescription("SUPERMERCADO X LTDA");

        var manualTransaction = createTransaction(2L, value, date, TransactionSource.MANUAL);
        manualTransaction.setDescription("Supermercado X");

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(List.of(csvTransaction, manualTransaction));

        var result = service.findAllPotentialDuplicates(accountId);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getTransactions().size());

        var sources = result.get(0).getTransactions().stream()
                .map(t -> t.getSource())
                .toList();
        assertTrue(sources.contains(TransactionSource.CSV_IMPORT));
        assertTrue(sources.contains(TransactionSource.MANUAL));
    }

    @Test
    void findAllPotentialDuplicates_groupsTransactionsWithinWindow() {
        var accountId = 1L;
        var baseDate = LocalDate.of(2025, 1, 15);
        var value = BigDecimal.valueOf(-100);

        var csv1 = createTransaction(1L, value, baseDate, TransactionSource.CSV_IMPORT);
        var manual1 = createTransaction(2L, value, baseDate.plusDays(2), TransactionSource.MANUAL);
        var csv2 = createTransaction(3L, value, baseDate.plusDays(10), TransactionSource.CSV_IMPORT);

        when(transactionRepository.findAllByAccountId(accountId))
                .thenReturn(List.of(csv1, manual1, csv2));

        var result = service.findAllPotentialDuplicates(accountId);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getTransactions().size());

        var ids = result.get(0).getTransactions().stream().map(t -> t.getId()).toList();
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(2L));
        assertFalse(ids.contains(3L));
    }

    @Test
    void findAllPotentialDuplicates_doesNotGroupDifferentValues() {
        var accountId = 1L;
        var date = LocalDate.now();

        var csv = createTransaction(1L, BigDecimal.valueOf(-100), date, TransactionSource.CSV_IMPORT);
        var manual = createTransaction(2L, BigDecimal.valueOf(-150), date, TransactionSource.MANUAL);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(List.of(csv, manual));

        var result = service.findAllPotentialDuplicates(accountId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllPotentialDuplicates_doesNotGroupTransactionsOutsideWindow() {
        var accountId = 1L;
        var baseDate = LocalDate.of(2025, 1, 1);
        var value = BigDecimal.valueOf(-100);

        var csv = createTransaction(1L, value, baseDate, TransactionSource.CSV_IMPORT);
        var manual = createTransaction(2L, value, baseDate.plusDays(5), TransactionSource.MANUAL);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(List.of(csv, manual));

        var result = service.findAllPotentialDuplicates(accountId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllPotentialDuplicates_allowsMultipleCsvTransactionsWithSameValue() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-50);

        var csv1 = createTransaction(1L, value, date, TransactionSource.CSV_IMPORT);
        var csv2 = createTransaction(2L, value, date, TransactionSource.CSV_IMPORT);
        var csv3 = createTransaction(3L, value, date, TransactionSource.CSV_IMPORT);

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(List.of(csv1, csv2, csv3));

        var result = service.findAllPotentialDuplicates(accountId);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllPotentialDuplicates_includesSimplifiedDescription() {
        var accountId = 1L;
        var date = LocalDate.now();
        var value = BigDecimal.valueOf(-100);

        var csv = createTransaction(1L, value, date, TransactionSource.CSV_IMPORT);
        csv.setDescription("ORIGINAL DESC");
        csv.setSimplifiedDescription("Simplified");

        var manual = createTransaction(2L, value, date, TransactionSource.MANUAL);
        manual.setDescription("Manual entry");

        when(transactionRepository.findAllByAccountId(accountId)).thenReturn(List.of(csv, manual));

        var result = service.findAllPotentialDuplicates(accountId);

        assertEquals(1, result.size());

        var csvDto = result.get(0).getTransactions().stream()
                .filter(t -> t.getSource() == TransactionSource.CSV_IMPORT)
                .findFirst()
                .orElseThrow();

        assertEquals("Simplified", csvDto.getSimplifiedDescription());
    }

    private Transaction createTransaction(Long id, BigDecimal value, LocalDate date, TransactionSource source) {
        var account = new Account();
        account.setId(100L);

        var transaction = new Transaction();
        transaction.setId(id);
        transaction.setValue(value);
        transaction.setDate(date);
        transaction.setSource(source);
        transaction.setAccount(account);
        transaction.setDescription("Default Description");

        return transaction;
    }
}
