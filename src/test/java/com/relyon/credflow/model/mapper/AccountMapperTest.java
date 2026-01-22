package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AccountMapperTest {

    private AccountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void usersToIds_withUsers_returnsIds() {
        var user1 = new User();
        user1.setId(1L);
        var user2 = new User();
        user2.setId(2L);
        Collection<User> users = List.of(user1, user2);

        var result = mapper.usersToIds(users);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
    }

    @Test
    void usersToIds_withNullUsers_returnsEmptyList() {
        var result = mapper.usersToIds(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void usersToIds_withEmptyCollection_returnsEmptyList() {
        var result = mapper.usersToIds(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void usersToIds_withNullUserInCollection_filtersOutNull() {
        var user1 = new User();
        user1.setId(1L);
        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(null);

        var result = mapper.usersToIds(users);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    void usersToIds_withUserWithNullId_filtersOutNullIds() {
        var user1 = new User();
        user1.setId(1L);
        var user2 = new User();
        user2.setId(null);
        Collection<User> users = List.of(user1, user2);

        var result = mapper.usersToIds(users);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    void toDto_withAccount_mapsAllFields() {
        var account = new Account();
        account.setId(1L);
        account.setName("Test Account");
        account.setDescription("Test Description");
        account.setInviteCode("ABC123");

        var user = new User();
        user.setId(10L);
        account.setUsers(List.of(user));

        var result = mapper.toDto(account);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Account", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals("ABC123", result.getInviteCode());
        assertEquals(1, result.getUserIds().size());
        assertTrue(result.getUserIds().contains(10L));
    }

    @Test
    void toDto_withNullUsers_returnsEmptyUserIds() {
        var account = new Account();
        account.setId(1L);
        account.setName("Test");
        account.setUsers(null);

        var result = mapper.toDto(account);

        assertNotNull(result);
        assertNotNull(result.getUserIds());
        assertTrue(result.getUserIds().isEmpty());
    }
}
