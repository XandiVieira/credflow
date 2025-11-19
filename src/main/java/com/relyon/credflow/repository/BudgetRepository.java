package com.relyon.credflow.repository;

import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.model.budget.BudgetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByAccountIdAndPeriod(Long accountId, YearMonth period);

    Optional<Budget> findByIdAndAccountId(Long id, Long accountId);

    Optional<Budget> findByAccountIdAndPeriodAndTypeAndCategoryIdAndUserId(
            Long accountId, YearMonth period, BudgetType type, Long categoryId, Long userId);

    @Query("""
            select b from Budget b
             where b.account.id = :accountId
               and b.period = :period
               and b.type = :type
               and (:categoryId is null and b.category is null or b.category.id = :categoryId)
               and (:userId is null and b.user is null or b.user.id = :userId)
            """)
    Optional<Budget> findMatchingBudget(Long accountId, YearMonth period, BudgetType type,
                                        Long categoryId, Long userId);

    List<Budget> findByAccountIdAndAllowRolloverTrue(Long accountId);

    List<Budget> findByAccountIdAndPeriodAndCategoryId(Long accountId, YearMonth period, Long categoryId);

    List<Budget> findByAccountIdAndPeriodAndUserId(Long accountId, YearMonth period, Long userId);
}
