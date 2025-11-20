package com.relyon.credflow.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentGroupResponseDTO {

    private String installmentGroupId;
    private String description;
    private BigDecimal totalAmount;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer pendingInstallments;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private List<TransactionResponseDTO> installments;
}
