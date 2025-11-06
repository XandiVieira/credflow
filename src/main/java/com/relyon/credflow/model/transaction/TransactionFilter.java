package com.relyon.credflow.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransactionFilter(
        Long accountId,
        LocalDate fromDate,
        LocalDate toDate,
        String descriptionContains,
        String simplifiedContains,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        List<Long> responsibleUserIds,
        List<Long> categoryIds,
        Long creditCardId
) {}