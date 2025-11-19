package com.relyon.credflow.service;

import com.relyon.credflow.model.report.CategoryReportDTO;
import com.relyon.credflow.model.report.CategoryReportDTO.CategoryExpenseDTO;
import com.relyon.credflow.model.report.CreditCardReportDTO;
import com.relyon.credflow.model.report.CreditCardReportDTO.CreditCardExpenseDTO;
import com.relyon.credflow.model.report.MonthComparisonDTO;
import com.relyon.credflow.model.report.MonthComparisonDTO.ComparisonSummaryDTO;
import com.relyon.credflow.model.report.MonthComparisonDTO.MonthlyDataDTO;
import com.relyon.credflow.model.report.UserReportDTO;
import com.relyon.credflow.model.report.UserReportDTO.UserExpenseDTO;
import com.relyon.credflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public CategoryReportDTO getCategoryReport(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating category report for account {} from {} to {}", accountId, startDate, endDate);

        var transactions = transactionRepository.search(
                accountId, null, null, startDate, endDate, null, null, null, null, null
        );

        var expenseTransactions = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getCategory() != null)
                .toList();

        var total = expenseTransactions.stream()
                .map(t -> t.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var categoryGroups = expenseTransactions.stream()
                .collect(Collectors.groupingBy(transaction -> transaction.getCategory()));

        var categoryExpenses = categoryGroups.entrySet().stream()
                .map(entry -> {
                    var category = entry.getKey();
                    var categoryTransactions = entry.getValue();

                    var amount = categoryTransactions.stream()
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    var count = categoryTransactions.size();

                    var percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    var average = count > 0
                            ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return CategoryExpenseDTO.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                            .parentCategoryName(category.getParentCategory() != null ? category.getParentCategory().getName() : null)
                            .amount(amount)
                            .transactionCount(count)
                            .percentage(percentage)
                            .averageTransactionAmount(average)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();

        var parentRollup = rollupToParentCategories(categoryExpenses, total);

        return CategoryReportDTO.builder()
                .categories(parentRollup)
                .totalExpense(total)
                .build();
    }

    private List<CategoryExpenseDTO> rollupToParentCategories(List<CategoryExpenseDTO> categories, BigDecimal total) {
        var withParents = categories.stream()
                .filter(c -> c.getParentCategoryId() != null)
                .collect(Collectors.groupingBy(CategoryExpenseDTO::getParentCategoryId));

        var parentTotals = withParents.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            var amount = entry.getValue().stream()
                                    .map(CategoryExpenseDTO::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            var count = entry.getValue().stream()
                                    .mapToInt(CategoryExpenseDTO::getTransactionCount)
                                    .sum();
                            return Map.entry(amount, count);
                        }
                ));

        return categories.stream()
                .map(cat -> {
                    if (cat.getParentCategoryId() == null && parentTotals.containsKey(cat.getCategoryId())) {
                        var rollupData = parentTotals.get(cat.getCategoryId());
                        var newAmount = cat.getAmount().add(rollupData.getKey());
                        var newCount = cat.getTransactionCount() + rollupData.getValue();
                        var newPercentage = total.compareTo(BigDecimal.ZERO) > 0
                                ? newAmount.multiply(BigDecimal.valueOf(100))
                                        .divide(total, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        var newAverage = newCount > 0
                                ? newAmount.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        return CategoryExpenseDTO.builder()
                                .categoryId(cat.getCategoryId())
                                .categoryName(cat.getCategoryName())
                                .parentCategoryId(cat.getParentCategoryId())
                                .parentCategoryName(cat.getParentCategoryName())
                                .amount(newAmount)
                                .transactionCount(newCount)
                                .percentage(newPercentage)
                                .averageTransactionAmount(newAverage)
                                .build();
                    }
                    return cat;
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserReportDTO getUserReport(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating user report for account {} from {} to {}", accountId, startDate, endDate);

        var transactions = transactionRepository.search(
                accountId, null, null, startDate, endDate, null, null, null, null, null
        );

        var expenseTransactions = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(transaction -> transaction.getResponsibleUsers() != null && !transaction.getResponsibleUsers().isEmpty())
                .toList();

        var total = expenseTransactions.stream()
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var userExpenseMap = new java.util.HashMap<Long, UserExpenseAccumulator>();

        for (var transaction : expenseTransactions) {
            var transactionAmount = transaction.getValue().abs();
            for (var user : transaction.getResponsibleUsers()) {
                userExpenseMap.computeIfAbsent(user.getId(),
                        userId -> new UserExpenseAccumulator(user.getId(), user.getName()))
                        .addTransaction(transactionAmount);
            }
        }

        var userExpenses = userExpenseMap.values().stream()
                .map(accumulator -> {
                    var percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? accumulator.amount.multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return UserExpenseDTO.builder()
                            .userId(accumulator.userId)
                            .userName(accumulator.userName)
                            .amount(accumulator.amount)
                            .transactionCount(accumulator.count)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();

        return UserReportDTO.builder()
                .users(userExpenses)
                .totalExpense(total)
                .build();
    }

    private static class UserExpenseAccumulator {
        Long userId;
        String userName;
        BigDecimal amount = BigDecimal.ZERO;
        int count = 0;

        UserExpenseAccumulator(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        void addTransaction(BigDecimal transactionAmount) {
            this.amount = this.amount.add(transactionAmount);
            this.count++;
        }
    }

    @Transactional(readOnly = true)
    public CreditCardReportDTO getCreditCardReport(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating credit card report for account {} from {} to {}", accountId, startDate, endDate);

        var transactions = transactionRepository.search(
                accountId, null, null, startDate, endDate, null, null, null, null, null
        );

        var expenseTransactions = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getCreditCard() != null)
                .toList();

        var total = expenseTransactions.stream()
                .map(t -> t.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var cardGroups = expenseTransactions.stream()
                .collect(Collectors.groupingBy(transaction -> transaction.getCreditCard()));

        var cardExpenses = cardGroups.entrySet().stream()
                .map(entry -> {
                    var card = entry.getKey();
                    var cardTransactions = entry.getValue();

                    var amount = cardTransactions.stream()
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    var count = cardTransactions.size();

                    var percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    var average = count > 0
                            ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return CreditCardExpenseDTO.builder()
                            .creditCardId(card.getId())
                            .creditCardNickname(card.getNickname())
                            .amount(amount)
                            .transactionCount(count)
                            .percentage(percentage)
                            .averageTransactionAmount(average)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();

        return CreditCardReportDTO.builder()
                .creditCards(cardExpenses)
                .totalExpense(total)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthComparisonDTO getMonthComparison(Long accountId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating month comparison for account {} from {} to {}", accountId, startDate, endDate);

        var transactions = transactionRepository.search(
                accountId, null, null, startDate, endDate, null, null, null, null, null
        );

        var monthlyGroups = transactions.stream()
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getDate())));

        var months = monthlyGroups.entrySet().stream()
                .map(entry -> {
                    var yearMonth = entry.getKey();
                    var monthlyTransactions = entry.getValue();

                    var income = monthlyTransactions.stream()
                            .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) > 0)
                            .map(transaction -> transaction.getValue())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    var expense = monthlyTransactions.stream()
                            .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    var balance = income.subtract(expense);

                    return MonthlyDataDTO.builder()
                            .year(yearMonth.getYear())
                            .month(yearMonth.getMonthValue())
                            .monthName(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                            .income(income)
                            .expense(expense)
                            .balance(balance)
                            .transactionCount(monthlyTransactions.size())
                            .build();
                })
                .sorted(Comparator.comparing(MonthlyDataDTO::getYear).thenComparing(MonthlyDataDTO::getMonth))
                .toList();

        var summary = calculateComparisonSummary(months);

        return MonthComparisonDTO.builder()
                .months(months)
                .summary(summary)
                .build();
    }

    private ComparisonSummaryDTO calculateComparisonSummary(List<MonthlyDataDTO> months) {
        if (months.isEmpty()) {
            return ComparisonSummaryDTO.builder()
                    .averageMonthlyIncome(BigDecimal.ZERO)
                    .averageMonthlyExpense(BigDecimal.ZERO)
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(BigDecimal.ZERO)
                    .highestExpenseMonth(BigDecimal.ZERO)
                    .highestExpenseMonthName(null)
                    .lowestExpenseMonth(BigDecimal.ZERO)
                    .lowestExpenseMonthName(null)
                    .build();
        }

        var totalIncome = months.stream()
                .map(MonthlyDataDTO::getIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var totalExpense = months.stream()
                .map(MonthlyDataDTO::getExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var avgIncome = totalIncome.divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);
        var avgExpense = totalExpense.divide(BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);

        var highest = months.stream()
                .max(Comparator.comparing(MonthlyDataDTO::getExpense))
                .orElseThrow();

        var lowest = months.stream()
                .min(Comparator.comparing(MonthlyDataDTO::getExpense))
                .orElseThrow();

        return ComparisonSummaryDTO.builder()
                .averageMonthlyIncome(avgIncome)
                .averageMonthlyExpense(avgExpense)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .highestExpenseMonth(highest.getExpense())
                .highestExpenseMonthName(highest.getMonthName() + " " + highest.getYear())
                .lowestExpenseMonth(lowest.getExpense())
                .lowestExpenseMonthName(lowest.getMonthName() + " " + lowest.getYear())
                .build();
    }
}
