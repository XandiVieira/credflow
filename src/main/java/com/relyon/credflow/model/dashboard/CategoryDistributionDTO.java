package com.relyon.credflow.model.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDistributionDTO {
    private List<CategorySliceDTO> slices;
    private BigDecimal total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySliceDTO {
        private Long categoryId;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
        private String color;
    }
}
