package com.relyon.credflow.model.budget;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BudgetPreferencesResponseDTO {
    private Long id;
    private Long accountId;
    private Long userId;
    private String userName;
    private Boolean enableAlerts;
    private Boolean enableRollover;
    private Integer rolloverMaxMonths;
    private Integer rolloverMaxPercentage;
    private Integer yellowWarningThreshold;
    private Integer orangeWarningThreshold;
    private Integer redWarningThreshold;
    private Boolean enableProjectedWarnings;
    private Integer projectedWarningMinDays;
}
