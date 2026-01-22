package com.relyon.credflow.exception;

public class UnauthorizedAccessException extends DomainException {

    public UnauthorizedAccessException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}
