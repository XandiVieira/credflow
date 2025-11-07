package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryRequestDTO;
import com.relyon.credflow.model.category.CategoryResponseDTO;
import com.relyon.credflow.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Mapper(config = MapStructCentralConfig.class)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "defaultResponsibles", source = "defaultResponsibleIds", qualifiedByName = "idsToUsers")
    @Mapping(target = "parentCategory", source = "parentCategoryId", qualifiedByName = "idToCategory")
    Category toEntity(CategoryRequestDTO dto);

    @Mapping(target = "defaultResponsibleIds", source = "defaultResponsibles", qualifiedByName = "usersToIds")
    @Mapping(target = "parentCategoryId", source = "parentCategory.id")
    @Mapping(target = "parentCategoryName", source = "parentCategory.name")
    @Mapping(target = "childCategories", ignore = true)
    CategoryResponseDTO toDto(Category category);

    @Named("idsToUsers")
    default Set<User> idsToUsers(List<Long> ids) {
        if (ids == null) return null;
        Set<User> out = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) continue;
            User u = new User();
            u.setId(id);
            out.add(u);
        }
        return out;
    }

    @Named("usersToIds")
    default List<Long> usersToIds(Set<User> users) {
        if (users == null) return List.of();
        return users.stream().map(User::getId).filter(Objects::nonNull).toList();
    }

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        Category category = new Category();
        category.setId(id);
        return category;
    }
}