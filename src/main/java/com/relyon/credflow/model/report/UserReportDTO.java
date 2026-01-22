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
public class UserReportDTO {
    private List<UserExpenseDTO> users;
    private BigDecimal totalExpense;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserExpenseDTO {
        private Long userId;
        private String userName;
        private BigDecimal amount;
        private Integer transactionCount;
        private BigDecimal percentage;
    }
}
