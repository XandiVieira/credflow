package com.relyon.credflow.model.budget;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "budget_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BudgetPreferences extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "enable_alerts", nullable = false)
    @Builder.Default
    private Boolean enableAlerts = true;

    @Column(name = "enable_rollover", nullable = false)
    @Builder.Default
    private Boolean enableRollover = false;

    @Column(name = "rollover_max_months")
    @Builder.Default
    private Integer rolloverMaxMonths = 2;

    @Column(name = "rollover_max_percentage")
    @Builder.Default
    private Integer rolloverMaxPercentage = 50;

    @Column(name = "yellow_warning_threshold")
    @Builder.Default
    private Integer yellowWarningThreshold = 80;

    @Column(name = "orange_warning_threshold")
    @Builder.Default
    private Integer orangeWarningThreshold = 100;

    @Column(name = "red_warning_threshold")
    @Builder.Default
    private Integer redWarningThreshold = 120;

    @Column(name = "enable_projected_warnings", nullable = false)
    @Builder.Default
    private Boolean enableProjectedWarnings = true;

    @Column(name = "projected_warning_min_days")
    @Builder.Default
    private Integer projectedWarningMinDays = 5;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Account account;
}
