package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserPreferencesMapperTest {

    private UserPreferencesMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserPreferencesMapper.class);
    }

    @Test
    void toEntity_withDto_mapsFields() {
        var dto = UserPreferencesDTO.builder()
                .defaultCurrency("USD")
                .language(Language.EN)
                .theme(Theme.DARK)
                .emailNotificationsEnabled(true)
                .notificationsEnabled(true)
                .budgetAlertsEnabled(true)
                .build();

        var result = mapper.toEntity(dto);

        assertNotNull(result);
        assertEquals("USD", result.getDefaultCurrency());
        assertEquals(Language.EN, result.getLanguage());
        assertEquals(Theme.DARK, result.getTheme());
        assertTrue(result.getEmailNotificationsEnabled());
        assertTrue(result.getNotificationsEnabled());
        assertTrue(result.getBudgetAlertsEnabled());
    }

    @Test
    void toEntity_ignoresIdAndTimestamps() {
        var dto = UserPreferencesDTO.builder()
                .defaultCurrency("BRL")
                .language(Language.PT)
                .theme(Theme.LIGHT)
                .notificationsEnabled(false)
                .emailNotificationsEnabled(false)
                .build();

        var result = mapper.toEntity(dto);

        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
        assertNull(result.getDeletedAt());
        assertNull(result.getUser());
    }

    @Test
    void toDto_withEntity_mapsAllFields() {
        var preferences = new UserPreferences();
        preferences.setId(1L);
        preferences.setDefaultCurrency("EUR");
        preferences.setLanguage(Language.EN);
        preferences.setTheme(Theme.DARK);
        preferences.setEmailNotificationsEnabled(true);
        preferences.setNotificationsEnabled(false);
        preferences.setBudgetAlertsEnabled(true);
        preferences.setBillRemindersEnabled(true);
        preferences.setWeeklySummaryEnabled(false);

        var result = mapper.toDto(preferences);

        assertNotNull(result);
        assertEquals("EUR", result.getDefaultCurrency());
        assertEquals(Language.EN, result.getLanguage());
        assertEquals(Theme.DARK, result.getTheme());
        assertTrue(result.getEmailNotificationsEnabled());
        assertFalse(result.getNotificationsEnabled());
        assertTrue(result.getBudgetAlertsEnabled());
    }

    @Test
    void updateEntityFromDto_updatesExistingEntity() {
        var entity = new UserPreferences();
        entity.setId(1L);
        entity.setDefaultCurrency("USD");
        entity.setLanguage(Language.EN);
        entity.setTheme(Theme.LIGHT);
        entity.setEmailNotificationsEnabled(true);
        entity.setNotificationsEnabled(true);
        entity.setBudgetAlertsEnabled(true);
        entity.setBillRemindersEnabled(true);
        entity.setWeeklySummaryEnabled(true);

        var user = new User();
        user.setId(10L);
        entity.setUser(user);

        var dto = UserPreferencesDTO.builder()
                .defaultCurrency("BRL")
                .language(Language.PT)
                .theme(Theme.DARK)
                .emailNotificationsEnabled(false)
                .notificationsEnabled(false)
                .budgetAlertsEnabled(false)
                .billRemindersEnabled(false)
                .weeklySummaryEnabled(false)
                .build();

        mapper.updateEntityFromDto(dto, entity);

        assertEquals(1L, entity.getId());
        assertEquals("BRL", entity.getDefaultCurrency());
        assertEquals(Language.PT, entity.getLanguage());
        assertEquals(Theme.DARK, entity.getTheme());
        assertFalse(entity.getEmailNotificationsEnabled());
        assertFalse(entity.getNotificationsEnabled());
        assertFalse(entity.getBudgetAlertsEnabled());
        assertEquals(user, entity.getUser());
    }

    @Test
    void updateEntityFromDto_preservesIdAndUser() {
        var user = new User();
        user.setId(5L);

        var entity = new UserPreferences();
        entity.setId(100L);
        entity.setUser(user);
        entity.setDefaultCurrency("USD");

        var dto = UserPreferencesDTO.builder()
                .defaultCurrency("EUR")
                .language(Language.EN)
                .theme(Theme.DARK)
                .notificationsEnabled(true)
                .emailNotificationsEnabled(true)
                .budgetAlertsEnabled(true)
                .billRemindersEnabled(true)
                .weeklySummaryEnabled(true)
                .build();

        mapper.updateEntityFromDto(dto, entity);

        assertEquals(100L, entity.getId());
        assertEquals(user, entity.getUser());
        assertEquals("EUR", entity.getDefaultCurrency());
    }

    @Test
    void toDto_withNullValues_handlesNulls() {
        var preferences = new UserPreferences();
        preferences.setDefaultCurrency(null);
        preferences.setLanguage(null);
        preferences.setTheme(null);
        preferences.setEmailNotificationsEnabled(null);
        preferences.setNotificationsEnabled(null);
        preferences.setBudgetAlertsEnabled(null);
        preferences.setBillRemindersEnabled(null);
        preferences.setWeeklySummaryEnabled(null);

        var result = mapper.toDto(preferences);

        assertNotNull(result);
        assertNull(result.getDefaultCurrency());
        assertNull(result.getLanguage());
        assertNull(result.getTheme());
        assertNull(result.getEmailNotificationsEnabled());
        assertNull(result.getNotificationsEnabled());
        assertNull(result.getBudgetAlertsEnabled());
    }
}
