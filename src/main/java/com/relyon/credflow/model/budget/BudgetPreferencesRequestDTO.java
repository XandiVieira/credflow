package com.relyon.credflow.model.budget;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BudgetPreferencesRequestDTO {

    private Long userId;

    @NotNull(message = "Enable alerts flag is required")
    private Boolean enableAlerts;

    @NotNull(message = "Enable rollover flag is required")
    private Boolean enableRollover;

    @Min(value = 1, message = "Rollover max months must be at least 1")
    @Max(value = 12, message = "Rollover max months cannot exceed 12")
    private Integer rolloverMaxMonths;

    @Min(value = 1, message = "Rollover max percentage must be at least 1")
    @Max(value = 200, message = "Rollover max percentage cannot exceed 200")
    private Integer rolloverMaxPercentage;

    @Min(value = 1, message = "Yellow warning threshold must be at least 1")
    @Max(value = 200, message = "Yellow warning threshold cannot exceed 200")
    private Integer yellowWarningThreshold;

    @Min(value = 1, message = "Orange warning threshold must be at least 1")
    @Max(value = 200, message = "Orange warning threshold cannot exceed 200")
    private Integer orangeWarningThreshold;

    @Min(value = 1, message = "Red warning threshold must be at least 1")
    @Max(value = 300, message = "Red warning threshold cannot exceed 300")
    private Integer redWarningThreshold;

    @NotNull(message = "Enable projected warnings flag is required")
    private Boolean enableProjectedWarnings;

    @Min(value = 1, message = "Projected warning min days must be at least 1")
    @Max(value = 31, message = "Projected warning min days cannot exceed 31")
    private Integer projectedWarningMinDays;
}
