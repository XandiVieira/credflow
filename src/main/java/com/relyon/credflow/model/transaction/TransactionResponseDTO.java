package com.relyon.credflow.model.transaction;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionResponseDTO {
    private Long id;
    private LocalDate date;
    private String description;
    private String simplifiedDescription;
    private String category;
    private BigDecimal value;
    private String responsible;
    private Long accountId;
}