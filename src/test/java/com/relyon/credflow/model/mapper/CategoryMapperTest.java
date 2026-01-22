package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CategoryMapperTest {

    private CategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CategoryMapper.class);
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
    void idsToUsers_withNullList_returnsNull() {
        var result = mapper.idsToUsers(null);

        assertNull(result);
    }

    @Test
    void idsToUsers_withEmptyList_returnsEmptySet() {
        var result = mapper.idsToUsers(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void idsToUsers_withNullIdInList_skipsNull() {
        var ids = new ArrayList<Long>();
        ids.add(1L);
        ids.add(null);
        ids.add(3L);

        var result = mapper.idsToUsers(ids);

        assertNotNull(result);
        assertEquals(2, result.size());
        var resultIds = result.stream().map(User::getId).toList();
        assertTrue(resultIds.contains(1L));
        assertTrue(resultIds.contains(3L));
    }

    @Test
    void usersToIds_withUsers_returnsIds() {
        var user1 = new User();
        user1.setId(10L);
        var user2 = new User();
        user2.setId(20L);
        Set<User> users = Set.of(user1, user2);

        var result = mapper.usersToIds(users);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(10L));
        assertTrue(result.contains(20L));
    }

    @Test
    void usersToIds_withNullSet_returnsEmptyList() {
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
    void usersToIds_filtersNullIds() {
        var user1 = new User();
        user1.setId(10L);
        var user2 = new User();
        user2.setId(null);
        Set<User> users = new HashSet<>();
        users.add(user1);
        users.add(user2);

        var result = mapper.usersToIds(users);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(10L));
    }

    @Test
    void idToCategory_withId_returnsCategoryWithId() {
        var result = mapper.idToCategory(50L);

        assertNotNull(result);
        assertEquals(50L, result.getId());
    }

    @Test
    void idToCategory_withNullId_returnsNull() {
        var result = mapper.idToCategory(null);

        assertNull(result);
    }
}
