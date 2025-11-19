package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.budget.*;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapStructCentralConfig.class)
public interface BudgetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    @Mapping(target = "user", source = "userId", qualifiedByName = "idToUser")
    @Mapping(target = "rolledOverAmount", ignore = true)
    Budget toEntity(BudgetRequestDTO dto);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "effectiveBudget", expression = "java(entity.getAmount().add(entity.getRolledOverAmount() != null ? entity.getRolledOverAmount() : java.math.BigDecimal.ZERO))")
    BudgetResponseDTO toDto(Budget entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "user", source = "userId", qualifiedByName = "idToUser")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    BudgetPreferences toEntity(BudgetPreferencesRequestDTO dto);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.name")
    BudgetPreferencesResponseDTO toDto(BudgetPreferences entity);

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        var category = new Category();
        category.setId(id);
        return category;
    }

    @Named("idToUser")
    default User idToUser(Long id) {
        if (id == null) return null;
        var user = new User();
        user.setId(id);
        return user;
    }
}
