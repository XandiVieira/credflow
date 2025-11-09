package com.relyon.credflow.specification;

import com.relyon.credflow.model.transaction.TransactionFilter;

public final class TransactionFilterNormalizer {
    private TransactionFilterNormalizer() {}

    public static TransactionFilter normalize(TransactionFilter f) {
        var min = f.minAmount();
        var max = f.maxAmount();
        if (min != null && max != null && min.compareTo(max) > 0) {
            var tmp = min; min = max; max = tmp;
        }
        return new TransactionFilter(
                f.accountId(),
                f.fromDate(),
                f.toDate(),
                safeTrim(f.descriptionContains()),
                safeTrim(f.simplifiedContains()),
                min,
                max,
                f.responsibleUserIds(),
                f.categoryIds(),
                f.creditCardIds()
        );
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}