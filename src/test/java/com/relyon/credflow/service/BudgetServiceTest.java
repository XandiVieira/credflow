package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.model.budget.BudgetType;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.BudgetRepository;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private BudgetService budgetService;

    private Long accountId;
    private Account account;
    private Category category;
    private User user;

    @BeforeEach
    void setUp() {
        accountId = 1L;
        account = new Account();
        account.setId(accountId);

        category = new Category();
        category.setId(10L);
        category.setName("Food");

        user = new User();
        user.setId(20L);
        user.setName("John");
    }

    @Test
    void create_accountWideBudget_shouldSucceed() {
        var budget = Budget.builder()
                .period(YearMonth.of(2025, 1))
                .amount(BigDecimal.valueOf(1000))
                .type(BudgetType.ACCOUNT_WIDE)
                .allowRollover(true)
                .build();

        when(accountService.findById(accountId)).thenReturn(account);
        when(budgetRepository.findMatchingBudget(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        var result = budgetService.create(budget, accountId);

        assertThat(result.getAccount()).isEqualTo(account);
        assertThat(result.getRolledOverAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(budgetRepository).save(budget);
    }

    @Test
    void create_categorySpecificBudget_shouldSucceed() {
        var budget = Budget.builder()
                .period(YearMonth.of(2025, 1))
                .amount(BigDecimal.valueOf(500))
                .type(BudgetType.CATEGORY_SPECIFIC)
                .category(category)
                .build();

        when(categoryRepository.findByIdAndAccountId(category.getId(), accountId)).thenReturn(Optional.of(category));
        when(accountService.findById(accountId)).thenReturn(account);
        when(budgetRepository.findMatchingBudget(any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        var result = budgetService.create(budget, accountId);

        assertThat(result.getCategory()).isEqualTo(category);
        verify(categoryRepository).findByIdAndAccountId(category.getId(), accountId);
    }

    @Test
    void create_duplicateBudget_shouldThrowException() {
        var budget = Budget.builder()
                .period(YearMonth.of(2025, 1))
                .amount(BigDecimal.valueOf(1000))
                .type(BudgetType.ACCOUNT_WIDE)
                .build();

        var existing = Budget.builder().id(1L).build();

        when(budgetRepository.findMatchingBudget(any(), any(), any(), any(), any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> budgetService.create(budget, accountId))
                .isInstanceOf(com.relyon.credflow.exception.ResourceAlreadyExistsException.class);
    }

    @Test
    void create_accountWideWithCategory_shouldThrowException() {
        var budget = Budget.builder()
                .period(YearMonth.of(2025, 1))
                .amount(BigDecimal.valueOf(1000))
                .type(BudgetType.ACCOUNT_WIDE)
                .category(category)
                .build();

        assertThatThrownBy(() -> budgetService.create(budget, accountId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_existingBudget_shouldSucceed() {
        var existing = Budget.builder()
                .id(1L)
                .period(YearMonth.of(2025, 1))
                .amount(BigDecimal.valueOf(1000))
                .type(BudgetType.ACCOUNT_WIDE)
                .account(account)
                .build();

        var updates = Budget.builder()
                .amount(BigDecimal.valueOf(1500))
                .type(BudgetType.ACCOUNT_WIDE)
                .allowRollover(true)
                .build();

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        var result = budgetService.update(1L, updates, accountId);

        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(result.getAllowRollover()).isTrue();
    }

    @Test
    void findById_existingBudget_shouldReturnBudget() {
        var budget = Budget.builder().id(1L).build();

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));

        var result = budgetService.findById(1L, accountId);

        assertThat(result).isEqualTo(budget);
    }

    @Test
    void findById_nonExistingBudget_shouldThrowException() {
        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.findById(1L, accountId))
                .isInstanceOf(com.relyon.credflow.exception.ResourceNotFoundException.class);
    }

    @Test
    void delete_existingBudget_shouldSucceed() {
        var budget = Budget.builder().id(1L).build();

        when(budgetRepository.findByIdAndAccountId(1L, accountId)).thenReturn(Optional.of(budget));

        budgetService.delete(1L, accountId);

        verify(budgetRepository).delete(budget);
    }
}
