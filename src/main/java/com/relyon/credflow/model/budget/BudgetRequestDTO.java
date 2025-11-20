package com.relyon.credflow.model.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
public class BudgetRequestDTO {

    @NotNull(message = "Period is required")
    private YearMonth period;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Budget type is required")
    private BudgetType type;

    private Long categoryId;

    private Long userId;

    private Boolean allowRollover = false;
}
