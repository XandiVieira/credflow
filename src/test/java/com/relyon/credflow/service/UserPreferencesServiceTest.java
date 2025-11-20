package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.mapper.UserPreferencesMapper;
import com.relyon.credflow.model.user.Theme;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserPreferences;
import com.relyon.credflow.model.user.UserPreferencesDTO;
import com.relyon.credflow.repository.UserPreferencesRepository;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesMapper userPreferencesMapper;

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private UserPreferencesService userPreferencesService;

    @Test
    void getPreferences_whenPreferencesExist_shouldReturnPreferences() {
        var userId = 1L;
        var accountId = 10L;
        var preferences = UserPreferences.builder().id(1L).build();
        var dto = new UserPreferencesDTO();

        when(userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId))
                .thenReturn(Optional.of(preferences));
        when(userPreferencesMapper.toDto(preferences)).thenReturn(dto);

        var result = userPreferencesService.getPreferences(userId, accountId);

        assertThat(result).isEqualTo(dto);
        verify(userPreferencesRepository).findByUserIdAndUserAccountId(userId, accountId);
        verify(userPreferencesMapper).toDto(preferences);
    }

    @Test
    void getPreferences_whenPreferencesDoNotExist_shouldCreateDefault() {
        var userId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();
        var user = User.builder().id(userId).account(account).build();
        var defaultPreferences = UserPreferences.builder()
                .id(1L)
                .user(user)
                .build();
        var dto = new UserPreferencesDTO();

        when(userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId))
                .thenReturn(Optional.empty());
        when(userRepository.findByIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(user));
        when(userPreferencesRepository.save(any(UserPreferences.class)))
                .thenReturn(defaultPreferences);
        when(userPreferencesMapper.toDto(defaultPreferences)).thenReturn(dto);

        var result = userPreferencesService.getPreferences(userId, accountId);

        assertThat(result).isEqualTo(dto);
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_whenPreferencesExist_shouldUpdate() {
        var userId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();
        var user = User.builder().id(userId).account(account).build();

        var existingPreferences = UserPreferences.builder()
                .id(1L)
                .user(user)
                .theme(Theme.LIGHT)
                .build();

        var dto = new UserPreferencesDTO();
        dto.setTheme(Theme.DARK);

        when(userRepository.findByIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingPreferences));
        when(userPreferencesRepository.save(existingPreferences))
                .thenReturn(existingPreferences);
        when(userPreferencesMapper.toDto(existingPreferences)).thenReturn(dto);

        var result = userPreferencesService.updatePreferences(userId, accountId, dto);

        assertThat(result).isEqualTo(dto);
        verify(userPreferencesMapper).updateEntityFromDto(dto, existingPreferences);
        verify(userPreferencesRepository).save(existingPreferences);
    }

    @Test
    void updatePreferences_whenUserNotFound_shouldThrowException() {
        var userId = 999L;
        var accountId = 10L;
        var dto = new UserPreferencesDTO();

        when(userRepository.findByIdAndAccountId(userId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.updatePreferences(userId, accountId, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByIdAndAccountId(userId, accountId);
        verifyNoInteractions(userPreferencesRepository);
    }

    @Test
    void updatePreferences_whenPreferencesDoNotExist_shouldCreateAndUpdate() {
        var userId = 1L;
        var accountId = 10L;
        var account = Account.builder().id(accountId).build();
        var user = User.builder().id(userId).account(account).build();

        var newPreferences = UserPreferences.builder()
                .user(user)
                .build();

        var dto = new UserPreferencesDTO();
        dto.setTheme(Theme.DARK);

        when(userRepository.findByIdAndAccountId(userId, accountId))
                .thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.empty());
        when(userPreferencesRepository.save(any(UserPreferences.class)))
                .thenReturn(newPreferences);
        when(userPreferencesMapper.toDto(newPreferences)).thenReturn(dto);

        var result = userPreferencesService.updatePreferences(userId, accountId, dto);

        assertThat(result).isEqualTo(dto);
        verify(userPreferencesRepository, times(2)).save(any(UserPreferences.class));
    }

    @Test
    void deletePreferences_whenPreferencesExist_shouldDelete() {
        var userId = 1L;
        var accountId = 10L;
        var preferences = UserPreferences.builder().id(5L).build();

        when(userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId))
                .thenReturn(Optional.of(preferences));

        userPreferencesService.deletePreferences(userId, accountId);

        verify(userPreferencesRepository).deleteById(5L);
    }

    @Test
    void deletePreferences_whenPreferencesDoNotExist_shouldThrowException() {
        var userId = 1L;
        var accountId = 10L;

        when(userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userPreferencesService.deletePreferences(userId, accountId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userPreferencesRepository).findByUserIdAndUserAccountId(userId, accountId);
        verify(userPreferencesRepository, never()).deleteById(any());
    }
}
