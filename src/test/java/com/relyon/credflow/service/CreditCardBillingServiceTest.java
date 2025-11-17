package com.relyon.credflow.service;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.repository.CreditCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardBillingServiceTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private CreditCardBillingService service;

    @Test
    void calculateBillingCycleStartDate_whenTodayBeforeClosingDay_shouldReturnLastMonthClosingPlusOne() {
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 10);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingCycleStartDate(closingDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 10, 16));
    }

    @Test
    void calculateBillingCycleStartDate_whenTodayAfterClosingDay_shouldReturnThisMonthClosingPlusOne() {
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 20);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingCycleStartDate(closingDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 11, 16));
    }

    @Test
    void calculateBillingCycleStartDate_whenTodayEqualsClosingDay_shouldReturnThisMonthClosingPlusOne() {
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 15);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingCycleStartDate(closingDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 11, 16));
    }

    @Test
    void calculateBillingCycleClosingDate_whenTodayBeforeClosingDay_shouldReturnThisMonthClosing() {
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 10);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingCycleClosingDate(closingDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 11, 15));
    }

    @Test
    void calculateBillingCycleClosingDate_whenTodayAfterClosingDay_shouldReturnNextMonthClosing() {
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 20);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingCycleClosingDate(closingDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 12, 15));
    }

    @Test
    void calculateBillingDueDate_whenTodayBeforeClosingDay_shouldReturnThisMonthDueDay() {
        var closingDay = 15;
        var dueDay = 25;
        var today = LocalDate.of(2025, 11, 10);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingDueDate(closingDay, dueDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 11, 25));
    }

    @Test
    void calculateBillingDueDate_whenTodayAfterClosingDay_shouldReturnNextMonthDueDay() {
        var closingDay = 15;
        var dueDay = 25;
        var today = LocalDate.of(2025, 11, 20);

        LocalDate result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.calculateBillingDueDate(closingDay, dueDay);
        }

        assertThat(result).isEqualTo(LocalDate.of(2025, 12, 25));
    }

    @Test
    void computeAvailableLimit_whenCreditCardExists_shouldReturnLimitMinusCurrentSpending() {
        var creditCardId = 1L;
        var creditCard = CreditCard.builder()
                .id(creditCardId)
                .closingDay(15)
                .creditLimit(new BigDecimal("5000.00"))
                .build();

        var transaction1 = Transaction.builder().value(new BigDecimal("100.00")).build();
        var transaction2 = Transaction.builder().value(new BigDecimal("250.50")).build();

        when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));
        when(transactionService.search(any(TransactionFilter.class), eq(null)))
                .thenReturn(List.of(transaction1, transaction2));

        var result = service.computeAvailableLimit(creditCardId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("4649.50"));
        verify(creditCardRepository).findById(creditCardId);
        verify(transactionService).search(any(TransactionFilter.class), eq(null));
    }

    @Test
    void computeAvailableLimit_whenNoTransactions_shouldReturnFullLimit() {
        var creditCardId = 1L;
        var creditCard = CreditCard.builder()
                .id(creditCardId)
                .closingDay(15)
                .creditLimit(new BigDecimal("5000.00"))
                .build();

        when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));
        when(transactionService.search(any(TransactionFilter.class), eq(null)))
                .thenReturn(List.of());

        var result = service.computeAvailableLimit(creditCardId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void computeAvailableLimit_whenCreditCardNotFound_shouldThrowException() {
        var creditCardId = 999L;

        when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.empty());
        when(translationService.translateMessage("creditCard.notFound")).thenReturn("Credit card not found");

        assertThatThrownBy(() -> service.computeAvailableLimit(creditCardId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit card not found");
    }

    @Test
    void computeAvailableLimit_shouldUseCorrectFilterWithCycleStartDate() {
        var creditCardId = 1L;
        var closingDay = 15;
        var today = LocalDate.of(2025, 11, 20);
        var expectedCycleStart = LocalDate.of(2025, 11, 16);

        var creditCard = CreditCard.builder()
                .id(creditCardId)
                .closingDay(closingDay)
                .creditLimit(new BigDecimal("5000.00"))
                .build();

        when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));
        when(transactionService.search(any(TransactionFilter.class), eq(null))).thenReturn(List.of());

        BigDecimal result;
        try (MockedStatic<LocalDate> mockedStatic = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(LocalDate::now).thenReturn(today);
            result = service.computeAvailableLimit(creditCardId);
        }

        var filterCaptor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(transactionService).search(filterCaptor.capture(), eq(null));

        var capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter.fromDate()).isEqualTo(expectedCycleStart);
        assertThat(capturedFilter.creditCardIds()).containsExactly(creditCardId);
        assertThat(capturedFilter.includeReversals()).isTrue();
    }

    @Test
    void computeCurrentBill_shouldReturnBillWithCorrectDates() {
        var creditCardId = 1L;
        var closingDay = 15;
        var dueDay = 25;
        var today = LocalDate.of(2025, 11, 20);

        var transaction = Transaction.builder().value(new BigDecimal("300.00")).build();

        when(transactionService.search(any(TransactionFilter.class), eq(null)))
                .thenReturn(List.of(transaction));

        var result = service.computeCurrentBill(creditCardId, closingDay, dueDay);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void computeCurrentBill_whenNoTransactions_shouldReturnZeroAmount() {
        var creditCardId = 1L;
        var closingDay = 15;
        var dueDay = 25;

        when(transactionService.search(any(TransactionFilter.class), eq(null)))
                .thenReturn(List.of());

        var result = service.computeCurrentBill(creditCardId, closingDay, dueDay);

        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getCycleStartDate()).isNotNull();
        assertThat(result.getCycleClosingDate()).isNotNull();
        assertThat(result.getDueDate()).isNotNull();
    }

    @Test
    void computeCurrentBill_shouldSumAllTransactionsInCycle() {
        var creditCardId = 1L;
        var closingDay = 15;
        var dueDay = 25;

        var transaction1 = Transaction.builder().value(new BigDecimal("100.00")).build();
        var transaction2 = Transaction.builder().value(new BigDecimal("250.75")).build();
        var transaction3 = Transaction.builder().value(new BigDecimal("49.25")).build();

        when(transactionService.search(any(TransactionFilter.class), eq(null)))
                .thenReturn(List.of(transaction1, transaction2, transaction3));

        var result = service.computeCurrentBill(creditCardId, closingDay, dueDay);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    void computeCurrentBill_shouldUseCorrectFilterParameters() {
        var creditCardId = 1L;
        var closingDay = 15;
        var dueDay = 25;

        when(transactionService.search(any(TransactionFilter.class), eq(null))).thenReturn(List.of());

        service.computeCurrentBill(creditCardId, closingDay, dueDay);

        var filterCaptor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(transactionService).search(filterCaptor.capture(), eq(null));

        var capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter.creditCardIds()).containsExactly(creditCardId);
        assertThat(capturedFilter.includeReversals()).isTrue();
        assertThat(capturedFilter.fromDate()).isNotNull();
    }
}
