package com.relyon.credflow.repository;

import com.relyon.credflow.model.budget.BudgetPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BudgetPreferencesRepository extends JpaRepository<BudgetPreferences, Long> {

    Optional<BudgetPreferences> findByAccountIdAndUserId(Long accountId, Long userId);

    Optional<BudgetPreferences> findByAccountIdAndUserIsNull(Long accountId);

    @Query("""
            select bp from BudgetPreferences bp
             where bp.account.id = :accountId
               and (:userId is null and bp.user is null or bp.user.id = :userId)
            """)
    Optional<BudgetPreferences> findByAccountAndUser(Long accountId, Long userId);

    Optional<BudgetPreferences> findByIdAndAccountId(Long id, Long accountId);
}
