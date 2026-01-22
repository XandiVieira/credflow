package com.relyon.credflow.model.report;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthComparisonDTO {
    private List<MonthlyDataDTO> months;
    private ComparisonSummaryDTO summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyDataDTO {
        private Integer year;
        private Integer month;
        private String monthName;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
        private Integer transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummaryDTO {
        private BigDecimal averageMonthlyIncome;
        private BigDecimal averageMonthlyExpense;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal highestExpenseMonth;
        private String highestExpenseMonthName;
        private BigDecimal lowestExpenseMonth;
        private String lowestExpenseMonthName;
    }
}
