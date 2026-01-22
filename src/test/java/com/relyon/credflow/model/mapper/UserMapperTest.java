package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toDto_withUser_mapsAllFields() {
        var account = new Account();
        account.setId(1L);
        account.setName("Test Account");

        var user = new User();
        user.setId(10L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRole(UserRole.OWNER);
        user.setAccount(account);

        var result = mapper.toDto(user);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals(1L, result.getAccountId());
    }

    @Test
    void toDto_withNullAccount_returnsNullAccountId() {
        var user = new User();
        user.setId(10L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setAccount(null);

        var result = mapper.toDto(user);

        assertNotNull(result);
        assertNull(result.getAccountId());
    }

    @Test
    void toDto_multipleUsers_mapsCorrectly() {
        var account = new Account();
        account.setId(1L);

        var user1 = new User();
        user1.setId(1L);
        user1.setName("User1");
        user1.setEmail("user1@test.com");
        user1.setAccount(account);

        var user2 = new User();
        user2.setId(2L);
        user2.setName("User2");
        user2.setEmail("user2@test.com");
        user2.setAccount(account);

        var result1 = mapper.toDto(user1);
        var result2 = mapper.toDto(user2);

        assertEquals(1L, result1.getId());
        assertEquals("User1", result1.getName());
        assertEquals(2L, result2.getId());
        assertEquals("User2", result2.getName());
    }
}
