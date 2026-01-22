package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.model.budget.BudgetPreferencesResponseDTO;
import com.relyon.credflow.model.budget.BudgetType;
import com.relyon.credflow.model.budget.WarningLevel;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.BudgetRepository;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BudgetTrackingServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetPreferencesService budgetPreferencesService;

    @InjectMocks
    private BudgetTrackingService budgetTrackingService;

    private Long accountId;
    private BudgetPreferencesResponseDTO defaultPreferences;

    @BeforeEach
    void setUp() {
        accountId = 1L;

        defaultPreferences = BudgetPreferencesResponseDTO.builder()
                .enableAlerts(true)
                .enableRollover(true)
                .rolloverMaxMonths(2)
                .rolloverMaxPercentage(50)
                .yellowWarningThreshold(80)
                .orangeWarningThreshold(100)
                .redWarningThreshold(120)
                .enableProjectedWarnings(true)
                .projectedWarningMinDays(5)
                .build();
    }

    @Test
    void trackBudget_withSpending_shouldCalculateCorrectly() {
        var period = YearMonth.of(2025, 1);
        var budget = createBudget(1L, period, BigDecimal.valueOf(1000), BudgetType.ACCOUNT_WIDE);

        var transactions = List.of(
                createTransaction(LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-200)),
                createTransaction(LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-300))
        );

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));
        when(budgetPreferencesService.getPreferencesForAccount(accountId, null)).thenReturn(defaultPreferences);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted()))).thenReturn(transactions);

        var result = budgetTrackingService.trackBudget(1L, accountId);

        assertThat(result.getBudgetId()).isEqualTo(1L);
        assertThat(result.getEffectiveBudget()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.getCurrentSpend()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.getRemainingBudget()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.getPercentageUsed()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void trackBudget_withRollover_shouldIncludeInEffectiveBudget() {
        var period = YearMonth.of(2025, 1);
        var budget = createBudget(1L, period, BigDecimal.valueOf(1000), BudgetType.ACCOUNT_WIDE);
        budget.setRolledOverAmount(BigDecimal.valueOf(200));

        var transactions = List.of(
                createTransaction(LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-400))
        );

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));
        when(budgetPreferencesService.getPreferencesForAccount(accountId, null)).thenReturn(defaultPreferences);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted()))).thenReturn(transactions);

        var result = budgetTrackingService.trackBudget(1L, accountId);

        assertThat(result.getEffectiveBudget()).isEqualByComparingTo(BigDecimal.valueOf(1200));
        assertThat(result.getRolledOverAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(result.getCurrentSpend()).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    @Test
    void trackBudget_withProjectedOverspend_shouldCalculateWarning() {
        var period = YearMonth.now();
        var budget = createBudget(1L, period, BigDecimal.valueOf(1000), BudgetType.ACCOUNT_WIDE);

        var today = LocalDate.now();
        var dayOfMonth = today.getDayOfMonth();
        var spendPerDay = BigDecimal.valueOf(100);
        var totalSpendSoFar = spendPerDay.multiply(BigDecimal.valueOf(dayOfMonth));

        var transactions = List.of(
                createTransaction(today, totalSpendSoFar.negate())
        );

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));
        when(budgetPreferencesService.getPreferencesForAccount(accountId, null)).thenReturn(defaultPreferences);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted()))).thenReturn(transactions);

        var result = budgetTrackingService.trackBudget(1L, accountId);

        assertThat(result.getDailyBurnRate()).isNotNull();
        assertThat(result.getProjectedEndSpend()).isNotNull();

        if (result.getProjectedEndSpend().compareTo(result.getEffectiveBudget()) > 0) {
            assertThat(result.getWarningLevel()).isNotEqualTo(WarningLevel.NONE);
            assertThat(result.getWarningMessage()).isNotNull();
        }
    }

    @Test
    void trackBudget_categorySpecific_shouldOnlyCountMatchingTransactions() {
        var period = YearMonth.of(2025, 1);
        var category = new Category();
        category.setId(10L);
        category.setName("Food");

        var budget = createBudget(1L, period, BigDecimal.valueOf(500), BudgetType.CATEGORY_SPECIFIC);
        budget.setCategory(category);

        var matchingTransaction = createTransaction(LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-200));
        matchingTransaction.setCategory(category);

        var otherCategory = new Category();
        otherCategory.setId(20L);
        var nonMatchingTransaction = createTransaction(LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-100));
        nonMatchingTransaction.setCategory(otherCategory);

        var transactions = List.of(matchingTransaction, nonMatchingTransaction);

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));
        when(budgetPreferencesService.getPreferencesForAccount(accountId, null)).thenReturn(defaultPreferences);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted()))).thenReturn(transactions);

        var result = budgetTrackingService.trackBudget(1L, accountId);

        assertThat(result.getCurrentSpend()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void trackBudget_userSpecific_shouldOnlyCountMatchingTransactions() {
        var period = YearMonth.of(2025, 1);
        var user = new User();
        user.setId(5L);
        user.setName("John");

        var budget = createBudget(1L, period, BigDecimal.valueOf(500), BudgetType.USER_SPECIFIC);
        budget.setUser(user);

        var matchingTransaction = createTransaction(LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-200));
        matchingTransaction.setResponsibleUsers(Set.of(user));

        var otherUser = new User();
        otherUser.setId(6L);
        var nonMatchingTransaction = createTransaction(LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-100));
        nonMatchingTransaction.setResponsibleUsers(Set.of(otherUser));

        var transactions = List.of(matchingTransaction, nonMatchingTransaction);

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));
        when(budgetPreferencesService.getPreferencesForAccount(accountId, 5L)).thenReturn(defaultPreferences);
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted()))).thenReturn(transactions);

        var result = budgetTrackingService.trackBudget(1L, accountId);

        assertThat(result.getCurrentSpend()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    private Budget createBudget(Long id, YearMonth period, BigDecimal amount, BudgetType type) {
        return Budget.builder()
                .id(id)
                .period(period)
                .amount(amount)
                .type(type)
                .allowRollover(false)
                .rolledOverAmount(BigDecimal.ZERO)
                .build();
    }

    private Transaction createTransaction(LocalDate date, BigDecimal value) {
        var transaction = new Transaction();
        transaction.setDate(date);
        transaction.setValue(value);
        transaction.setDescription("Test");
        return transaction;
    }
}
