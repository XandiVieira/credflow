package com.relyon.credflow.service;

import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.model.budget.BudgetPreferencesResponseDTO;
import com.relyon.credflow.model.budget.BudgetTrackingDTO;
import com.relyon.credflow.model.budget.WarningLevel;
import com.relyon.credflow.repository.BudgetRepository;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.specification.TransactionSpecFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetTrackingService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetPreferencesService budgetPreferencesService;

    @Transactional(readOnly = true)
    public List<BudgetTrackingDTO> trackBudgets(YearMonth period, Long accountId) {
        log.info("Tracking budgets for account {} in period {}", accountId, period);

        var budgets = budgetRepository.findByAccountIdAndPeriod(accountId, period);
        var result = new ArrayList<BudgetTrackingDTO>();

        for (var budget : budgets) {
            var tracking = calculateBudgetTracking(budget, period, accountId);
            result.add(tracking);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public BudgetTrackingDTO trackBudget(Long budgetId, Long accountId) {
        log.info("Tracking budget {} for account {}", budgetId, accountId);

        var budget = budgetRepository.findByIdAndAccountId(budgetId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));

        return calculateBudgetTracking(budget, budget.getPeriod(), accountId);
    }

    @Transactional
    public void processRollover(YearMonth fromPeriod, Long accountId) {
        log.info("Processing rollover from period {} for account {}", fromPeriod, accountId);

        var preferences = budgetPreferencesService.getPreferencesForAccount(accountId, null);
        if (!preferences.getEnableRollover()) {
            log.info("Rollover disabled for account {}", accountId);
            return;
        }

        var budgets = budgetRepository.findByAccountIdAndPeriod(accountId, fromPeriod);
        var nextPeriod = fromPeriod.plusMonths(1);

        for (var budget : budgets) {
            if (!budget.getAllowRollover()) {
                continue;
            }

            var startDate = fromPeriod.atDay(1);
            var endDate = fromPeriod.atEndOfMonth();
            var currentSpend = calculateSpend(budget, startDate, endDate, accountId);
            var effectiveBudget = budget.getAmount().add(budget.getRolledOverAmount());
            var unused = effectiveBudget.subtract(currentSpend);

            if (unused.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("No unused budget to rollover for budget {}", budget.getId());
                continue;
            }

            var maxRollover = calculateMaxRollover(budget.getAmount(), preferences);
            var rolloverAmount = unused.min(maxRollover);

            var nextBudget = budgetRepository.findMatchingBudget(
                    accountId,
                    nextPeriod,
                    budget.getType(),
                    budget.getCategory() != null ? budget.getCategory().getId() : null,
                    budget.getUser() != null ? budget.getUser().getId() : null
            );

            if (nextBudget.isPresent()) {
                var existing = nextBudget.get();
                existing.setRolledOverAmount(rolloverAmount);
                budgetRepository.save(existing);
                log.info("Rolled over {} to existing budget {} for period {}", rolloverAmount, existing.getId(), nextPeriod);
            } else {
                var newBudget = Budget.builder()
                        .account(budget.getAccount())
                        .period(nextPeriod)
                        .amount(budget.getAmount())
                        .type(budget.getType())
                        .category(budget.getCategory())
                        .user(budget.getUser())
                        .allowRollover(budget.getAllowRollover())
                        .rolledOverAmount(rolloverAmount)
                        .build();
                budgetRepository.save(newBudget);
                log.info("Created new budget with rollover {} for period {}", rolloverAmount, nextPeriod);
            }
        }
    }

    private BudgetTrackingDTO calculateBudgetTracking(Budget budget, YearMonth period, Long accountId) {
        var preferences = budgetPreferencesService.getPreferencesForAccount(
                accountId,
                budget.getUser() != null ? budget.getUser().getId() : null
        );

        var startDate = period.atDay(1);
        var endDate = period.atEndOfMonth();
        var today = LocalDate.now();

        if (today.isBefore(startDate)) {
            today = startDate;
        } else if (today.isAfter(endDate)) {
            today = endDate;
        }

        var currentSpend = calculateSpend(budget, startDate, today, accountId);
        var effectiveBudget = budget.getAmount().add(budget.getRolledOverAmount());
        var remainingBudget = effectiveBudget.subtract(currentSpend);
        var percentageUsed = effectiveBudget.compareTo(BigDecimal.ZERO) > 0
                ? currentSpend.multiply(BigDecimal.valueOf(100))
                .divide(effectiveBudget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        var daysElapsed = (int) ChronoUnit.DAYS.between(startDate, today) + 1;
        var totalDays = period.lengthOfMonth();
        var daysRemaining = totalDays - daysElapsed;

        BigDecimal dailyBurnRate = BigDecimal.ZERO;
        BigDecimal projectedEndSpend = currentSpend;
        BigDecimal projectedOverspend = BigDecimal.ZERO;

        if (daysElapsed > 0 && preferences.getEnableProjectedWarnings() && daysRemaining >= preferences.getProjectedWarningMinDays()) {
            dailyBurnRate = currentSpend.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
            projectedEndSpend = dailyBurnRate.multiply(BigDecimal.valueOf(totalDays));
            projectedOverspend = projectedEndSpend.subtract(effectiveBudget);
        }

        var warningLevel = calculateWarningLevel(projectedEndSpend, effectiveBudget, preferences);
        var warningMessage = buildWarningMessage(warningLevel, projectedOverspend, effectiveBudget);

        return BudgetTrackingDTO.builder()
                .budgetId(budget.getId())
                .period(period)
                .categoryName(budget.getCategory() != null ? budget.getCategory().getName() : null)
                .userName(budget.getUser() != null ? budget.getUser().getName() : null)
                .budgetAmount(budget.getAmount())
                .rolledOverAmount(budget.getRolledOverAmount())
                .effectiveBudget(effectiveBudget)
                .currentSpend(currentSpend)
                .remainingBudget(remainingBudget)
                .percentageUsed(percentageUsed)
                .daysElapsed(daysElapsed)
                .daysRemaining(daysRemaining)
                .totalDaysInPeriod(totalDays)
                .dailyBurnRate(dailyBurnRate)
                .projectedEndSpend(projectedEndSpend)
                .projectedOverspend(projectedOverspend)
                .warningLevel(warningLevel)
                .warningMessage(warningMessage)
                .build();
    }

    private BigDecimal calculateSpend(Budget budget, LocalDate startDate, LocalDate endDate, Long accountId) {
        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.unsorted());

        return transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .filter(transaction -> matchesBudget(transaction, budget))
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean matchesBudget(Transaction transaction, Budget budget) {
        return switch (budget.getType()) {
            case ACCOUNT_WIDE -> true;
            case CATEGORY_SPECIFIC ->
                    transaction.getCategory() != null && transaction.getCategory().getId().equals(budget.getCategory().getId());
            case USER_SPECIFIC -> transaction.getResponsibleUsers() != null &&
                    transaction.getResponsibleUsers().stream()
                            .anyMatch(user -> user.getId().equals(budget.getUser().getId()));
            case CATEGORY_USER_SPECIFIC -> transaction.getCategory() != null &&
                    transaction.getCategory().getId().equals(budget.getCategory().getId()) &&
                    transaction.getResponsibleUsers() != null &&
                    transaction.getResponsibleUsers().stream()
                            .anyMatch(user -> user.getId().equals(budget.getUser().getId()));
        };
    }

    private BigDecimal calculateMaxRollover(BigDecimal budgetAmount, BudgetPreferencesResponseDTO preferences) {
        var percentageLimit = budgetAmount
                .multiply(BigDecimal.valueOf(preferences.getRolloverMaxPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return percentageLimit;
    }

    private WarningLevel calculateWarningLevel(BigDecimal projectedSpend, BigDecimal budget, BudgetPreferencesResponseDTO preferences) {
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            return WarningLevel.NONE;
        }

        var percentage = projectedSpend.multiply(BigDecimal.valueOf(100))
                .divide(budget, 2, RoundingMode.HALF_UP);

        if (percentage.compareTo(BigDecimal.valueOf(preferences.getRedWarningThreshold())) >= 0) {
            return WarningLevel.RED;
        } else if (percentage.compareTo(BigDecimal.valueOf(preferences.getOrangeWarningThreshold())) >= 0) {
            return WarningLevel.ORANGE;
        } else if (percentage.compareTo(BigDecimal.valueOf(preferences.getYellowWarningThreshold())) >= 0) {
            return WarningLevel.YELLOW;
        }

        return WarningLevel.NONE;
    }

    private String buildWarningMessage(WarningLevel level, BigDecimal overspend, BigDecimal budget) {
        if (level == WarningLevel.NONE) {
            return "";
        }

        if (overspend.compareTo(BigDecimal.ZERO) <= 0) {
            return "On track to stay within budget";
        }

        var percentage = overspend.multiply(BigDecimal.valueOf(100))
                .divide(budget, 0, RoundingMode.HALF_UP);

        return String.format("At current pace, you'll exceed budget by $%s (%d%% over)",
                overspend.setScale(2, RoundingMode.HALF_UP), percentage.intValue());
    }
}
