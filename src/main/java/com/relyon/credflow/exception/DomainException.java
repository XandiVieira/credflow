package com.relyon.credflow.exception;

import lombok.Getter;

/**
 * Base class for all domain exceptions that support i18n message translation.
 * Subclasses should provide a message key and optional arguments for parameterized messages.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final String messageKey;
    private final Object[] arguments;

    protected DomainException(String messageKey, Object... arguments) {
        super(messageKey);
        this.messageKey = messageKey;
        this.arguments = arguments;
    }

    protected DomainException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.arguments = arguments;
    }
}
