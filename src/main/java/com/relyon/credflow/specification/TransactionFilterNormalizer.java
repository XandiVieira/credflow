package com.relyon.credflow.specification;

import com.relyon.credflow.model.transaction.TransactionFilter;

public final class TransactionFilterNormalizer {
    private TransactionFilterNormalizer() {
    }

    public static TransactionFilter normalize(TransactionFilter transactionFilter) {
        var min = transactionFilter.minAmount();
        var max = transactionFilter.maxAmount();
        if (min != null && max != null && min.compareTo(max) > 0) {
            var tmp = min;
            min = max;
            max = tmp;
        }
        return new TransactionFilter(
                transactionFilter.accountId(),
                transactionFilter.fromDate(),
                transactionFilter.toDate(),
                safeTrim(transactionFilter.descriptionContains()),
                safeTrim(transactionFilter.simplifiedContains()),
                min,
                max,
                transactionFilter.responsibleUserIds(),
                transactionFilter.categoryIds(),
                transactionFilter.creditCardIds(),
                transactionFilter.transactionTypes(),
                transactionFilter.transactionSources(),
                false
        );
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}