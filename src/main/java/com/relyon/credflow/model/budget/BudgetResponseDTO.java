package com.relyon.credflow.model.budget;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
public class BudgetResponseDTO {
    private Long id;
    private YearMonth period;
    private BigDecimal amount;
    private BudgetType type;
    private Long categoryId;
    private String categoryName;
    private Long userId;
    private String userName;
    private Boolean allowRollover;
    private BigDecimal rolledOverAmount;
    private BigDecimal effectiveBudget;
    private Long accountId;
}
