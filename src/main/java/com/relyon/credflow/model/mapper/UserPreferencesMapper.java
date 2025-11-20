package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.user.UserPreferences;
import com.relyon.credflow.model.user.UserPreferencesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructCentralConfig.class)
public interface UserPreferencesMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserPreferences toEntity(UserPreferencesDTO dto);

    UserPreferencesDTO toDto(UserPreferences preferences);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(UserPreferencesDTO dto, @MappingTarget UserPreferences entity);
}
