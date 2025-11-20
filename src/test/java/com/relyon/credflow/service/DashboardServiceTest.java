package com.relyon.credflow.service;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        accountId = 1L;
        startDate = LocalDate.of(2025, 1, 1);
        endDate = LocalDate.of(2025, 1, 31);
    }

    @Test
    void getDashboardSummary_withTransactions_shouldCalculateCorrectly() {
        var category1 = createCategory(1L, "Food");
        var category2 = createCategory(2L, "Transport");
        var creditCard = createCreditCard(1L, "Test Card");

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(1000), "Salary", null, null),
                createTransaction(2L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-200), "Groceries", category1, null),
                createTransaction(3L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-150), "Uber", category2, null),
                createTransaction(4L, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(-100), "Restaurant", category1, null),
                createTransaction(5L, LocalDate.of(2025, 2, 5), BigDecimal.valueOf(-50), "Future Bill", category1, creditCard)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions.subList(0, 4))
                .thenReturn(List.of(transactions.get(4)));

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var summary = dashboardService.getDashboardSummary(filter);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(450));
        assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(550));

        assertThat(summary.getTopCategories()).hasSize(2);
        assertThat(summary.getTopCategories().getFirst().getCategoryName()).isEqualTo("Food");
        assertThat(summary.getTopCategories().getFirst().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(summary.getTopCategories().getFirst().getTransactionCount()).isEqualTo(2);

        assertThat(summary.getUpcomingBills()).hasSize(1);
        assertThat(summary.getUpcomingBills().getFirst().getDescription()).isEqualTo("Future Bill");
        assertThat(summary.getUpcomingBills().getFirst().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));

        assertThat(summary.getBalanceTrend()).isNotEmpty();
    }

    @Test
    void getDashboardSummary_withNoTransactions_shouldReturnZeros() {
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(List.of());

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var summary = dashboardService.getDashboardSummary(filter);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTopCategories()).isEmpty();
        assertThat(summary.getUpcomingBills()).isEmpty();
    }

    @Test
    void getDashboardSummary_withOnlyIncome_shouldCalculateCorrectly() {
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(1000), "Salary", null, null),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(500), "Bonus", null, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions)
                .thenReturn(List.of());

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var summary = dashboardService.getDashboardSummary(filter);

        assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(summary.getTopCategories()).isEmpty();
    }

    @Test
    void getExpenseTrend_withMultipleDates_shouldGroupByDate() {
        var category = createCategory(1L, "Food");
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-100), "Groceries", category, null),
                createTransaction(2L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-50), "Restaurant", category, null),
                createTransaction(3L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-200), "Supermarket", category, null),
                createTransaction(4L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(500), "Income", null, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions);

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var trend = dashboardService.getExpenseTrend(filter);

        assertThat(trend.getDataPoints()).hasSize(2);

        var jan5 = trend.getDataPoints().stream()
                .filter(p -> p.getDate().equals(LocalDate.of(2025, 1, 5)))
                .findFirst()
                .orElseThrow();
        assertThat(jan5.getValue()).isEqualByComparingTo(BigDecimal.valueOf(150));

        var jan10 = trend.getDataPoints().stream()
                .filter(p -> p.getDate().equals(LocalDate.of(2025, 1, 10)))
                .findFirst()
                .orElseThrow();
        assertThat(jan10.getValue()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void getExpenseTrend_withNoExpenses_shouldReturnEmpty() {
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(1000), "Income", null, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions);

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var trend = dashboardService.getExpenseTrend(filter);

        assertThat(trend.getDataPoints()).isEmpty();
    }

    @Test
    void getCategoryDistribution_withMultipleCategories_shouldCalculatePercentages() {
        var category1 = createCategory(1L, "Food");
        var category2 = createCategory(2L, "Transport");
        var category3 = createCategory(3L, "Entertainment");

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-400), "Groceries", category1, null),
                createTransaction(2L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-200), "Uber", category2, null),
                createTransaction(3L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-100), "Cinema", category3, null),
                createTransaction(4L, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(-100), "Restaurant", category1, null),
                createTransaction(5L, LocalDate.of(2025, 1, 25), BigDecimal.valueOf(-200), "Taxi", category2, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions);

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var distribution = dashboardService.getCategoryDistribution(filter);

        assertThat(distribution.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(distribution.getSlices()).hasSize(3);

        var foodSlice = distribution.getSlices().stream()
                .filter(s -> s.getCategoryName().equals("Food"))
                .findFirst()
                .orElseThrow();
        assertThat(foodSlice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(foodSlice.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(foodSlice.getColor()).isNotNull();

        var transportSlice = distribution.getSlices().stream()
                .filter(s -> s.getCategoryName().equals("Transport"))
                .findFirst()
                .orElseThrow();
        assertThat(transportSlice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(transportSlice.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(40.00));

        var entertainmentSlice = distribution.getSlices().stream()
                .filter(s -> s.getCategoryName().equals("Entertainment"))
                .findFirst()
                .orElseThrow();
        assertThat(entertainmentSlice.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(entertainmentSlice.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
    }

    @Test
    void getCategoryDistribution_withNoCategories_shouldReturnEmpty() {
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-100), "Uncategorized", null, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions);

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var distribution = dashboardService.getCategoryDistribution(filter);

        assertThat(distribution.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(distribution.getSlices()).isEmpty();
    }

    @Test
    void getCategoryDistribution_withSingleCategory_shouldReturn100Percent() {
        var category = createCategory(1L, "Food");
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 5), BigDecimal.valueOf(-300), "Groceries", category, null),
                createTransaction(2L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-700), "Restaurant", category, null)
        );

        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), eq(Sort.unsorted())))
                .thenReturn(transactions);

        var filter = new TransactionFilter(accountId, startDate, endDate, null, null,
                null, null, null, null, null, null, null, false);
        var distribution = dashboardService.getCategoryDistribution(filter);

        assertThat(distribution.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(distribution.getSlices()).hasSize(1);
        assertThat(distribution.getSlices().getFirst().getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    private Category createCategory(Long id, String name) {
        var category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private CreditCard createCreditCard(Long id, String nickname) {
        var creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setNickname(nickname);
        return creditCard;
    }

    private Transaction createTransaction(Long id, LocalDate date, BigDecimal value, String description,
                                          Category category, CreditCard creditCard) {
        var transaction = new Transaction();
        transaction.setId(id);
        transaction.setDate(date);
        transaction.setValue(value);
        transaction.setDescription(description);
        transaction.setCategory(category);
        transaction.setCreditCard(creditCard);
        return transaction;
    }
}
