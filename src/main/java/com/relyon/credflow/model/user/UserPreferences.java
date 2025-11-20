package com.relyon.credflow.model.user;

import com.relyon.credflow.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserPreferences extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Theme theme = Theme.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Language language = Language.EN;

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationsEnabled = true;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "budget_alerts_enabled", nullable = false)
    @Builder.Default
    private Boolean budgetAlertsEnabled = true;

    @Column(name = "bill_reminders_enabled", nullable = false)
    @Builder.Default
    private Boolean billRemindersEnabled = true;

    @Column(name = "weekly_summary_enabled", nullable = false)
    @Builder.Default
    private Boolean weeklySummaryEnabled = false;

    @Column(name = "default_currency", length = 3)
    @Builder.Default
    private String defaultCurrency = "BRL";
}
