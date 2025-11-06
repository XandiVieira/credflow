package com.relyon.credflow.service;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.TransactionRepository;
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

    private final TransactionRepository transactionRepository;
    private final CreditCardRepository creditCardRepository;

    public BigDecimal computeAvailableLimit(Long creditCardId) {
        log.info("Computing available limit for credit card {}", creditCardId);

        CreditCard creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));

        LocalDate billingCycleStartDate = calculateBillingCycleStartDate(creditCard.getClosingDay());
        log.info("Billing cycle start date for card {}: {}", creditCardId, billingCycleStartDate);

        List<Transaction> transactions =
                transactionRepository.findByCreditCardIdAndDateAfter(creditCardId, billingCycleStartDate);

        BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total spent in current billing cycle for card {}: {}", creditCardId, totalSpent);

        BigDecimal availableLimit = creditCard.getCreditLimit().subtract(totalSpent);
        log.info("Available limit for card {}: {}", creditCardId, availableLimit);

        return availableLimit;
    }

    public CreditCardResponseDTO.CurrentBillDTO computeCurrentBill(Long creditCardId, Integer closingDay, Integer dueDay) {
        log.info("Computing current bill for credit card {}", creditCardId);

        LocalDate cycleStartDate = calculateBillingCycleStartDate(closingDay);
        LocalDate cycleClosingDate = calculateBillingCycleClosingDate(closingDay);
        LocalDate dueDate = calculateBillingDueDate(closingDay, dueDay);

        List<Transaction> transactions =
                transactionRepository.findByCreditCardIdAndDateAfter(creditCardId, cycleStartDate);

        BigDecimal totalAmount = transactions.stream()
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
        LocalDate today = LocalDate.now();
        LocalDate lastClosingDate;

        if (today.getDayOfMonth() >= closingDay) {
            lastClosingDate = today.withDayOfMonth(closingDay);
        } else {
            lastClosingDate = today.minusMonths(1).withDayOfMonth(closingDay);
        }

        return lastClosingDate.plusDays(1);
    }

    public LocalDate calculateBillingCycleClosingDate(Integer closingDay) {
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() >= closingDay) {
            return today.plusMonths(1).withDayOfMonth(closingDay);
        } else {
            return today.withDayOfMonth(closingDay);
        }
    }

    public LocalDate calculateBillingDueDate(Integer closingDay, Integer dueDay) {
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() >= closingDay) {
            return today.plusMonths(1).withDayOfMonth(dueDay);
        } else {
            return today.withDayOfMonth(dueDay);
        }
    }
}
