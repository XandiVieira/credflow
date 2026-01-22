package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DescriptionMappingMapperTest {

    private DescriptionMappingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(DescriptionMappingMapper.class);
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
    void idToCategory_withZeroId_returnsCategory() {
        var result = mapper.idToCategory(0L);

        assertNotNull(result);
        assertEquals(0L, result.getId());
    }

    @Test
    void idToCategory_withNegativeId_returnsCategory() {
        var result = mapper.idToCategory(-1L);

        assertNotNull(result);
        assertEquals(-1L, result.getId());
    }
}
