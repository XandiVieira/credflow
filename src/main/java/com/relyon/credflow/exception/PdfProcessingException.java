package com.relyon.credflow.exception;

public class PdfProcessingException extends DomainException {

    public PdfProcessingException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }

    public PdfProcessingException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}
