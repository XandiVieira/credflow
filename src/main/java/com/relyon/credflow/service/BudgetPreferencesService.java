package com.relyon.credflow.service;

import com.relyon.credflow.constant.BusinessConstants;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.budget.BudgetPreferences;
import com.relyon.credflow.model.budget.BudgetPreferencesResponseDTO;
import com.relyon.credflow.model.mapper.BudgetMapper;
import com.relyon.credflow.repository.BudgetPreferencesRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetPreferencesService {

    private final BudgetPreferencesRepository budgetPreferencesRepository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final BudgetMapper budgetMapper;

    @Transactional
    public BudgetPreferences createOrUpdate(BudgetPreferences preferences, Long accountId) {
        log.info("Creating/updating budget preferences for account {}, user {}",
                accountId, preferences.getUser() != null ? preferences.getUser().getId() : null);

        if (preferences.getUser() != null && preferences.getUser().getId() != null) {
            var user = userRepository.findByIdAndAccountId(preferences.getUser().getId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFound"));
            preferences.setUser(user);
        }

        var existing = budgetPreferencesRepository.findByAccountAndUser(
                accountId,
                preferences.getUser() != null ? preferences.getUser().getId() : null
        );

        if (existing.isPresent()) {
            var entity = existing.get();
            entity.setEnableAlerts(preferences.getEnableAlerts());
            entity.setEnableRollover(preferences.getEnableRollover());
            entity.setRolloverMaxMonths(preferences.getRolloverMaxMonths());
            entity.setRolloverMaxPercentage(preferences.getRolloverMaxPercentage());
            entity.setYellowWarningThreshold(preferences.getYellowWarningThreshold());
            entity.setOrangeWarningThreshold(preferences.getOrangeWarningThreshold());
            entity.setRedWarningThreshold(preferences.getRedWarningThreshold());
            entity.setEnableProjectedWarnings(preferences.getEnableProjectedWarnings());
            entity.setProjectedWarningMinDays(preferences.getProjectedWarningMinDays());
            return budgetPreferencesRepository.save(entity);
        }

        preferences.setAccount(accountService.findById(accountId));
        return budgetPreferencesRepository.save(preferences);
    }

    @Transactional(readOnly = true)
    public BudgetPreferencesResponseDTO getPreferencesForAccount(Long accountId, Long userId) {
        var userPrefs = userId != null
                ? budgetPreferencesRepository.findByAccountIdAndUserId(accountId, userId)
                : budgetPreferencesRepository.findByAccountIdAndUserIsNull(accountId);

        if (userPrefs.isPresent()) {
            return budgetMapper.toDto(userPrefs.get());
        }

        var accountPrefs = budgetPreferencesRepository.findByAccountIdAndUserIsNull(accountId);
        if (accountPrefs.isPresent()) {
            return budgetMapper.toDto(accountPrefs.get());
        }

        return createDefaultPreferences();
    }

    @Transactional(readOnly = true)
    public BudgetPreferences findById(Long id, Long accountId) {
        return budgetPreferencesRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.budgetPreferences.notFound"));
    }

    @Transactional
    public void delete(Long id, Long accountId) {
        log.info("Deleting budget preferences {} for account {}", id, accountId);
        var preferences = findById(id, accountId);
        budgetPreferencesRepository.delete(preferences);
    }

    private BudgetPreferencesResponseDTO createDefaultPreferences() {
        return BudgetPreferencesResponseDTO.builder()
                .enableAlerts(true)
                .enableRollover(false)
                .rolloverMaxMonths(BusinessConstants.Budget.ROLLOVER_MAX_MONTHS_DEFAULT)
                .rolloverMaxPercentage(BusinessConstants.Budget.ROLLOVER_MAX_PERCENTAGE_DEFAULT)
                .yellowWarningThreshold(BusinessConstants.Budget.YELLOW_WARNING_THRESHOLD_DEFAULT)
                .orangeWarningThreshold(BusinessConstants.Budget.ORANGE_WARNING_THRESHOLD_DEFAULT)
                .redWarningThreshold(BusinessConstants.Budget.RED_WARNING_THRESHOLD_DEFAULT)
                .enableProjectedWarnings(true)
                .projectedWarningMinDays(BusinessConstants.Budget.PROJECTED_WARNING_MIN_DAYS_DEFAULT)
                .build();
    }
}
