package com.relyon.credflow.model.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionRequestDTO {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Original description is required")
    private String description;

    private String simplifiedDescription;

    private String category;

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.01", message = "Value must be greater than zero")
    private BigDecimal value;

    @NotBlank(message = "Responsible is required")
    private String responsible;

    @NotNull(message = "Account ID is required")
    private Long accountId;
}