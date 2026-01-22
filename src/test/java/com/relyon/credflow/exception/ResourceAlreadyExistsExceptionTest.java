package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResourceAlreadyExistsExceptionTest {

    @Test
    void constructor_withMessageKey_setsMessageKey() {
        var exception = new ResourceAlreadyExistsException("resource.email.exists");

        assertEquals("resource.email.exists", exception.getMessageKey());
        assertEquals("resource.email.exists", exception.getMessage());
    }

    @Test
    void constructor_withMessageKeyAndArguments_setsBoth() {
        var exception = new ResourceAlreadyExistsException("resource.exists", "User", "test@test.com");

        assertEquals("resource.exists", exception.getMessageKey());
        assertEquals(2, exception.getArguments().length);
        assertEquals("User", exception.getArguments()[0]);
        assertEquals("test@test.com", exception.getArguments()[1]);
    }

    @Test
    void constructor_noArguments_hasEmptyArgumentsArray() {
        var exception = new ResourceAlreadyExistsException("resource.duplicate");

        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    void exception_extendsDomainException() {
        var exception = new ResourceAlreadyExistsException("test");

        assertInstanceOf(DomainException.class, exception);
    }

    @Test
    void exception_isRuntimeException() {
        var exception = new ResourceAlreadyExistsException("test");

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void constructor_withSingleArgument_setsCorrectly() {
        var exception = new ResourceAlreadyExistsException("resource.category.exists", "Food");

        assertEquals("resource.category.exists", exception.getMessageKey());
        assertEquals(1, exception.getArguments().length);
        assertEquals("Food", exception.getArguments()[0]);
    }
}
