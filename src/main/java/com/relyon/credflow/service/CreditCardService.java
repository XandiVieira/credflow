package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public List<CreditCardResponseDTO> findAll(Long accountId) {
        log.info("Fetching all credit cards for account {}", accountId);
        List<CreditCard> creditCards = creditCardRepository.findAllByAccountId(accountId);
        return creditCards.stream().map(creditCard -> {
            var creditCardResponse = creditCardMapper.toDTO(creditCard);
            enrichDTOWithCalculatedFields(creditCardResponse, creditCard);
            return creditCardResponse;
        }).toList();

    }

    public CreditCardResponseDTO findById(Long id, Long accountId) {
        log.info("Fetching credit card with id {} for account {}", id, accountId);
        CreditCard creditCard = creditCardRepository.findByIdAndAccountId(id, accountId).orElse(null);
        var creditCardResponse = creditCardMapper.toDTO(creditCard);
        enrichDTOWithCalculatedFields(creditCardResponse, creditCard);
        return creditCardResponse;
    }

    private void enrichDTOWithCalculatedFields(CreditCardResponseDTO dto, CreditCard entity) {
        dto.setAvailableCreditLimit(computeAvailableLimit(dto.getId()));
        dto.setCurrentBill(computeCurrentBill(dto.getId(), entity.getClosingDay(), entity.getDueDay()));
    }

    public CreditCard create(CreditCard creditCard, Long accountId, Long holderId) {
        log.info("Creating credit card for account {} with holder {}", accountId, holderId);

        User holder = userRepository.findByIdAndAccountId(holderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Holder not found or does not belong to this account"));

        creditCard.setAccount(Account.builder().id(accountId).build());
        creditCard.setHolder(holder);
        return creditCardRepository.save(creditCard);
    }

    public BigDecimal computeAvailableLimit(Long creditCardId) {
        log.info("Computing available limit for credit card {}", creditCardId);

        CreditCard creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new IllegalArgumentException("Credit card not found"));

        LocalDate billingCycleStartDate = calculateBillingCycleStartDate(creditCard.getClosingDay());
        log.info("Billing cycle start date for card {}: {}", creditCardId, billingCycleStartDate);

        List<com.relyon.credflow.model.transaction.Transaction> transactions =
                transactionRepository.findByCreditCardIdAndDateAfter(creditCardId, billingCycleStartDate);

        BigDecimal totalSpent = transactions.stream()
                .map(com.relyon.credflow.model.transaction.Transaction::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total spent in current billing cycle for card {}: {}", creditCardId, totalSpent);

        BigDecimal availableLimit = creditCard.getCreditLimit().subtract(totalSpent);
        log.info("Available limit for card {}: {}", creditCardId, availableLimit);

        return availableLimit;
    }

    public CreditCardResponseDTO.CurrentBillDTO computeCurrentBill(Long creditCardId, Integer closingDay, Integer dueDay) {
        log.info("Computing current bill for credit card {}", creditCardId);

        // Calcular datas do ciclo
        LocalDate cycleStartDate = calculateBillingCycleStartDate(closingDay);
        LocalDate cycleClosingDate = calculateBillingCycleClosingDate(closingDay);
        LocalDate dueDate = calculateBillingDueDate(closingDay, dueDay);

        // Buscar transações do período e calcular total
        List<com.relyon.credflow.model.transaction.Transaction> transactions =
                transactionRepository.findByCreditCardIdAndDateAfter(creditCardId, cycleStartDate);

        BigDecimal totalAmount = transactions.stream()
                .map(com.relyon.credflow.model.transaction.Transaction::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Montar o objeto
        var currentBill = new CreditCardResponseDTO.CurrentBillDTO();
        currentBill.setCycleStartDate(cycleStartDate);
        currentBill.setCycleClosingDate(cycleClosingDate);
        currentBill.setDueDate(dueDate);
        currentBill.setTotalAmount(totalAmount);

        log.info("Current bill for card {}: {} transactions, total amount: {}",
                creditCardId, transactions.size(), totalAmount);

        return currentBill;
    }

    private LocalDate calculateBillingCycleStartDate(Integer closingDay) {
        LocalDate today = LocalDate.now();
        LocalDate lastClosingDate;

        if (today.getDayOfMonth() >= closingDay) {
            lastClosingDate = today.withDayOfMonth(closingDay);
        } else {
            lastClosingDate = today.minusMonths(1).withDayOfMonth(closingDay);
        }

        return lastClosingDate.plusDays(1);
    }

    private LocalDate calculateBillingCycleClosingDate(Integer closingDay) {
        LocalDate today = LocalDate.now();

        if (today.getDayOfMonth() >= closingDay) {
            return today.plusMonths(1).withDayOfMonth(closingDay);
        } else {
            return today.withDayOfMonth(closingDay);
        }
    }

    private LocalDate calculateBillingDueDate(Integer closingDay, Integer dueDay) {
        LocalDate today = LocalDate.now();

        // Se hoje >= closingDay, a data de vencimento será no próximo mês
        if (today.getDayOfMonth() >= closingDay) {
            return today.plusMonths(1).withDayOfMonth(dueDay);
        } else {
            // Senão, a data de vencimento será neste mês
            return today.withDayOfMonth(dueDay);
        }
    }
}
