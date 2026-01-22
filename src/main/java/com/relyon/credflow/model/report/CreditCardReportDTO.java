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
public class CreditCardReportDTO {
    private List<CreditCardExpenseDTO> creditCards;
    private BigDecimal totalExpense;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCardExpenseDTO {
        private Long creditCardId;
        private String creditCardNickname;
        private BigDecimal amount;
        private Integer transactionCount;
        private BigDecimal percentage;
        private BigDecimal averageTransactionAmount;
    }
}
