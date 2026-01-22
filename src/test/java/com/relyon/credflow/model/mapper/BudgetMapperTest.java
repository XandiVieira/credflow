package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.model.budget.BudgetType;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BudgetMapperTest {

    private BudgetMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(BudgetMapper.class);
    }

    @Test
    void idToCategory_withId_returnsCategoryWithId() {
        var result = mapper.idToCategory(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void idToCategory_withNullId_returnsNull() {
        var result = mapper.idToCategory(null);

        assertNull(result);
    }

    @Test
    void idToUser_withId_returnsUserWithId() {
        var result = mapper.idToUser(25L);

        assertNotNull(result);
        assertEquals(25L, result.getId());
    }

    @Test
    void idToUser_withNullId_returnsNull() {
        var result = mapper.idToUser(null);

        assertNull(result);
    }

    @Test
    void toDto_withBudget_mapsAllFields() {
        var account = new Account();
        account.setId(1L);

        var category = new Category();
        category.setId(10L);
        category.setName("Food");

        var user = new User();
        user.setId(5L);
        user.setName("Test User");

        var budget = Budget.builder()
                .id(100L)
                .period(YearMonth.of(2024, 6))
                .amount(BigDecimal.valueOf(500))
                .type(BudgetType.CATEGORY_SPECIFIC)
                .category(category)
                .user(user)
                .allowRollover(true)
                .rolledOverAmount(BigDecimal.valueOf(50))
                .account(account)
                .build();

        var result = mapper.toDto(budget);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(1L, result.getAccountId());
        assertEquals(10L, result.getCategoryId());
        assertEquals("Food", result.getCategoryName());
        assertEquals(5L, result.getUserId());
        assertEquals("Test User", result.getUserName());
        assertEquals(YearMonth.of(2024, 6), result.getPeriod());
        assertEquals(BigDecimal.valueOf(500), result.getAmount());
        assertEquals(BudgetType.CATEGORY_SPECIFIC, result.getType());
        assertTrue(result.getAllowRollover());
        assertEquals(BigDecimal.valueOf(50), result.getRolledOverAmount());
    }

    @Test
    void toDto_effectiveBudget_calculatesAmountPlusRolledOver() {
        var account = new Account();
        account.setId(1L);

        var budget = Budget.builder()
                .amount(BigDecimal.valueOf(1000))
                .rolledOverAmount(BigDecimal.valueOf(200))
                .account(account)
                .period(YearMonth.now())
                .type(BudgetType.ACCOUNT_WIDE)
                .build();

        var result = mapper.toDto(budget);

        assertEquals(BigDecimal.valueOf(1200), result.getEffectiveBudget());
    }

    @Test
    void toDto_effectiveBudget_withNullRolledOver_usesZero() {
        var account = new Account();
        account.setId(1L);

        var budget = Budget.builder()
                .amount(BigDecimal.valueOf(1000))
                .rolledOverAmount(null)
                .account(account)
                .period(YearMonth.now())
                .type(BudgetType.ACCOUNT_WIDE)
                .build();

        var result = mapper.toDto(budget);

        assertEquals(BigDecimal.valueOf(1000), result.getEffectiveBudget());
    }

    @Test
    void toDto_withNullCategory_mapsNullCategoryFields() {
        var account = new Account();
        account.setId(1L);

        var budget = Budget.builder()
                .amount(BigDecimal.valueOf(500))
                .category(null)
                .account(account)
                .period(YearMonth.now())
                .type(BudgetType.USER_SPECIFIC)
                .build();

        var result = mapper.toDto(budget);

        assertNull(result.getCategoryId());
        assertNull(result.getCategoryName());
    }

    @Test
    void toDto_withNullUser_mapsNullUserFields() {
        var account = new Account();
        account.setId(1L);

        var budget = Budget.builder()
                .amount(BigDecimal.valueOf(500))
                .user(null)
                .account(account)
                .period(YearMonth.now())
                .type(BudgetType.CATEGORY_SPECIFIC)
                .build();

        var result = mapper.toDto(budget);

        assertNull(result.getUserId());
        assertNull(result.getUserName());
    }
}
