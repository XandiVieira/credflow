package com.relyon.credflow.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.relyon.credflow.service.LocalizedMessageTranslationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private LocalizedMessageTranslationService translationService;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleResourceNotFound_translatesMessageAndReturnsNotFoundStatus() {
        var exception = new ResourceNotFoundException("resource.user.notFound", 123L);
        when(translationService.translateMessage(exception)).thenReturn("User not found");

        var result = handler.handleResourceNotFound(exception);

        assertNotNull(result);
        assertEquals(404, result.getStatus());
        assertEquals("User not found", result.getMessage());
        assertNull(result.getErrors());
        assertNotNull(result.getTimestamp());
        verify(translationService).translateMessage(exception);
    }

    @Test
    void handleResourceAlreadyExists_translatesMessageAndReturnsConflictStatus() {
        var exception = new ResourceAlreadyExistsException("resource.email.exists", "test@test.com");
        when(translationService.translateMessage(exception)).thenReturn("Email already exists");

        var result = handler.handleResourceAlreadyExists(exception);

        assertNotNull(result);
        assertEquals(409, result.getStatus());
        assertEquals("Email already exists", result.getMessage());
        assertNull(result.getErrors());
        verify(translationService).translateMessage(exception);
    }

    @Test
    void handleCsvProcessing_translatesMessageAndReturnsBadRequestStatus() {
        var exception = new CsvProcessingException("csv.invalidFormat");
        when(translationService.translateMessage(exception)).thenReturn("Invalid CSV format");

        var result = handler.handleCsvProcessing(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Invalid CSV format", result.getMessage());
        verify(translationService).translateMessage(exception);
    }

    @Test
    void handlePdfProcessing_translatesMessageAndReturnsBadRequestStatus() {
        var exception = new PdfProcessingException("pdf.parseError");
        when(translationService.translateMessage(exception)).thenReturn("PDF parse error");

        var result = handler.handlePdfProcessing(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("PDF parse error", result.getMessage());
        verify(translationService).translateMessage(exception);
    }

    @Test
    void handleIllegalArgument_usesMessageDirectlyAndReturnsBadRequest() {
        var exception = new IllegalArgumentException("Invalid argument provided");

        var result = handler.handleIllegalArgument(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Invalid argument provided", result.getMessage());
        verifyNoInteractions(translationService);
    }

    @Test
    void handleConstraintViolation_extractsAllViolationMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
        when(violation1.getMessage()).thenReturn("Field must not be null");
        when(violation2.getMessage()).thenReturn("Field must be positive");

        var exception = new ConstraintViolationException(Set.of(violation1, violation2));

        var result = handler.handleConstraintViolation(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNull(result.getMessage());
        assertNotNull(result.getErrors());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("Field must not be null"));
        assertTrue(result.getErrors().contains("Field must be positive"));
    }

    @Test
    void handleAuthExceptions_badCredentials_returnsUnauthorized() {
        var exception = new BadCredentialsException("Bad credentials");
        when(translationService.translateMessage("auth.invalidCredentials")).thenReturn("Invalid credentials");

        var result = handler.handleAuthExceptions(exception);

        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertEquals("Invalid credentials", result.getMessage());
        verify(translationService).translateMessage("auth.invalidCredentials");
    }

    @Test
    void handleAuthExceptions_usernameNotFound_returnsUnauthorized() {
        var exception = new UsernameNotFoundException("User not found");
        when(translationService.translateMessage("auth.invalidCredentials")).thenReturn("Invalid credentials");

        var result = handler.handleAuthExceptions(exception);

        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertEquals("Invalid credentials", result.getMessage());
    }

    @Test
    void handleValidationError_combinesFieldAndGlobalErrors() {
        var exception = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        var fieldError1 = new FieldError("object", "field1", "must not be null");
        var fieldError2 = new FieldError("object", "field2", "must be positive");
        var globalError = new ObjectError("object", "passwords must match");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

        var result = handler.handleValidationError(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNull(result.getMessage());
        assertNotNull(result.getErrors());
        assertEquals(3, result.getErrors().size());
        assertTrue(result.getErrors().contains("must not be null"));
        assertTrue(result.getErrors().contains("must be positive"));
        assertTrue(result.getErrors().contains("passwords must match"));
    }

    @Test
    void handleValidationError_emptyErrors_returnsEmptyList() {
        var exception = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        when(bindingResult.getGlobalErrors()).thenReturn(List.of());

        var result = handler.handleValidationError(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void handleHandlerMethodValidation_extractsNonNullMessages() {
        var exception = mock(HandlerMethodValidationException.class);
        var error1 = mock(org.springframework.context.MessageSourceResolvable.class);
        var error2 = mock(org.springframework.context.MessageSourceResolvable.class);
        var error3 = mock(org.springframework.context.MessageSourceResolvable.class);
        when(error1.getDefaultMessage()).thenReturn("Error 1");
        when(error2.getDefaultMessage()).thenReturn(null);
        when(error3.getDefaultMessage()).thenReturn("Error 3");
        doReturn(List.of(error1, error2, error3)).when(exception).getAllErrors();

        var result = handler.handleHandlerMethodValidation(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNotNull(result.getErrors());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("Error 1"));
        assertTrue(result.getErrors().contains("Error 3"));
    }

    @Test
    void handleInvalidSort_propertyReferenceException_translatesWithPropertyName() {
        var exception = mock(PropertyReferenceException.class);
        when(exception.getPropertyName()).thenReturn("invalidField");
        when(translationService.translateMessage("query.invalidSort", "invalidField"))
                .thenReturn("Invalid sort property: invalidField");

        var result = handler.handleInvalidSort(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Invalid sort property: invalidField", result.getMessage());
    }

    @Test
    void handleInvalidSort_invalidDataAccessApiUsageException_translatesWithMessage() {
        var exception = new InvalidDataAccessApiUsageException("Some data access error");
        when(translationService.translateMessage("query.invalidParameter", "Some data access error"))
                .thenReturn("Invalid query parameter");

        var result = handler.handleInvalidSort(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Invalid query parameter", result.getMessage());
    }

    @Test
    void handleBindException_formatsFieldErrors() {
        var bindingResult = mock(BindingResult.class);
        var fieldError1 = new FieldError("form", "username", "must not be blank");
        var fieldError2 = new FieldError("form", "email", "must be a valid email");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        var exception = new BindException(bindingResult);

        var result = handler.handleBindException(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertNotNull(result.getErrors());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains("username: must not be blank"));
        assertTrue(result.getErrors().contains("email: must be a valid email"));
    }

    @Test
    void handleMissingRequestParam_translatesWithParameterName() {
        var exception = new MissingServletRequestParameterException("page", "int");
        when(translationService.translateMessage("query.missingParameter", "page"))
                .thenReturn("Missing required parameter: page");

        var result = handler.handleMissingRequestParam(exception);

        assertNotNull(result);
        assertEquals(400, result.getStatus());
        assertEquals("Missing required parameter: page", result.getMessage());
    }

    @Test
    void handleOptimisticLocking_translatesConflictMessage() {
        var exception = new ObjectOptimisticLockingFailureException("Entity", 1L);
        when(translationService.translateMessage("optimisticLock.conflict"))
                .thenReturn("The resource was modified by another user");

        var result = handler.handleOptimisticLocking(exception);

        assertNotNull(result);
        assertEquals(409, result.getStatus());
        assertEquals("The resource was modified by another user", result.getMessage());
    }

    @Test
    void handleGeneric_returnsInternalServerError() {
        var exception = new RuntimeException("Unexpected error occurred");

        var result = handler.handleGeneric(exception);

        assertNotNull(result);
        assertEquals(500, result.getStatus());
        assertEquals("Unexpected error occurred", result.getMessage());
    }

    @Test
    void handleGeneric_nullMessage_handlesGracefully() {
        var exception = new RuntimeException((String) null);

        var result = handler.handleGeneric(exception);

        assertNotNull(result);
        assertEquals(500, result.getStatus());
        assertNull(result.getMessage());
    }
}
