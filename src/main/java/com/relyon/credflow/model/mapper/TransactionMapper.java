package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionResponseDTO;
import com.relyon.credflow.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(config = MapStructCentralConfig.class)
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    @Mapping(target = "responsibles", source = "responsibles", qualifiedByName = "idsToUsers")
    Transaction toEntity(TransactionRequestDTO dto);

    @Mapping(target = "category", source = "category.name")
    @Mapping(target = "responsibles", source = "responsibles", qualifiedByName = "usersToIds")
    @Mapping(target = "accountId", source = "account.id")
    TransactionResponseDTO toDto(Transaction entity);

    @Named("usersToIds")
    default List<Long> usersToIds(Set<User> users) {
        if (users == null || users.isEmpty()) return java.util.Collections.emptyList();
        return users.stream().map(User::getId).toList();
    }

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        var c = new Category();
        c.setId(id);
        return c;
    }

    @Named("idsToUsers")
    default Set<User> idsToUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptySet();
        var out = new java.util.LinkedHashSet<User>();
        for (Long id : ids) {
            if (id == null) continue;
            var u = new User();
            u.setId(id);
            out.add(u);
        }
        return out;
    }

    @Named("usersToNames")
    default List<String> usersToNames(Set<User> users) {
        if (users == null || users.isEmpty()) return java.util.Collections.emptyList();
        return users.stream().map(User::getName).toList();
    }
}