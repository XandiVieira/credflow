package com.relyon.credflow.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Uses i18n message keys for localized error messages.
 */
public class ResourceAlreadyExistsException extends DomainException {

    /**
     * Creates a ResourceAlreadyExistsException with a message key and arguments.
     *
     * @param messageKey the i18n message key
     * @param arguments optional arguments for parameterized messages
     */
    public ResourceAlreadyExistsException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}
