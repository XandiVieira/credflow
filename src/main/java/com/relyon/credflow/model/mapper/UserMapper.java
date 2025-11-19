package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.model.user.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructCentralConfig.class)
public interface UserMapper {

    // Controller -> Entity (service sets account and id/timestamps)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "resetTokenExpiry", ignore = true)
    User toEntity(UserRequestDTO dto);

    // Entity -> DTO (flatten account id)
    @Mapping(target = "accountId", source = "account.id")
    UserResponseDTO toDto(User user);
}