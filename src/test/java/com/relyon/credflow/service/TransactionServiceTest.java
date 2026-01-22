package com.relyon.credflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private DescriptionMappingRepository descriptionMappingRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private UserService userService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private RefundDetectionService refundDetectionService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_setsSourceToManual() {
        var accountId = 1L;
        var account = createAccount(accountId);
        var transaction = createBasicTransaction();

        when(accountService.findById(accountId)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.create(transaction, accountId);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        var saved = captor.getValue();
        assertEquals(TransactionSource.MANUAL, saved.getSource());
        assertFalse(saved.getWasEditedAfterImport());
        assertFalse(saved.getIsReversal());
    }

    @Test
    void create_callsRefundDetectionService() {
        var accountId = 1L;
        var account = createAccount(accountId);
        var transaction = createBasicTransaction();

        when(accountService.findById(accountId)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.create(transaction, accountId);

        verify(refundDetectionService, times(1)).detectAndLinkReversal(any(Transaction.class));
    }

    @Test
    void update_whenSourceIsNotManual_marksAsEditedAfterImport() {
        var transactionId = 5L;
        var accountId = 1L;

        var existing = createBasicTransaction();
        existing.setId(transactionId);
        existing.setSource(TransactionSource.CSV_IMPORT);
        existing.setWasEditedAfterImport(false);

        var updated = createBasicTransaction();
        updated.setDescription("Updated Description");

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(createAccount(accountId));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.update(transactionId, updated, accountId);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        var saved = captor.getValue();
        assertTrue(saved.getWasEditedAfterImport());
    }

    @Test
    void update_whenSourceIsManual_doesNotMarkAsEditedAfterImport() {
        var transactionId = 5L;
        var accountId = 1L;

        var existing = createBasicTransaction();
        existing.setId(transactionId);
        existing.setSource(TransactionSource.MANUAL);
        existing.setWasEditedAfterImport(false);

        var updated = createBasicTransaction();

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(createAccount(accountId));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.update(transactionId, updated, accountId);

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        var saved = captor.getValue();
        assertFalse(saved.getWasEditedAfterImport());
    }

    @Test
    void update_callsRefundDetectionService() {
        var transactionId = 5L;
        var accountId = 1L;
        var existing = createBasicTransaction();
        existing.setId(transactionId);

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(existing));
        when(accountService.findById(accountId)).thenReturn(createAccount(accountId));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.update(transactionId, createBasicTransaction(), accountId);

        verify(refundDetectionService, times(1)).detectAndLinkReversal(any(Transaction.class));
    }

    @Test
    void bulkDelete_deletesAllTransactionsSuccessfully() {
        var accountId = 1L;
        var transactionIds = List.of(1L, 2L, 3L);

        var t1 = createBasicTransaction();
        t1.setId(1L);
        var t2 = createBasicTransaction();
        t2.setId(2L);
        var t3 = createBasicTransaction();
        t3.setId(3L);

        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.findByIdAndAccountId(2L, accountId)).thenReturn(Optional.of(t2));
        when(transactionRepository.findByIdAndAccountId(3L, accountId)).thenReturn(Optional.of(t3));

        transactionService.bulkDelete(transactionIds, accountId);

        ArgumentCaptor<Iterable<Transaction>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(transactionRepository).deleteAll(captor.capture());

        var deleted = (List<Transaction>) captor.getValue();
        assertEquals(3, deleted.size());
        assertTrue(deleted.contains(t1));
        assertTrue(deleted.contains(t2));
        assertTrue(deleted.contains(t3));
    }

    @Test
    void bulkDelete_whenTransactionNotFound_throwsResourceNotFoundException() {
        var accountId = 1L;
        var transactionIds = List.of(1L, 999L);

        var t1 = createBasicTransaction();
        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.findByIdAndAccountId(999L, accountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.bulkDelete(transactionIds, accountId));

        verify(transactionRepository, never()).deleteAll(any());
    }

    @Test
    void bulkUpdateCategory_updatesAllTransactionsWithNewCategory() {
        var accountId = 1L;
        var categoryId = 10L;
        var transactionIds = List.of(1L, 2L);

        var category = new Category();
        category.setId(categoryId);
        category.setName("Food");

        var t1 = createBasicTransaction();
        t1.setId(1L);
        var t2 = createBasicTransaction();
        t2.setId(2L);

        when(categoryService.findById(categoryId, accountId)).thenReturn(category);
        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.findByIdAndAccountId(2L, accountId)).thenReturn(Optional.of(t2));
        when(transactionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = transactionService.bulkUpdateCategory(transactionIds, categoryId, accountId);

        assertEquals(2, result.size());
        assertEquals(category, t1.getCategory());
        assertEquals(category, t2.getCategory());
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void bulkUpdateCategory_withNullCategoryId_setsCategoriesToNull() {
        var accountId = 1L;
        var transactionIds = List.of(1L);

        var t1 = createBasicTransaction();
        t1.setId(1L);
        t1.setCategory(new Category());

        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.bulkUpdateCategory(transactionIds, null, accountId);

        assertNull(t1.getCategory());
        verify(categoryService, never()).findById(any(), any());
    }

    @Test
    void bulkUpdateCategory_marksImportedTransactionsAsEdited() {
        var accountId = 1L;
        var categoryId = 10L;
        var transactionIds = List.of(1L);

        var category = new Category();
        category.setId(categoryId);

        var t1 = createBasicTransaction();
        t1.setId(1L);
        t1.setSource(TransactionSource.INVOICE_IMPORT);
        t1.setWasEditedAfterImport(false);

        when(categoryService.findById(categoryId, accountId)).thenReturn(category);
        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.bulkUpdateCategory(transactionIds, categoryId, accountId);

        assertTrue(t1.getWasEditedAfterImport());
    }

    @Test
    void bulkUpdateResponsibleUsers_updatesAllTransactionsWithNewResponsibleUsers() {
        var accountId = 1L;
        var responsibleIds = List.of(5L, 6L);
        var transactionIds = List.of(1L, 2L);

        var user1 = createUser(5L, "John");
        var user2 = createUser(6L, "Jane");
        var account = createAccount(accountId);
        user1.setAccount(account);
        user2.setAccount(account);

        var t1 = createBasicTransaction();
        t1.setId(1L);
        var t2 = createBasicTransaction();
        t2.setId(2L);

        when(userService.findAllByIds(any())).thenReturn(List.of(user1, user2));
        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.findByIdAndAccountId(2L, accountId)).thenReturn(Optional.of(t2));
        when(transactionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = transactionService.bulkUpdateResponsibleUsers(transactionIds, responsibleIds, accountId);

        assertEquals(2, result.size());
        assertEquals(2, t1.getResponsibleUsers().size());
        assertEquals(2, t2.getResponsibleUsers().size());
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void bulkUpdateResponsibleUsers_marksImportedTransactionsAsEdited() {
        var accountId = 1L;
        var responsibleIds = List.of(5L);

        var user = createUser(5L, "John");
        var account = createAccount(accountId);
        user.setAccount(account);

        var t1 = createBasicTransaction();
        t1.setId(1L);
        t1.setSource(TransactionSource.CSV_IMPORT);
        t1.setWasEditedAfterImport(false);

        when(userService.findAllByIds(responsibleIds)).thenReturn(List.of(user));
        when(transactionRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(t1));
        when(transactionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        transactionService.bulkUpdateResponsibleUsers(List.of(1L), responsibleIds, accountId);

        assertTrue(t1.getWasEditedAfterImport());
    }

    @Test
    void delete_deletesTransactionSuccessfully() {
        var transactionId = 1L;
        var accountId = 1L;
        var transaction = createBasicTransaction();

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        transactionService.delete(transactionId, accountId);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void delete_whenNotFound_throwsResourceNotFoundException() {
        var transactionId = 999L;
        var accountId = 1L;

        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.delete(transactionId, accountId));

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    private Transaction createBasicTransaction() {
        var transaction = new Transaction();
        transaction.setDate(LocalDate.now());
        transaction.setDescription("Test Transaction");
        transaction.setValue(BigDecimal.valueOf(100));
        transaction.setTransactionType(TransactionType.ONE_TIME);
        transaction.setResponsibleUsers(new HashSet<>());
        transaction.setSource(TransactionSource.MANUAL);
        transaction.setWasEditedAfterImport(false);
        transaction.setIsReversal(false);
        return transaction;
    }

    private Account createAccount(Long id) {
        var account = new Account();
        account.setId(id);
        account.setName("Test Account");
        return account;
    }

    private User createUser(Long id, String name) {
        var user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}