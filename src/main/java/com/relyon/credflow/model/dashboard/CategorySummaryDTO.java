package com.relyon.credflow.model.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryDTO {
    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private Integer transactionCount;
    private BigDecimal percentage;
}
