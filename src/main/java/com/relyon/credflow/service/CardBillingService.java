package com.relyon.credflow.service;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class CardBillingService {

    private final TransactionService transactionService;

    public BigDecimal statementTotal(CreditCard card, YearMonth cycle) {
        var start = cycle.atDay(1);
        var end = cycle.atEndOfMonth();

        List<Transaction> txs = transactionService.findByFilters(
                card.getAccount().getId(),
                start,
                end,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        var total = txs.stream()
                .map(t -> Objects.requireNonNullElse(t.getValue(), BigDecimal.ZERO))
                .filter(v -> v.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.setScale(2, RoundingMode.HALF_UP);
    }


}
