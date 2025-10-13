package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructCentralConfig.class)
public interface CreditCardMapper {

    @Mapping(target = "accountId", source = "account.id")
    CreditCardResponseDTO toDTO(CreditCard entity);
}
