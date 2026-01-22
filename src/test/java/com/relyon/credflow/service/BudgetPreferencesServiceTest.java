package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relyon.credflow.constant.BusinessConstants;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.budget.BudgetPreferences;
import com.relyon.credflow.model.budget.BudgetPreferencesResponseDTO;
import com.relyon.credflow.model.mapper.BudgetMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.BudgetPreferencesRepository;
import com.relyon.credflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetPreferencesServiceTest {

    @Mock
    private BudgetPreferencesRepository budgetPreferencesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetPreferencesService budgetPreferencesService;

    private Account account;
    private User user;
    private BudgetPreferences preferences;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setName("Test Account");

        user = new User();
        user.setId(10L);
        user.setName("Test User");
        user.setAccount(account);

        preferences = new BudgetPreferences();
        preferences.setId(100L);
        preferences.setAccount(account);
        preferences.setEnableAlerts(true);
        preferences.setEnableRollover(false);
        preferences.setRolloverMaxMonths(2);
        preferences.setRolloverMaxPercentage(50);
        preferences.setYellowWarningThreshold(80);
        preferences.setOrangeWarningThreshold(100);
        preferences.setRedWarningThreshold(120);
        preferences.setEnableProjectedWarnings(true);
        preferences.setProjectedWarningMinDays(5);
    }

    @Nested
    class CreateOrUpdate {

        @Test
        void whenCreatingNewPreferencesWithoutUser_shouldSaveWithAccount() {
            var newPrefs = new BudgetPreferences();
            newPrefs.setEnableAlerts(false);
            newPrefs.setEnableRollover(true);

            when(budgetPreferencesRepository.findByAccountAndUser(1L, null)).thenReturn(Optional.empty());
            when(accountService.findById(1L)).thenReturn(account);
            when(budgetPreferencesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = budgetPreferencesService.createOrUpdate(newPrefs, 1L);

            assertThat(result.getAccount()).isEqualTo(account);
            verify(budgetPreferencesRepository).save(newPrefs);
        }

        @Test
        void whenCreatingNewPreferencesWithUser_shouldValidateUserBelongsToAccount() {
            var newPrefs = new BudgetPreferences();
            newPrefs.setUser(user);
            newPrefs.setEnableAlerts(true);

            when(userRepository.findByIdAndAccountId(10L, 1L)).thenReturn(Optional.of(user));
            when(budgetPreferencesRepository.findByAccountAndUser(1L, 10L)).thenReturn(Optional.empty());
            when(accountService.findById(1L)).thenReturn(account);
            when(budgetPreferencesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = budgetPreferencesService.createOrUpdate(newPrefs, 1L);

            assertThat(result.getUser()).isEqualTo(user);
            verify(userRepository).findByIdAndAccountId(10L, 1L);
        }

        @Test
        void whenUserNotInAccount_shouldThrowResourceNotFoundException() {
            var newPrefs = new BudgetPreferences();
            newPrefs.setUser(user);

            when(userRepository.findByIdAndAccountId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetPreferencesService.createOrUpdate(newPrefs, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("resource.user.notFound");
        }

        @Test
        void whenUpdatingExisting_shouldMergeFieldsAndSave() {
            var existingPrefs = new BudgetPreferences();
            existingPrefs.setId(50L);
            existingPrefs.setEnableAlerts(true);
            existingPrefs.setEnableRollover(false);
            existingPrefs.setRolloverMaxMonths(1);
            existingPrefs.setRolloverMaxPercentage(25);
            existingPrefs.setYellowWarningThreshold(70);
            existingPrefs.setOrangeWarningThreshold(90);
            existingPrefs.setRedWarningThreshold(110);
            existingPrefs.setEnableProjectedWarnings(false);
            existingPrefs.setProjectedWarningMinDays(3);

            var updatePrefs = new BudgetPreferences();
            updatePrefs.setEnableAlerts(false);
            updatePrefs.setEnableRollover(true);
            updatePrefs.setRolloverMaxMonths(3);
            updatePrefs.setRolloverMaxPercentage(75);
            updatePrefs.setYellowWarningThreshold(85);
            updatePrefs.setOrangeWarningThreshold(105);
            updatePrefs.setRedWarningThreshold(125);
            updatePrefs.setEnableProjectedWarnings(true);
            updatePrefs.setProjectedWarningMinDays(7);

            when(budgetPreferencesRepository.findByAccountAndUser(1L, null)).thenReturn(Optional.of(existingPrefs));
            when(budgetPreferencesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = budgetPreferencesService.createOrUpdate(updatePrefs, 1L);

            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.getEnableAlerts()).isFalse();
            assertThat(result.getEnableRollover()).isTrue();
            assertThat(result.getRolloverMaxMonths()).isEqualTo(3);
            assertThat(result.getRolloverMaxPercentage()).isEqualTo(75);
            assertThat(result.getYellowWarningThreshold()).isEqualTo(85);
            assertThat(result.getOrangeWarningThreshold()).isEqualTo(105);
            assertThat(result.getRedWarningThreshold()).isEqualTo(125);
            assertThat(result.getEnableProjectedWarnings()).isTrue();
            assertThat(result.getProjectedWarningMinDays()).isEqualTo(7);
        }
    }

    @Nested
    class GetPreferencesForAccount {

        @Test
        void whenUserPrefsExist_shouldReturnUserPrefs() {
            var dto = BudgetPreferencesResponseDTO.builder()
                    .id(100L)
                    .enableAlerts(true)
                    .build();

            when(budgetPreferencesRepository.findByAccountIdAndUserId(1L, 10L)).thenReturn(Optional.of(preferences));
            when(budgetMapper.toDto(preferences)).thenReturn(dto);

            var result = budgetPreferencesService.getPreferencesForAccount(1L, 10L);

            assertThat(result.getId()).isEqualTo(100L);
            verify(budgetPreferencesRepository).findByAccountIdAndUserId(1L, 10L);
        }

        @Test
        void whenUserPrefsNotExistButAccountPrefsExist_shouldReturnAccountPrefs() {
            var accountPrefs = new BudgetPreferences();
            accountPrefs.setId(200L);
            accountPrefs.setEnableAlerts(false);

            var dto = BudgetPreferencesResponseDTO.builder()
                    .id(200L)
                    .enableAlerts(false)
                    .build();

            when(budgetPreferencesRepository.findByAccountIdAndUserId(1L, 10L)).thenReturn(Optional.empty());
            when(budgetPreferencesRepository.findByAccountIdAndUserIsNull(1L)).thenReturn(Optional.of(accountPrefs));
            when(budgetMapper.toDto(accountPrefs)).thenReturn(dto);

            var result = budgetPreferencesService.getPreferencesForAccount(1L, 10L);

            assertThat(result.getId()).isEqualTo(200L);
        }

        @Test
        void whenNoPrefsExist_shouldReturnDefaultPreferences() {
            when(budgetPreferencesRepository.findByAccountIdAndUserId(1L, 10L)).thenReturn(Optional.empty());
            when(budgetPreferencesRepository.findByAccountIdAndUserIsNull(1L)).thenReturn(Optional.empty());

            var result = budgetPreferencesService.getPreferencesForAccount(1L, 10L);

            assertThat(result.getId()).isNull();
            assertThat(result.getEnableAlerts()).isTrue();
            assertThat(result.getEnableRollover()).isFalse();
            assertThat(result.getRolloverMaxMonths()).isEqualTo(BusinessConstants.Budget.ROLLOVER_MAX_MONTHS_DEFAULT);
            assertThat(result.getRolloverMaxPercentage()).isEqualTo(BusinessConstants.Budget.ROLLOVER_MAX_PERCENTAGE_DEFAULT);
            assertThat(result.getYellowWarningThreshold()).isEqualTo(BusinessConstants.Budget.YELLOW_WARNING_THRESHOLD_DEFAULT);
            assertThat(result.getOrangeWarningThreshold()).isEqualTo(BusinessConstants.Budget.ORANGE_WARNING_THRESHOLD_DEFAULT);
            assertThat(result.getRedWarningThreshold()).isEqualTo(BusinessConstants.Budget.RED_WARNING_THRESHOLD_DEFAULT);
            assertThat(result.getEnableProjectedWarnings()).isTrue();
            assertThat(result.getProjectedWarningMinDays()).isEqualTo(BusinessConstants.Budget.PROJECTED_WARNING_MIN_DAYS_DEFAULT);
        }

        @Test
        void whenUserIdIsNull_shouldQueryAccountLevelPrefs() {
            var accountPrefs = new BudgetPreferences();
            accountPrefs.setId(300L);

            var dto = BudgetPreferencesResponseDTO.builder().id(300L).build();

            when(budgetPreferencesRepository.findByAccountIdAndUserIsNull(1L)).thenReturn(Optional.of(accountPrefs));
            when(budgetMapper.toDto(accountPrefs)).thenReturn(dto);

            var result = budgetPreferencesService.getPreferencesForAccount(1L, null);

            assertThat(result.getId()).isEqualTo(300L);
            verify(budgetPreferencesRepository, never()).findByAccountIdAndUserId(anyLong(), anyLong());
        }
    }

    @Nested
    class FindById {

        @Test
        void whenExists_shouldReturnPreferences() {
            when(budgetPreferencesRepository.findByIdAndAccountId(100L, 1L)).thenReturn(Optional.of(preferences));

            var result = budgetPreferencesService.findById(100L, 1L);

            assertThat(result).isEqualTo(preferences);
        }

        @Test
        void whenNotExists_shouldThrowResourceNotFoundException() {
            when(budgetPreferencesRepository.findByIdAndAccountId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetPreferencesService.findById(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("resource.budgetPreferences.notFound");
        }
    }

    @Nested
    class Delete {

        @Test
        void whenExists_shouldDeletePreferences() {
            when(budgetPreferencesRepository.findByIdAndAccountId(100L, 1L)).thenReturn(Optional.of(preferences));

            budgetPreferencesService.delete(100L, 1L);

            verify(budgetPreferencesRepository).delete(preferences);
        }

        @Test
        void whenNotExists_shouldThrowResourceNotFoundException() {
            when(budgetPreferencesRepository.findByIdAndAccountId(999L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetPreferencesService.delete(999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(budgetPreferencesRepository, never()).delete(any());
        }
    }
}
