package com.relyon.credflow.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Uses i18n message keys for localized error messages.
 */
public class ResourceNotFoundException extends DomainException {

    /**
     * Creates a ResourceNotFoundException with a message key and arguments.
     *
     * @param messageKey the i18n message key
     * @param arguments optional arguments for parameterized messages
     */
    public ResourceNotFoundException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }

    /**
     * @deprecated Use constructor with message key instead. This method exists for backward compatibility.
     */
    @Deprecated
    public static ResourceNotFoundException withMessage(String message) {
        return new ResourceNotFoundException("resource.notFound", message);
    }
}
