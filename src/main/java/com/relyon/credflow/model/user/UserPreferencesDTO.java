package com.relyon.credflow.model.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesDTO {

    private Long id;

    @Size(max = 500, message = "{userPreferences.profilePictureUrl.size}")
    private String profilePictureUrl;

    private Theme theme;

    private Language language;

    private Boolean notificationsEnabled;

    private Boolean emailNotificationsEnabled;

    private Boolean budgetAlertsEnabled;

    private Boolean billRemindersEnabled;

    private Boolean weeklySummaryEnabled;

    @Pattern(regexp = "^[A-Z]{3}$", message = "{userPreferences.currency.pattern}")
    private String defaultCurrency;
}
