package com.relyon.credflow.service;

import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardBillingService {

    private final TransactionService transactionService;
    private final CreditCardRepository creditCardRepository;
    private final LocalizedMessageTranslationService translationService;

    public BigDecimal computeAvailableLimit(Long creditCardId) {
        log.info("Computing available limit for credit card {}", creditCardId);

        var creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException(translationService.translateMessage("creditCard.notFound")));

        var billingCycleStartDate = calculateBillingCycleStartDate(creditCard.getClosingDay());
        log.info("Billing cycle start date for card {}: {}", creditCardId, billingCycleStartDate);

        var filter = new TransactionFilter(
                null,
                billingCycleStartDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(creditCardId),
                null,
                null,
                true
        );

        var transactions = transactionService.search(filter, null);

        var totalSpent = transactions.stream()
                .map(Transaction::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total spent in current billing cycle for card {}: {}", creditCardId, totalSpent);

        var availableLimit = creditCard.getCreditLimit().subtract(totalSpent);
        log.info("Available limit for card {}: {}", creditCardId, availableLimit);

        return availableLimit;
    }

    public CreditCardResponseDTO.CurrentBillDTO computeCurrentBill(Long creditCardId, Integer closingDay, Integer dueDay) {
        log.info("Computing current bill for credit card {}", creditCardId);

        var cycleStartDate = calculateBillingCycleStartDate(closingDay);
        var cycleClosingDate = calculateBillingCycleClosingDate(closingDay);
        var dueDate = calculateBillingDueDate(closingDay, dueDay);

        var filter = new TransactionFilter(
                null,
                cycleStartDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(creditCardId),
                null,
                null,
                true
        );

        var transactions = transactionService.search(filter, null);

        var totalAmount = transactions.stream()
                .map(Transaction::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var currentBill = new CreditCardResponseDTO.CurrentBillDTO();
        currentBill.setCycleStartDate(cycleStartDate);
        currentBill.setCycleClosingDate(cycleClosingDate);
        currentBill.setDueDate(dueDate);
        currentBill.setTotalAmount(totalAmount);

        log.info("Current bill for card {}: {} transactions, total amount: {}",
                creditCardId, transactions.size(), totalAmount);

        return currentBill;
    }

    public LocalDate calculateBillingCycleStartDate(Integer closingDay) {
        var today = LocalDate.now();
        var lastClosingDate = today.getDayOfMonth() >= closingDay
                ? today.withDayOfMonth(closingDay)
                : today.minusMonths(1).withDayOfMonth(closingDay);

        return lastClosingDate.plusDays(1);
    }

    public LocalDate calculateBillingCycleClosingDate(Integer closingDay) {
        var today = LocalDate.now();
        return today.getDayOfMonth() >= closingDay
                ? today.plusMonths(1).withDayOfMonth(closingDay)
                : today.withDayOfMonth(closingDay);
    }

    public LocalDate calculateBillingDueDate(Integer closingDay, Integer dueDay) {
        var today = LocalDate.now();
        return today.getDayOfMonth() >= closingDay
                ? today.plusMonths(1).withDayOfMonth(dueDay)
                : today.withDayOfMonth(dueDay);
    }
}
