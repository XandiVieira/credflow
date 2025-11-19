package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RefundDetectionService service;

    @Test
    void detectAndLinkReversal_whenAlreadyMarkedAsReversal_returnsEmptyAndSkipsDetection() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(100), LocalDate.now());
        transaction.setIsReversal(true);

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void detectAndLinkReversal_whenValueIsZero_returnsEmptyAndSkipsDetection() {
        var transaction = createTransaction(1L, BigDecimal.ZERO, LocalDate.now());

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void detectAndLinkReversal_whenValueIsNull_returnsEmptyAndSkipsDetection() {
        var transaction = createTransaction(1L, null, LocalDate.now());

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void detectAndLinkReversal_whenNoMatchingReversalFound_returnsEmpty() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), LocalDate.now());
        transaction.setDescription("Purchase at Store");

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of());

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verify(transactionRepository, times(1)).findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        );
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void detectAndLinkReversal_whenExactDescriptionMatch_linksTransactionsAndReturnsCandidateAlso() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), LocalDate.now());
        transaction.setDescription("Purchase at Store XYZ");

        var reversal = createTransaction(2L, BigDecimal.valueOf(100), LocalDate.now().plusDays(2));
        reversal.setDescription("Purchase at Store XYZ");

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of(reversal));

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isPresent());
        assertSame(reversal, result.get());

        verify(transactionRepository, times(2)).save(any(Transaction.class));

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());

        var savedTransactions = captor.getAllValues();
        assertTrue(savedTransactions.get(0).getIsReversal());
        assertTrue(savedTransactions.get(1).getIsReversal());
        assertNotNull(savedTransactions.get(0).getRelatedTransaction());
        assertNotNull(savedTransactions.get(1).getRelatedTransaction());
    }

    @Test
    void detectAndLinkReversal_whenSimilarDescriptionAboveThreshold_linksTransactions() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-150.50), LocalDate.now());
        transaction.setDescription("AMAZON PURCHASE 12345");

        var reversal = createTransaction(2L, BigDecimal.valueOf(150.50), LocalDate.now().plusDays(5));
        reversal.setDescription("AMAZON REFUND 12345");

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of(reversal));

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isPresent());
        assertSame(reversal, result.get());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void detectAndLinkReversal_whenPositiveValue_skipsDetection() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(100), LocalDate.now());
        transaction.setDescription("Refund from Store");

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void detectAndLinkReversal_whenDissimilarDescription_doesNotLink() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), LocalDate.now());
        transaction.setDescription("Grocery Store ABC");

        var notAReversal = createTransaction(2L, BigDecimal.valueOf(100), LocalDate.now().plusDays(1));
        notAReversal.setDescription("Restaurant XYZ Dinner");

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of(notAReversal));

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isEmpty());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void detectAndLinkReversal_withCreditCard_passesCardIdToRepository() {
        var creditCard = new CreditCard();
        creditCard.setId(5L);

        var transaction = createTransaction(1L, BigDecimal.valueOf(-200), LocalDate.now());
        transaction.setCreditCard(creditCard);

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), eq(5L)
        )).thenReturn(List.of());

        service.detectAndLinkReversal(transaction);

        verify(transactionRepository).findPotentialReversals(
                eq(transaction.getAccount().getId()),
                eq(transaction.getId()),
                eq(transaction.getValue()),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(5L)
        );
    }

    @Test
    void detectAndLinkReversal_withNoCreditCard_passesNullCardIdToRepository() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-200), LocalDate.now());
        transaction.setCreditCard(null);

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), isNull()
        )).thenReturn(List.of());

        service.detectAndLinkReversal(transaction);

        verify(transactionRepository).findPotentialReversals(
                eq(transaction.getAccount().getId()),
                eq(transaction.getId()),
                eq(transaction.getValue()),
                any(LocalDate.class),
                any(LocalDate.class),
                isNull()
        );
    }

    @Test
    void detectAndLinkReversal_searchesWithin90DaysWindow() {
        var transactionDate = LocalDate.of(2025, 1, 15);
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), transactionDate);

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of());

        service.detectAndLinkReversal(transaction);

        var captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(transactionRepository).findPotentialReversals(
                anyLong(),
                anyLong(),
                any(),
                captor.capture(),
                captor.capture(),
                any()
        );

        var dates = captor.getAllValues();
        assertEquals(LocalDate.of(2024, 10, 17), dates.get(0));
        assertEquals(LocalDate.of(2025, 4, 15), dates.get(1));
    }

    @Test
    void detectAndLinkReversal_whenMultipleCandidates_selectsFirstMatch() {
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), LocalDate.now());
        transaction.setDescription("Store Purchase");

        var reversal1 = createTransaction(2L, BigDecimal.valueOf(100), LocalDate.now().plusDays(1));
        reversal1.setDescription("Store Purchase");

        var reversal2 = createTransaction(3L, BigDecimal.valueOf(100), LocalDate.now().plusDays(2));
        reversal2.setDescription("Store Purchase");

        when(transactionRepository.findPotentialReversals(
                anyLong(), anyLong(), any(), any(), any(), any()
        )).thenReturn(List.of(reversal1, reversal2));

        var result = service.detectAndLinkReversal(transaction);

        assertTrue(result.isPresent());
        assertSame(reversal1, result.get());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    private Transaction createTransaction(Long id, BigDecimal value, LocalDate date) {
        var account = new Account();
        account.setId(100L);

        var transaction = new Transaction();
        transaction.setId(id);
        transaction.setValue(value);
        transaction.setDate(date);
        transaction.setAccount(account);
        transaction.setIsReversal(false);
        transaction.setDescription("Default Description");

        return transaction;
    }
}
