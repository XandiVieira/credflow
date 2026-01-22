package com.relyon.credflow.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingBillDTO {
    private Long transactionId;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String creditCardNickname;
    private Integer daysUntilDue;
}
