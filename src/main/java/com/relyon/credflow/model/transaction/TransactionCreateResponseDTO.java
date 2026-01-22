package com.relyon.credflow.model.transaction;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionCreateResponseDTO {

    private TransactionResponseDTO transaction;
    private List<DuplicateGroupDTO.DuplicateTransactionDTO> potentialDuplicates;

    public boolean hasPotentialDuplicates() {
        return potentialDuplicates != null && !potentialDuplicates.isEmpty();
    }
}
