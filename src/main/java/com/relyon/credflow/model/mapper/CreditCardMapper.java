package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardRequestDTO;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import org.mapstruct.*;

@Mapper(config = MapStructCentralConfig.class)
public interface CreditCardMapper {


    @Mapping(target = "account", expression = "java(entity.getAccount() != null ? entity.getAccount().getId() : null)")
    @Mapping(target = "holder.id", source = "holder.id")
    @Mapping(target = "holder.name", source = "holder.name")
    @Mapping(target = "holder.email", source = "holder.email")
    @Mapping(target = "availableCreditLimit", ignore = true)
    @Mapping(target = "currentBill", ignore = true)
    CreditCardResponseDTO toDTO(CreditCard entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "holder", ignore = true)
    CreditCard toEntity(CreditCardRequestDTO dto);
}
