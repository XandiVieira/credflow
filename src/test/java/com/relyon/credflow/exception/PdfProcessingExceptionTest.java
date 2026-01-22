package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PdfProcessingExceptionTest {

    @Test
    void constructor_withMessageKey_setsMessageKey() {
        var exception = new PdfProcessingException("pdf.parseError");

        assertEquals("pdf.parseError", exception.getMessageKey());
        assertEquals("pdf.parseError", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructor_withMessageKeyAndArguments_setsBoth() {
        var exception = new PdfProcessingException("pdf.invalidPage", 5, "statement.pdf");

        assertEquals("pdf.invalidPage", exception.getMessageKey());
        assertEquals(2, exception.getArguments().length);
        assertEquals(5, exception.getArguments()[0]);
        assertEquals("statement.pdf", exception.getArguments()[1]);
    }

    @Test
    void constructor_withCause_setsCause() {
        var cause = new RuntimeException("PDF decryption failed");
        var exception = new PdfProcessingException("pdf.decryptionError", cause);

        assertEquals("pdf.decryptionError", exception.getMessageKey());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructor_withCauseAndArguments_setsBoth() {
        var cause = new IllegalStateException("Corrupted file");
        var exception = new PdfProcessingException("pdf.corruptedFile", cause, "filename.pdf");

        assertEquals("pdf.corruptedFile", exception.getMessageKey());
        assertSame(cause, exception.getCause());
        assertEquals(1, exception.getArguments().length);
        assertEquals("filename.pdf", exception.getArguments()[0]);
    }

    @Test
    void exception_extendsDomainException() {
        var exception = new PdfProcessingException("test");

        assertInstanceOf(DomainException.class, exception);
    }

    @Test
    void constructor_noArguments_hasEmptyArgumentsArray() {
        var exception = new PdfProcessingException("pdf.generic");

        assertNotNull(exception.getArguments());
        assertEquals(0, exception.getArguments().length);
    }

    @Test
    void constructor_causeOnly_noArguments() {
        var cause = new Exception("Root cause");
        var exception = new PdfProcessingException("pdf.error", cause);

        assertEquals(0, exception.getArguments().length);
        assertSame(cause, exception.getCause());
    }

    @Test
    void exception_isRuntimeException() {
        var exception = new PdfProcessingException("test");

        assertInstanceOf(RuntimeException.class, exception);
    }
}
