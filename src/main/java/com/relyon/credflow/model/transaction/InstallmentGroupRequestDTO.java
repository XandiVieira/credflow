package com.relyon.credflow.model.transaction;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentGroupRequestDTO {

    @NotNull(message = "{transaction.description.notNull}")
    @NotBlank(message = "{transaction.description.notBlank}")
    private String description;

    @NotNull(message = "{transaction.amount.notNull}")
    @DecimalMin(value = "0.01", message = "{transaction.amount.positive}")
    private BigDecimal totalAmount;

    @NotNull(message = "{transaction.type.notNull}")
    private TransactionType transactionType;

    @NotNull(message = "{transaction.categoryId.notNull}")
    private Long categoryId;

    private Long creditCardId;

    @NotNull(message = "{installment.total.notNull}")
    @Min(value = 2, message = "{installment.total.min}")
    @Max(value = 120, message = "{installment.total.max}")
    private Integer totalInstallments;

    @NotNull(message = "{installment.firstDate.notNull}")
    private LocalDate firstInstallmentDate;

    private Set<Long> responsibleUserIds;
}
