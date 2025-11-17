package com.relyon.credflow.service;

import com.relyon.credflow.exception.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for translating exception messages using Spring's MessageSource.
 * Automatically resolves the current locale from the request context.
 */
@RequiredArgsConstructor
@Service
public class LocalizedMessageTranslationService {

    private final MessageSource messageSource;

    /**
     * Translates a DomainException's message key to the current locale.
     *
     * @param exception the domain exception containing message key and arguments
     * @return the translated message
     */
    public String translateMessage(DomainException exception) {
        var locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(
                exception.getMessageKey(),
                exception.getArguments(),
                locale
        );
    }

    /**
     * Translates a message key with arguments to the current locale.
     *
     * @param messageKey the message key
     * @param arguments optional arguments for parameterized messages
     * @return the translated message
     */
    public String translateMessage(String messageKey, Object... arguments) {
        var locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(messageKey, arguments, locale);
    }
}
