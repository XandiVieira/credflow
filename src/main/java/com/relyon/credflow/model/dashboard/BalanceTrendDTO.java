package com.relyon.credflow.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceTrendDTO {
    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
}
