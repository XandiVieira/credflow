package com.relyon.credflow.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class I18nConfigTest {

    private I18nConfig i18nConfig;

    @BeforeEach
    void setUp() {
        i18nConfig = new I18nConfig();
    }

    @Test
    void messageSource_returnsResourceBundleMessageSource() {
        var messageSource = i18nConfig.messageSource();

        assertNotNull(messageSource);
        assertInstanceOf(ResourceBundleMessageSource.class, messageSource);
    }

    @Test
    void messageSource_configuredWithCorrectBasename() {
        var messageSource = (ResourceBundleMessageSource) i18nConfig.messageSource();

        var basenames = messageSource.getBasenameSet();
        assertTrue(basenames.contains("messages"));
    }

    @Test
    void localeResolver_returnsAcceptHeaderLocaleResolver() {
        var localeResolver = i18nConfig.localeResolver();

        assertNotNull(localeResolver);
        assertInstanceOf(AcceptHeaderLocaleResolver.class, localeResolver);
    }

    @Test
    void messageSource_resolvesKnownMessage() {
        var messageSource = i18nConfig.messageSource();

        var message = messageSource.getMessage("resource.transaction.notFound", new Object[]{"123"}, Locale.ENGLISH);

        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test
    void messageSource_differentLocales_returnDifferentMessages() {
        var messageSource = i18nConfig.messageSource();

        var englishMessage = messageSource.getMessage("resource.transaction.notFound", new Object[]{"123"}, Locale.ENGLISH);
        var portugueseMessage = messageSource.getMessage("resource.transaction.notFound", new Object[]{"123"}, new Locale("pt"));

        assertNotNull(englishMessage);
        assertNotNull(portugueseMessage);
    }

    @Test
    void messageSource_unknownKeyWithArgs_throwsNoSuchMessageException() {
        var messageSource = i18nConfig.messageSource();

        assertThrows(
                org.springframework.context.NoSuchMessageException.class,
                () -> messageSource.getMessage("unknown.key", null, Locale.ENGLISH)
        );
    }
}
