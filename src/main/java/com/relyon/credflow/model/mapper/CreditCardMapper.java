package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardRequestDTO;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.service.CreditCardService;
import org.mapstruct.*;

@Mapper(config = MapStructCentralConfig.class)
public interface CreditCardMapper {


    @Mapping(target = "account", source = "account.id")
    @Mapping(target = "availableCreditLimit", ignore = true)
    CreditCardResponseDTO toDTO(CreditCard entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "account", ignore = true)
    CreditCard toEntity(CreditCardRequestDTO dto);
}
