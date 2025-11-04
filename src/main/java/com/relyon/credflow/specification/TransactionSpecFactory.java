package com.relyon.credflow.specification;

import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionSpecFactory  {

    private TransactionSpecFactory() {}

    public static Specification<Transaction> from(TransactionFilter f) {
        return Specification.allOf(
                accountIdEq(f.accountId()),
                dateFrom(f.fromDate()),
                dateTo(f.toDate()),
                likeLower("description", f.descriptionContains()),
                likeLower("simplifiedDescription", f.simplifiedContains()),
                amountGte(f.minAmount()),
                amountLte(f.maxAmount()),
                anyResponsibleIn(f.responsibleUserIds()),
                categoryIn(f.categoryIds())
        );
    }

    private static Specification<Transaction> accountIdEq(Long accountId) {
        return (root, q, cb) -> accountId == null ? null : cb.equal(root.get("account").get("id"), accountId);
    }
    private static Specification<Transaction> dateFrom(LocalDate from) {
        return (root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("date"), from);
    }
    private static Specification<Transaction> dateTo(LocalDate to) {
        return (root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("date"), to);
    }
    private static Specification<Transaction> amountGte(BigDecimal min) {
        return (root, q, cb) -> min == null ? null : cb.greaterThanOrEqualTo(root.get("value"), min);
    }
    private static Specification<Transaction> amountLte(BigDecimal max) {
        return (root, q, cb) -> max == null ? null : cb.lessThanOrEqualTo(root.get("value"), max);
    }
    private static Specification<Transaction> anyResponsibleIn(List<Long> userIds) {
        return (root, q, cb) -> {
            if (userIds == null || userIds.isEmpty()) return null;
            q.distinct(true);
            var join = root.join("responsibles", JoinType.LEFT);
            return join.get("id").in(userIds);
        };
    }
    private static Specification<Transaction> categoryIn(List<Long> categoryIds) {
        return (root, q, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) return null;
            q.distinct(true);
            var join = root.join("category", JoinType.LEFT);
            return join.get("id").in(categoryIds);
        };
    }
    private static Specification<Transaction> likeLower(String field, String value) {
        return (root, q, cb) -> {
            var p = toLike(value);
            return p == null ? null : cb.like(cb.lower(root.get(field)), p);
        };
    }
    private static String toLike(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : "%" + s.toLowerCase() + "%";
    }
}
