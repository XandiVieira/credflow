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
public class CategoryReportDTO {
    private List<CategoryExpenseDTO> categories;
    private BigDecimal totalExpense;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpenseDTO {
        private Long categoryId;
        private String categoryName;
        private Long parentCategoryId;
        private String parentCategoryName;
        private BigDecimal amount;
        private Integer transactionCount;
        private BigDecimal percentage;
        private BigDecimal averageTransactionAmount;
    }
}
