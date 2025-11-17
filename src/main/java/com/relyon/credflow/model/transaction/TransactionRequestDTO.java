package com.relyon.credflow.model.transaction;

import com.relyon.credflow.validation.InstallmentConsistency;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@InstallmentConsistency
public class TransactionRequestDTO {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Original description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Simplified description cannot exceed 500 characters")
    private String simplifiedDescription;

    private Long categoryId;

    @NotNull(message = "Value is required")
    private BigDecimal value;

    private List<Long> responsibleUsers;

    private Long creditCardId;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Min(value = 1, message = "Current installment must be at least 1")
    private Integer currentInstallment;

    @Min(value = 1, message = "Total installments must be at least 1")
    @Max(value = 360, message = "Total installments cannot exceed 360")
    private Integer totalInstallments;

    @Size(max = 100, message = "Installment group ID cannot exceed 100 characters")
    private String installmentGroupId;
}