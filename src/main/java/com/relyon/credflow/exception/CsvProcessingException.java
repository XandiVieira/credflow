package com.relyon.credflow.exception;

/**
 * Exception thrown when CSV processing fails.
 * Uses i18n message keys for localized error messages.
 */
public class CsvProcessingException extends DomainException {

    /**
     * Creates a CsvProcessingException with a message key, cause, and arguments.
     *
     * @param messageKey the i18n message key
     * @param cause the underlying cause
     * @param arguments optional arguments for parameterized messages
     */
    public CsvProcessingException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }

    /**
     * Creates a CsvProcessingException with a message key and arguments (no cause).
     *
     * @param messageKey the i18n message key
     * @param arguments optional arguments for parameterized messages
     */
    public CsvProcessingException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}
