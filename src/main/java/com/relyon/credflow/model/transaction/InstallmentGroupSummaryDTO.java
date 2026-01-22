package com.relyon.credflow.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentGroupSummaryDTO {

    private String installmentGroupId;
    private String description;
    private BigDecimal totalAmount;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer pendingInstallments;
    private BigDecimal totalPaid;
    private BigDecimal totalPending;
    private LocalDate firstInstallmentDate;
    private LocalDate lastInstallmentDate;
    private String categoryName;
    private String creditCardNickname;
}
