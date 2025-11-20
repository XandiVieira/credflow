package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.budget.Budget;
import com.relyon.credflow.repository.BudgetRepository;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;

    @Transactional
    public Budget create(Budget budget, Long accountId) {
        log.info("Creating budget for account {}, period {}, type {}", accountId, budget.getPeriod(), budget.getType());

        validateBudgetType(budget);

        if (budget.getCategory() != null && budget.getCategory().getId() != null) {
            var category = categoryRepository.findByIdAndAccountId(budget.getCategory().getId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound"));
            budget.setCategory(category);
        }

        if (budget.getUser() != null && budget.getUser().getId() != null) {
            var user = userRepository.findByIdAndAccountId(budget.getUser().getId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFound"));
            budget.setUser(user);
        }

        var existing = budgetRepository.findMatchingBudget(
                accountId,
                budget.getPeriod(),
                budget.getType(),
                budget.getCategory() != null ? budget.getCategory().getId() : null,
                budget.getUser() != null ? budget.getUser().getId() : null
        );

        if (existing.isPresent()) {
            throw new ResourceAlreadyExistsException("budget.alreadyExists");
        }

        budget.setAccount(accountService.findById(accountId));
        budget.setRolledOverAmount(BigDecimal.ZERO);

        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget update(Long id, Budget budget, Long accountId) {
        log.info("Updating budget {} for account {}", id, accountId);

        var existing = budgetRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.budget.notFound"));

        validateBudgetType(budget);

        if (budget.getCategory() != null && budget.getCategory().getId() != null) {
            var category = categoryRepository.findByIdAndAccountId(budget.getCategory().getId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound"));
            existing.setCategory(category);
        } else {
            existing.setCategory(null);
        }

        if (budget.getUser() != null && budget.getUser().getId() != null) {
            var user = userRepository.findByIdAndAccountId(budget.getUser().getId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFound"));
            existing.setUser(user);
        } else {
            existing.setUser(null);
        }

        existing.setAmount(budget.getAmount());
        existing.setType(budget.getType());
        existing.setAllowRollover(budget.getAllowRollover());

        return budgetRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Budget findById(Long id, Long accountId) {
        return budgetRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.budget.notFound"));
    }

    @Transactional(readOnly = true)
    public List<Budget> findByPeriod(YearMonth period, Long accountId) {
        return budgetRepository.findByAccountIdAndPeriod(accountId, period);
    }

    @Transactional
    public void delete(Long id, Long accountId) {
        log.info("Deleting budget {} for account {}", id, accountId);
        var budget = findById(id, accountId);
        budgetRepository.delete(budget);
    }

    private void validateBudgetType(Budget budget) {
        switch (budget.getType()) {
            case ACCOUNT_WIDE:
                if (budget.getCategory() != null || budget.getUser() != null) {
                    throw new IllegalArgumentException("budget.accountWideMustNotHaveCategoryOrUser");
                }
                break;
            case CATEGORY_SPECIFIC:
                if (budget.getCategory() == null || budget.getUser() != null) {
                    throw new IllegalArgumentException("budget.categorySpecificMustHaveCategory");
                }
                break;
            case USER_SPECIFIC:
                if (budget.getUser() == null || budget.getCategory() != null) {
                    throw new IllegalArgumentException("budget.userSpecificMustHaveUser");
                }
                break;
            case CATEGORY_USER_SPECIFIC:
                if (budget.getCategory() == null || budget.getUser() == null) {
                    throw new IllegalArgumentException("budget.categoryUserSpecificMustHaveBoth");
                }
                break;
        }
    }
}
