package com.relyon.credflow.service;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        accountId = 1L;
        startDate = LocalDate.of(2025, 1, 1);
        endDate = LocalDate.of(2025, 3, 31);
    }

    @Test
    void getCategoryReport_withHierarchy_shouldRollupToParent() {
        var parentCategory = createCategory(1L, "Food", null);
        var childCategory = createCategory(2L, "Restaurants", parentCategory);

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-300), null, parentCategory, null),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-200), null, childCategory, null)
        );

        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(transactions);

        var report = reportService.getCategoryReport(accountId, startDate, endDate);

        assertThat(report.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(report.getCategories()).hasSize(2);

        var parentInReport = report.getCategories().stream()
                .filter(c -> c.getCategoryId().equals(parentCategory.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(parentInReport.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(parentInReport.getTransactionCount()).isEqualTo(2);
        assertThat(parentInReport.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void getCategoryReport_withMultipleCategories_shouldCalculatePercentages() {
        var category1 = createCategory(1L, "Food", null);
        var category2 = createCategory(2L, "Transport", null);

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-400), null, category1, null),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-100), null, category2, null)
        );

        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(transactions);

        var report = reportService.getCategoryReport(accountId, startDate, endDate);

        assertThat(report.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(report.getCategories()).hasSize(2);

        var foodCategory = report.getCategories().get(0);
        assertThat(foodCategory.getCategoryName()).isEqualTo("Food");
        assertThat(foodCategory.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(80.00));

        var transportCategory = report.getCategories().get(1);
        assertThat(transportCategory.getCategoryName()).isEqualTo("Transport");
        assertThat(transportCategory.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
    }

    @Test
    void getUserReport_withMultipleUsers_shouldCalculateCorrectly() {
        var user1 = createUser(1L, "John");
        var user2 = createUser(2L, "Jane");

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-300), user1, null, null),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-200), user2, null, null),
                createTransaction(3L, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(-100), user1, null, null)
        );

        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(transactions);

        var report = reportService.getUserReport(accountId, startDate, endDate);

        assertThat(report.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(report.getUsers()).hasSize(2);

        var johnReport = report.getUsers().get(0);
        assertThat(johnReport.getUserName()).isEqualTo("John");
        assertThat(johnReport.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(johnReport.getTransactionCount()).isEqualTo(2);
        assertThat(johnReport.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
    }

    @Test
    void getCreditCardReport_withMultipleCards_shouldCalculateCorrectly() {
        var card1 = createCreditCard(1L, "Visa");
        var card2 = createCreditCard(2L, "Mastercard");

        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(-500), null, null, card1),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-300), null, null, card1),
                createTransaction(3L, LocalDate.of(2025, 1, 20), BigDecimal.valueOf(-200), null, null, card2)
        );

        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(transactions);

        var report = reportService.getCreditCardReport(accountId, startDate, endDate);

        assertThat(report.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(report.getCreditCards()).hasSize(2);

        var visaReport = report.getCreditCards().get(0);
        assertThat(visaReport.getCreditCardNickname()).isEqualTo("Visa");
        assertThat(visaReport.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(visaReport.getTransactionCount()).isEqualTo(2);
        assertThat(visaReport.getPercentage()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
        assertThat(visaReport.getAverageTransactionAmount()).isEqualByComparingTo(BigDecimal.valueOf(400.00));
    }

    @Test
    void getMonthComparison_withMultipleMonths_shouldCalculateSummary() {
        var transactions = List.of(
                createTransaction(1L, LocalDate.of(2025, 1, 10), BigDecimal.valueOf(1000), null, null, null),
                createTransaction(2L, LocalDate.of(2025, 1, 15), BigDecimal.valueOf(-400), null, null, null),
                createTransaction(3L, LocalDate.of(2025, 2, 10), BigDecimal.valueOf(1200), null, null, null),
                createTransaction(4L, LocalDate.of(2025, 2, 15), BigDecimal.valueOf(-600), null, null, null),
                createTransaction(5L, LocalDate.of(2025, 3, 10), BigDecimal.valueOf(1100), null, null, null),
                createTransaction(6L, LocalDate.of(2025, 3, 15), BigDecimal.valueOf(-500), null, null, null)
        );

        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(transactions);

        var report = reportService.getMonthComparison(accountId, startDate, endDate);

        assertThat(report.getMonths()).hasSize(3);

        var jan = report.getMonths().get(0);
        assertThat(jan.getMonth()).isEqualTo(1);
        assertThat(jan.getIncome()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(jan.getExpense()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(jan.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));

        var summary = report.getSummary();
        assertThat(summary.getTotalIncome()).isEqualByComparingTo(BigDecimal.valueOf(3300));
        assertThat(summary.getTotalExpense()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(summary.getAverageMonthlyIncome()).isEqualByComparingTo(BigDecimal.valueOf(1100.00));
        assertThat(summary.getAverageMonthlyExpense()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
        assertThat(summary.getHighestExpenseMonth()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(summary.getLowestExpenseMonth()).isEqualByComparingTo(BigDecimal.valueOf(400));
    }

    @Test
    void getCategoryReport_withNoTransactions_shouldReturnEmpty() {
        when(transactionRepository.search(any(), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        var report = reportService.getCategoryReport(accountId, startDate, endDate);

        assertThat(report.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getCategories()).isEmpty();
    }

    private Category createCategory(Long id, String name, Category parent) {
        var category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentCategory(parent);
        return category;
    }

    private User createUser(Long id, String name) {
        var user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private CreditCard createCreditCard(Long id, String nickname) {
        var creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setNickname(nickname);
        return creditCard;
    }

    private Transaction createTransaction(Long id, LocalDate date, BigDecimal value,
                                          User responsible, Category category, CreditCard creditCard) {
        var transaction = new Transaction();
        transaction.setId(id);
        transaction.setDate(date);
        transaction.setValue(value);
        transaction.setDescription("Test");
        if (responsible != null) {
            transaction.setResponsibleUsers(java.util.Set.of(responsible));
        }
        transaction.setCategory(category);
        transaction.setCreditCard(creditCard);
        return transaction;
    }
}
