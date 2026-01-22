package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.user.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class TransactionMapperTest {

    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TransactionMapper.class);
    }

    @Test
    void categoryToName_withCategory_returnsName() {
        var category = new Category();
        category.setId(1L);
        category.setName("Food");

        var result = mapper.categoryToName(category);

        assertEquals("Food", result);
    }

    @Test
    void categoryToName_withNullCategory_returnsNull() {
        var result = mapper.categoryToName(null);

        assertNull(result);
    }

    @Test
    void usersToIds_withUsers_returnsIds() {
        var user1 = new User();
        user1.setId(1L);
        var user2 = new User();
        user2.setId(2L);
        Set<User> users = new HashSet<>();
        users.add(user1);
        users.add(user2);

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
    void usersToIds_withEmptySet_returnsEmptyList() {
        var result = mapper.usersToIds(Set.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void idToCategory_withId_returnsCategoryWithId() {
        var result = mapper.idToCategory(42L);

        assertNotNull(result);
        assertEquals(42L, result.getId());
    }

    @Test
    void idToCategory_withNullId_returnsNull() {
        var result = mapper.idToCategory(null);

        assertNull(result);
    }

    @Test
    void idToCreditCard_withId_returnsCreditCardWithId() {
        var result = mapper.idToCreditCard(100L);

        assertNotNull(result);
        assertInstanceOf(CreditCard.class, result);
        assertEquals(100L, result.getId());
    }

    @Test
    void idToCreditCard_withNullId_returnsNull() {
        var result = mapper.idToCreditCard(null);

        assertNull(result);
    }

    @Test
    void idsToUsers_withIds_returnsUsersWithIds() {
        var ids = List.of(1L, 2L, 3L);

        var result = mapper.idsToUsers(ids);

        assertNotNull(result);
        assertEquals(3, result.size());
        var resultIds = result.stream().map(User::getId).toList();
        assertTrue(resultIds.contains(1L));
        assertTrue(resultIds.contains(2L));
        assertTrue(resultIds.contains(3L));
    }

    @Test
    void idsToUsers_withNullList_returnsEmptySet() {
        var result = mapper.idsToUsers(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void idsToUsers_withEmptyList_returnsEmptySet() {
        var result = mapper.idsToUsers(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void idsToUsers_withNullIdInList_skipsNullIds() {
        var ids = new java.util.ArrayList<Long>();
        ids.add(1L);
        ids.add(null);
        ids.add(3L);

        var result = mapper.idsToUsers(ids);

        assertNotNull(result);
        assertEquals(2, result.size());
        var resultIds = result.stream().map(User::getId).toList();
        assertTrue(resultIds.contains(1L));
        assertTrue(resultIds.contains(3L));
        assertFalse(resultIds.contains(null));
    }

    @Test
    void idsToUsers_preservesOrder() {
        var ids = List.of(3L, 1L, 2L);

        var result = mapper.idsToUsers(ids);

        var resultList = result.stream().map(User::getId).toList();
        assertEquals(3L, resultList.get(0));
        assertEquals(1L, resultList.get(1));
        assertEquals(2L, resultList.get(2));
    }
}
