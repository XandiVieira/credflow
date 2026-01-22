package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    private static class TestDomainException extends DomainException {
        TestDomainException(String messageKey, Object... arguments) {
            super(messageKey, arguments);
        }

        TestDomainException(String messageKey, Throwable cause, Object... arguments) {
            super(messageKey, cause, arguments);
        }
    }

    @Test
    void constructor_withMessageKeyOnly_setsMessageKey() {
        var exception = new TestDomainException("error.test");

        assertEquals("error.test", exception.getMessageKey());
        assertEquals("error.test", exception.getMessage());
        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    void constructor_withMessageKeyAndArguments_setsBoth() {
        var exception = new TestDomainException("error.notFound", "User", 123);

        assertEquals("error.notFound", exception.getMessageKey());
        assertEquals(2, exception.getArguments().length);
        assertEquals("User", exception.getArguments()[0]);
        assertEquals(123, exception.getArguments()[1]);
    }

    @Test
    void constructor_withCause_setsCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new TestDomainException("error.test", cause);

        assertEquals("error.test", exception.getMessageKey());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructor_withCauseAndArguments_setsBoth() {
        var cause = new IllegalArgumentException("Bad argument");
        var exception = new TestDomainException("error.invalid", cause, "field1", "value1");

        assertEquals("error.invalid", exception.getMessageKey());
        assertSame(cause, exception.getCause());
        assertEquals(2, exception.getArguments().length);
        assertEquals("field1", exception.getArguments()[0]);
        assertEquals("value1", exception.getArguments()[1]);
    }

    @Test
    void getMessage_returnsMessageKey() {
        var exception = new TestDomainException("custom.message.key");

        assertEquals("custom.message.key", exception.getMessage());
    }

    @Test
    void getArguments_emptyWhenNoArgs() {
        var exception = new TestDomainException("error.noargs");

        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    void getArguments_preservesOrder() {
        var exception = new TestDomainException("error.ordered", "first", "second", "third");

        assertEquals(3, exception.getArguments().length);
        assertEquals("first", exception.getArguments()[0]);
        assertEquals("second", exception.getArguments()[1]);
        assertEquals("third", exception.getArguments()[2]);
    }

    @Test
    void getArguments_supportsVariousTypes() {
        var exception = new TestDomainException("error.mixed", "string", 42, 3.14, true);

        assertEquals(4, exception.getArguments().length);
        assertEquals("string", exception.getArguments()[0]);
        assertEquals(42, exception.getArguments()[1]);
        assertEquals(3.14, exception.getArguments()[2]);
        assertEquals(true, exception.getArguments()[3]);
    }

    @Test
    void exception_isRuntimeException() {
        var exception = new TestDomainException("error.test");

        assertInstanceOf(RuntimeException.class, exception);
    }
}
