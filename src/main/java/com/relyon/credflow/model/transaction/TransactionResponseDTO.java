package com.relyon.credflow.model.transaction;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionResponseDTO {
    private Long id;
    private LocalDate date;
    private String description;
    private String simplifiedDescription;
    private String category;
    private BigDecimal value;
    private Long accountId;
    private List<Long> responsibleUsers;
    private CreditCardDTO creditCard;
    private TransactionType transactionType;
    private Integer currentInstallment;
    private Integer totalInstallments;
    private String installmentGroupId;
    private TransactionSource source;
    private String importBatchId;
    private Boolean wasEditedAfterImport;
    private Boolean isReversal;
    private Long relatedTransactionId;

    @Data
    public static class CreditCardDTO {
        private Long id;
        private String nickname;
        private String brand;
        private String lastFourDigits;
    }
}