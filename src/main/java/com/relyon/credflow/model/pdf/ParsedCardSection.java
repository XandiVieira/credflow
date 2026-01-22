package com.relyon.credflow.model.pdf;

import java.util.List;
import lombok.Builder;

@Builder
public record ParsedCardSection(
        String lastFourDigits,
        String holderName,
        List<ParsedCreditCardTransaction> transactions
) {
}

