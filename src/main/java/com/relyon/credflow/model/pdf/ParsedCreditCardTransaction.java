package com.relyon.credflow.model.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record ParsedCreditCardTransaction(
        LocalDate date,
        String description,
        BigDecimal valueBrl,
        BigDecimal valueUsd,
        Integer currentInstallment,
        Integer totalInstallments,
        String cardLastFourDigits,
        String cardHolderName,
        String rawLine
) {
}

