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
    private List<Long> responsibles;
}