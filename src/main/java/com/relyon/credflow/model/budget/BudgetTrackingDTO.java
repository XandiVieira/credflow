package com.relyon.credflow.model.budget;

import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BudgetTrackingDTO {
    private Long budgetId;
    private YearMonth period;
    private String categoryName;
    private String userName;
    private BigDecimal budgetAmount;
    private BigDecimal rolledOverAmount;
    private BigDecimal effectiveBudget;
    private BigDecimal currentSpend;
    private BigDecimal remainingBudget;
    private BigDecimal percentageUsed;
    private Integer daysElapsed;
    private Integer daysRemaining;
    private Integer totalDaysInPeriod;
    private BigDecimal dailyBurnRate;
    private BigDecimal projectedEndSpend;
    private BigDecimal projectedOverspend;
    private WarningLevel warningLevel;
    private String warningMessage;
}
