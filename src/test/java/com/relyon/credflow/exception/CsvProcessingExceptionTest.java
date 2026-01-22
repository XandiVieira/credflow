package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CsvProcessingExceptionTest {

    @Test
    void constructor_withMessageKey_setsMessageKey() {
        var exception = new CsvProcessingException("csv.invalidFormat");

        assertEquals("csv.invalidFormat", exception.getMessageKey());
        assertEquals("csv.invalidFormat", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_withMessageKeyAndArguments_setsBoth() {
        var exception = new CsvProcessingException("csv.invalidRow", 5, "date");

        assertEquals("csv.invalidRow", exception.getMessageKey());
        assertEquals(2, exception.getArguments().length);
        assertEquals(5, exception.getArguments()[0]);
        assertEquals("date", exception.getArguments()[1]);
    }

    @Test
    void constructor_withCause_setsCause() {
        var cause = new RuntimeException("Parse error");
        var exception = new CsvProcessingException("csv.parseError", cause);

        assertEquals("csv.parseError", exception.getMessageKey());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructor_withCauseAndArguments_setsBoth() {
        var cause = new IllegalArgumentException("Invalid column");
        var exception = new CsvProcessingException("csv.invalidColumn", cause, "amount", 3);

        assertEquals("csv.invalidColumn", exception.getMessageKey());
        assertSame(cause, exception.getCause());
        assertEquals(2, exception.getArguments().length);
        assertEquals("amount", exception.getArguments()[0]);
        assertEquals(3, exception.getArguments()[1]);
    }

    @Test
    void exception_extendsDomainException() {
        var exception = new CsvProcessingException("test");

        assertInstanceOf(DomainException.class, exception);
    }

    @Test
    void constructor_noArguments_hasEmptyArgumentsArray() {
        var exception = new CsvProcessingException("csv.error");

        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    void constructor_causeOnly_noArguments() {
        var cause = new Exception("Root");
        var exception = new CsvProcessingException("csv.error", cause);

        assertEquals(0, exception.getArguments().length);
        assertSame(cause, exception.getCause());
    }
}
