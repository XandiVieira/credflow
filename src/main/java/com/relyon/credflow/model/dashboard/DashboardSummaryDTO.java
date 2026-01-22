package com.relyon.credflow.model.dashboard;

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
public class DashboardSummaryDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private List<CategorySummaryDTO> topCategories;
    private List<UpcomingBillDTO> upcomingBills;
    private List<BalanceTrendDTO> balanceTrend;
}
