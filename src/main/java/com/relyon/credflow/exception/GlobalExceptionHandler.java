package com.relyon.credflow.exception;

import com.relyon.credflow.service.LocalizedMessageTranslationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.*;

/**
 * Global exception handler that uses i18n message translation for consistent, localized error responses.
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LocalizedMessageTranslationService translationService;

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorBody handleResourceNotFound(DomainException cause) {
        log.error("NOT_FOUND", cause);
        var translated = translationService.translateMessage(cause);
        return ErrorBody.from(translated, NOT_FOUND.value());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(CONFLICT)
    public ErrorBody handleResourceAlreadyExists(DomainException cause) {
        log.error("CONFLICT", cause);
        var translated = translationService.translateMessage(cause);
        return ErrorBody.from(translated, CONFLICT.value());
    }

    @ExceptionHandler(CsvProcessingException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleCsvProcessing(DomainException cause) {
        log.error("BAD_REQUEST - CSV Processing", cause);
        var translated = translationService.translateMessage(cause);
        return ErrorBody.from(translated, BAD_REQUEST.value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleIllegalArgument(IllegalArgumentException cause) {
        log.error("BAD_REQUEST - Illegal Argument", cause);
        // For IllegalArgumentException, use the message directly since it's not a DomainException
        return ErrorBody.from(cause.getMessage(), BAD_REQUEST.value());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleConstraintViolation(ConstraintViolationException cause) {
        log.error("BAD_REQUEST - Constraint Violation", cause);
        var errors = cause.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .toList();
        return ErrorBody.from(errors, BAD_REQUEST.value());
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    @ResponseStatus(UNAUTHORIZED)
    public ErrorBody handleAuthExceptions(Exception cause) {
        log.error("UNAUTHORIZED - Authentication Failed", cause);
        var message = translationService.translateMessage("auth.invalidCredentials");
        return ErrorBody.from(message, UNAUTHORIZED.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleValidationError(MethodArgumentNotValidException cause) {
        log.error("BAD_REQUEST - Validation Error", cause);
        var fieldErrors = cause.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        var globalErrors = cause.getBindingResult().getGlobalErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        var allErrors = new java.util.ArrayList<String>(fieldErrors.size() + globalErrors.size());
        allErrors.addAll(fieldErrors);
        allErrors.addAll(globalErrors);

        return ErrorBody.from(allErrors, BAD_REQUEST.value());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleHandlerMethodValidation(HandlerMethodValidationException cause) {
        log.error("BAD_REQUEST - Handler Method Validation", cause);
        var errors = cause.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .toList();
        return ErrorBody.from(errors, BAD_REQUEST.value());
    }

    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleInvalidSort(RuntimeException cause) {
        log.error("BAD_REQUEST - Invalid Query/Sort", cause);
        String message;
        if (cause instanceof PropertyReferenceException pre) {
            message = translationService.translateMessage("query.invalidSort", pre.getPropertyName());
        } else {
            message = translationService.translateMessage("query.invalidParameter", cause.getMessage());
        }
        return ErrorBody.from(message, BAD_REQUEST.value());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleBindException(BindException cause) {
        log.error("BAD_REQUEST - Bind Exception", cause);
        var errors = cause.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ErrorBody.from(errors, BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorBody handleMissingRequestParam(MissingServletRequestParameterException cause) {
        log.error("BAD_REQUEST - Missing Request Parameter", cause);
        var message = translationService.translateMessage("query.missingParameter", cause.getParameterName());
        return ErrorBody.from(message, BAD_REQUEST.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorBody handleGeneric(Exception cause) {
        log.error("INTERNAL_SERVER_ERROR - Unexpected Exception", cause);
        return ErrorBody.from(cause.getMessage(), INTERNAL_SERVER_ERROR.value());
    }
}
