package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.mapper.UserPreferencesMapper;
import com.relyon.credflow.model.user.UserPreferences;
import com.relyon.credflow.model.user.UserPreferencesDTO;
import com.relyon.credflow.repository.UserPreferencesRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;
    private final UserPreferencesMapper userPreferencesMapper;
    private final LocalizedMessageTranslationService translationService;

    @Transactional(readOnly = true)
    public UserPreferencesDTO getPreferences(Long userId, Long accountId) {
        log.info("Fetching preferences for user {} in account {}", userId, accountId);

        var preferences = userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId)
                .orElseGet(() -> createDefaultPreferences(userId, accountId));

        return userPreferencesMapper.toDto(preferences);
    }

    @Transactional
    public UserPreferencesDTO updatePreferences(Long userId, Long accountId, UserPreferencesDTO dto) {
        log.info("Updating preferences for user {} in account {}", userId, accountId);

        var user = userRepository.findByIdAndAccountId(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFound", userId));

        var preferences = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    var newPrefs = UserPreferences.builder()
                            .user(user)
                            .build();
                    return userPreferencesRepository.save(newPrefs);
                });

        userPreferencesMapper.updateEntityFromDto(dto, preferences);
        var saved = userPreferencesRepository.save(preferences);

        log.info("Preferences updated for user {}", userId);
        return userPreferencesMapper.toDto(saved);
    }

    @Transactional
    public void deletePreferences(Long userId, Long accountId) {
        log.info("Deleting preferences for user {} in account {}", userId, accountId);

        var preferences = userPreferencesRepository.findByUserIdAndUserAccountId(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.userPreferences.notFound"));

        userPreferencesRepository.deleteById(preferences.getId());
        log.info("Preferences deleted for user {}", userId);
    }

    private UserPreferences createDefaultPreferences(Long userId, Long accountId) {
        log.info("Creating default preferences for user {} in account {}", userId, accountId);

        var user = userRepository.findByIdAndAccountId(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFound", userId));

        var preferences = UserPreferences.builder()
                .user(user)
                .build();

        return userPreferencesRepository.save(preferences);
    }
}
