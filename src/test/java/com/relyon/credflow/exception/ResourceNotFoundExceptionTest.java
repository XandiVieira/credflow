package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_withMessageKey_setsMessageKey() {
        var exception = new ResourceNotFoundException("resource.user.notFound");

        assertEquals("resource.user.notFound", exception.getMessageKey());
        assertEquals("resource.user.notFound", exception.getMessage());
    }

    @Test
    void constructor_withMessageKeyAndArguments_setsBoth() {
        var exception = new ResourceNotFoundException("resource.notFound", "User", 123L);

        assertEquals("resource.notFound", exception.getMessageKey());
        assertEquals(2, exception.getArguments().length);
        assertEquals("User", exception.getArguments()[0]);
        assertEquals(123L, exception.getArguments()[1]);
    }

    @Test
    void constructor_noArguments_hasEmptyArgumentsArray() {
        var exception = new ResourceNotFoundException("resource.generic.notFound");

        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    @SuppressWarnings("deprecation")
    void withMessage_deprecated_createsExceptionWithNotFoundKey() {
        var exception = ResourceNotFoundException.withMessage("Custom message");

        assertEquals("resource.notFound", exception.getMessageKey());
        assertEquals(1, exception.getArguments().length);
        assertEquals("Custom message", exception.getArguments()[0]);
    }

    @Test
    void exception_extendsDomainException() {
        var exception = new ResourceNotFoundException("test");

        assertInstanceOf(DomainException.class, exception);
    }

    @Test
    void exception_isRuntimeException() {
        var exception = new ResourceNotFoundException("test");

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void constructor_withNullArgument_handlesNull() {
        var exception = new ResourceNotFoundException("resource.notFound", (Object) null);

        assertEquals(1, exception.getArguments().length);
        assertNull(exception.getArguments()[0]);
    }

    @Test
    void constructor_withMultipleNullArguments_handlesNulls() {
        var exception = new ResourceNotFoundException("resource.notFound", null, "value", null);

        assertEquals(3, exception.getArguments().length);
        assertNull(exception.getArguments()[0]);
        assertEquals("value", exception.getArguments()[1]);
        assertNull(exception.getArguments()[2]);
    }
}
