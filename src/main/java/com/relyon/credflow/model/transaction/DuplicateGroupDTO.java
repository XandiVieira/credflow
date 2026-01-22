package com.relyon.credflow.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuplicateGroupDTO {

    private String groupKey;
    private List<DuplicateTransactionDTO> transactions;

    @Data
    @Builder
    public static class DuplicateTransactionDTO {
        private Long id;
        private LocalDate date;
        private String description;
        private String simplifiedDescription;
        private BigDecimal value;
        private TransactionSource source;
        private String categoryName;
        private String creditCardNickname;
    }
}
