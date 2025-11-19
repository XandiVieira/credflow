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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mapper(config = MapStructCentralConfig.class)
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    @Mapping(target = "responsibleUsers", source = "responsibleUsers", qualifiedByName = "idsToUsers")
    @Mapping(target = "creditCard", source = "creditCardId", qualifiedByName = "idToCreditCard")
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "importBatchId", ignore = true)
    @Mapping(target = "wasEditedAfterImport", ignore = true)
    @Mapping(target = "originalChecksum", ignore = true)
    @Mapping(target = "isReversal", ignore = true)
    @Mapping(target = "relatedTransaction", ignore = true)
    @Mapping(target = "csvImportHistory", ignore = true)
    Transaction toEntity(TransactionRequestDTO dto);

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "responsibleUsers", source = "responsibleUsers", qualifiedByName = "usersToIds")
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryToName")
    @Mapping(target = "creditCard.id", source = "creditCard.id")
    @Mapping(target = "creditCard.nickname", source = "creditCard.nickname")
    @Mapping(target = "creditCard.brand", source = "creditCard.brand")
    @Mapping(target = "creditCard.lastFourDigits", source = "creditCard.lastFourDigits")
    @Mapping(target = "relatedTransactionId", source = "relatedTransaction.id")
    TransactionResponseDTO toDto(Transaction entity);

    @Named("categoryToName")
    default String categoryToName(Category c) {
        return (c == null) ? null : c.getName();
    }

    @Named("usersToIds")
    default List<Long> usersToIds(Set<User> users) {
        if (users == null || users.isEmpty()) return List.of();
        return users.stream().map(User::getId).toList();
    }

    @Named("idToCategory")
    default Category idToCategory(Long id) {
        if (id == null) return null;
        var c = new Category();
        c.setId(id);
        return c;
    }

    @Named("idToCreditCard")
    default com.relyon.credflow.model.credit_card.CreditCard idToCreditCard(Long id) {
        if (id == null) return null;
        var card = new com.relyon.credflow.model.credit_card.CreditCard();
        card.setId(id);
        return card;
    }

    @Named("idsToUsers")
    default Set<User> idsToUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        var out = new LinkedHashSet<User>(ids.size());
        for (Long id : ids) {
            if (id == null) continue;
            var u = new User();
            u.setId(id);
            out.add(u);
        }
        return out;
    }
}