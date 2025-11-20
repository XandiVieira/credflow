package com.relyon.credflow.service;

import com.relyon.credflow.model.dashboard.*;
import com.relyon.credflow.model.dashboard.CategoryDistributionDTO.CategorySliceDTO;
import com.relyon.credflow.model.dashboard.TimeSeriesDataDTO.TimeSeriesPointDTO;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.specification.TransactionSpecFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private static final String[] CHART_COLORS = {
            "#4F46E5", "#EC4899", "#10B981", "#F59E0B", "#8B5CF6",
            "#06B6D4", "#EF4444", "#14B8A6", "#F97316", "#6366F1"
    };

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getDashboardSummary(TransactionFilter filter) {
        log.info("Generating dashboard summary for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.unsorted());

        var totalIncome = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(t -> t.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var totalExpense = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var balance = totalIncome.subtract(totalExpense);

        var topCategories = calculateTopCategories(transactions, totalExpense);
        var upcomingBills = calculateUpcomingBills(filter.accountId());
        var balanceTrend = calculateBalanceTrend(filter);

        return DashboardSummaryDTO.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .topCategories(topCategories)
                .upcomingBills(upcomingBills)
                .balanceTrend(balanceTrend)
                .build();
    }

    private List<CategorySummaryDTO> calculateTopCategories(
            List<com.relyon.credflow.model.transaction.Transaction> transactions,
            BigDecimal totalExpense) {

        var categoryTotals = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getValue().abs(),
                                BigDecimal::add
                        )
                ));

        return categoryTotals.entrySet().stream()
                .map(entry -> {
                    var percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(totalExpense, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    var count = transactions.stream()
                            .filter(t -> t.getCategory() != null && t.getCategory().equals(entry.getKey()))
                            .count();

                    return CategorySummaryDTO.builder()
                            .categoryId(entry.getKey().getId())
                            .categoryName(entry.getKey().getName())
                            .totalAmount(entry.getValue())
                            .transactionCount((int) count)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(5)
                .toList();
    }

    private List<UpcomingBillDTO> calculateUpcomingBills(Long accountId) {
        var today = LocalDate.now();
        var futureDate = today.plusDays(30);

        var filter = new TransactionFilter(accountId, today, futureDate, null, null,
                null, null, null, null, null, null, null, false);
        var spec = TransactionSpecFactory.from(filter);
        var upcomingTransactions = transactionRepository.findAll(spec, Sort.unsorted());

        return upcomingTransactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> {
                    var daysUntil = (int) ChronoUnit.DAYS.between(today, t.getDate());
                    return UpcomingBillDTO.builder()
                            .transactionId(t.getId())
                            .description(t.getDescription())
                            .amount(t.getValue().abs())
                            .dueDate(t.getDate())
                            .creditCardNickname(t.getCreditCard() != null ? t.getCreditCard().getNickname() : null)
                            .daysUntilDue(daysUntil)
                            .build();
                })
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(10)
                .toList();
    }

    private List<BalanceTrendDTO> calculateBalanceTrend(TransactionFilter filter) {
        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.unsorted());

        var groupedByDate = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate()));

        var trendList = new ArrayList<BalanceTrendDTO>();
        var runningBalance = BigDecimal.ZERO;

        var currentDate = filter.fromDate();
        while (!currentDate.isAfter(filter.toDate())) {
            var dailyTransactions = groupedByDate.getOrDefault(currentDate, List.of());

            var dailyIncome = dailyTransactions.stream()
                    .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .map(t -> t.getValue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            var dailyExpense = dailyTransactions.stream()
                    .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                    .map(t -> t.getValue().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            runningBalance = runningBalance.add(dailyIncome).subtract(dailyExpense);

            trendList.add(BalanceTrendDTO.builder()
                    .date(currentDate)
                    .income(dailyIncome)
                    .expense(dailyExpense)
                    .balance(runningBalance)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return trendList;
    }

    @Transactional(readOnly = true)
    public TimeSeriesDataDTO getExpenseTrend(TransactionFilter filter) {
        log.info("Generating expense trend for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.unsorted());

        var groupedByDate = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        t -> t.getDate(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getValue().abs(),
                                BigDecimal::add
                        )
                ));

        var dataPoints = groupedByDate.entrySet().stream()
                .map(entry -> TimeSeriesPointDTO.builder()
                        .date(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();

        return TimeSeriesDataDTO.builder()
                .dataPoints(dataPoints)
                .build();
    }

    @Transactional(readOnly = true)
    public CategoryDistributionDTO getCategoryDistribution(TransactionFilter filter) {
        log.info("Generating category distribution for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.unsorted());

        var expenseTransactions = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(t -> t.getCategory() != null)
                .toList();

        var total = expenseTransactions.stream()
                .map(t -> t.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var categoryTotals = expenseTransactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                t -> t.getValue().abs(),
                                BigDecimal::add
                        )
                ));

        var colorIndex = new int[]{0};
        var slices = categoryTotals.entrySet().stream()
                .map(entry -> {
                    var percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    var color = CHART_COLORS[colorIndex[0] % CHART_COLORS.length];
                    colorIndex[0]++;

                    return CategorySliceDTO.builder()
                            .categoryId(entry.getKey().getId())
                            .categoryName(entry.getKey().getName())
                            .amount(entry.getValue())
                            .percentage(percentage)
                            .color(color)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .toList();

        return CategoryDistributionDTO.builder()
                .slices(slices)
                .total(total)
                .build();
    }
}
