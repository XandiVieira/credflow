package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingRequestDTO;
import com.relyon.credflow.model.descriptionmapping.DescriptionMappingResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapStructCentralConfig.class)
public interface DescriptionMappingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "normalizedDescription", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    DescriptionMapping toEntity(DescriptionMappingRequestDTO dto);

    @Mapping(target = "category", source = "category.name")
    @Mapping(target = "accountId", source = "account.id")
    DescriptionMappingResponseDTO toDto(DescriptionMapping entity);

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        Category c = new Category();
        c.setId(id);
        return c;
    }
}