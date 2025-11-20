package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalizedMessageTranslationServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private LocalizedMessageTranslationService service;

    @Test
    void translateMessage_withDomainException_shouldResolveMessageKeyWithArguments() {
        var messageKey = "error.notFound";
        var arguments = new Object[]{"User", 123L};
        var exception = new ResourceNotFoundException(messageKey, arguments);

        when(messageSource.getMessage(messageKey, arguments, LocaleContextHolder.getLocale()))
                .thenReturn("User with id 123 not found");

        var result = service.translateMessage(exception);

        assertThat(result).isEqualTo("User with id 123 not found");
        verify(messageSource).getMessage(messageKey, arguments, LocaleContextHolder.getLocale());
    }

    @Test
    void translateMessage_withDomainExceptionNoArguments_shouldResolveMessageKey() {
        var messageKey = "error.generic";
        var exception = new ResourceNotFoundException(messageKey);

        when(messageSource.getMessage(messageKey, new Object[]{}, LocaleContextHolder.getLocale()))
                .thenReturn("An error occurred");

        var result = service.translateMessage(exception);

        assertThat(result).isEqualTo("An error occurred");
        verify(messageSource).getMessage(messageKey, new Object[]{}, LocaleContextHolder.getLocale());
    }

    @Test
    void translateMessage_withMessageKeyAndArguments_shouldResolveMessage() {
        var messageKey = "validation.min";
        var arguments = new Object[]{10};

        when(messageSource.getMessage(messageKey, arguments, LocaleContextHolder.getLocale()))
                .thenReturn("Value must be at least 10");

        var result = service.translateMessage(messageKey, arguments);

        assertThat(result).isEqualTo("Value must be at least 10");
        verify(messageSource).getMessage(messageKey, arguments, LocaleContextHolder.getLocale());
    }

    @Test
    void translateMessage_withMessageKeyNoArguments_shouldResolveMessage() {
        var messageKey = "success.created";

        when(messageSource.getMessage(messageKey, new Object[]{}, LocaleContextHolder.getLocale()))
                .thenReturn("Resource created successfully");

        var result = service.translateMessage(messageKey);

        assertThat(result).isEqualTo("Resource created successfully");
        verify(messageSource).getMessage(messageKey, new Object[]{}, LocaleContextHolder.getLocale());
    }

    @Test
    void translateMessage_shouldUseCurrentLocaleFromContext() {
        var messageKey = "greeting.hello";
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        when(messageSource.getMessage(messageKey, new Object[]{}, Locale.ENGLISH))
                .thenReturn("Hello");

        var result = service.translateMessage(messageKey);

        assertThat(result).isEqualTo("Hello");
        verify(messageSource).getMessage(messageKey, new Object[]{}, Locale.ENGLISH);

        LocaleContextHolder.resetLocaleContext();
    }
}
