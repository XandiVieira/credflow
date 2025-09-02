package com.relyon.credflow.model.mapper;

import com.relyon.credflow.configuration.MapStructCentralConfig;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.account.AccountResponseDTO;
import com.relyon.credflow.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper(config = MapStructCentralConfig.class)
public interface AccountMapper {

    @Mapping(target = "userIds", source = "users", qualifiedByName = "usersToIds")
    AccountResponseDTO toDto(Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "descriptionMappings", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "inviteCode", ignore = true)
    Account toEntity(AccountRequestDTO dto);

    @Named("usersToIds")
    default List<Long> usersToIds(Collection<User> users) {
        if (users == null) return List.of();
        return users.stream().map(User::getId).filter(Objects::nonNull).toList();
    }
}