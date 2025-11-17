package com.relyon.credflow.validation;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordMatchesValidatorTest {

    @Mock
    private PasswordMatches annotation;

    @Mock
    private ConstraintValidatorContext context;

    private PasswordMatchesValidator validator;

    @BeforeEach
    void setup() {
        validator = new PasswordMatchesValidator();
    }

    @Test
    void isValid_whenObjectIsNull_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);
        var result = validator.isValid(null, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenPasswordsMatch_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword("SecurePass123!");
        dto.setConfirmPassword("SecurePass123!");

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenPasswordsDontMatch_shouldReturnFalse() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword("SecurePass123!");
        dto.setConfirmPassword("DifferentPass456!");

        var result = validator.isValid(dto, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenPasswordIsNull_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword(null);
        dto.setConfirmPassword("password");

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenConfirmPasswordIsNull_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword("password");
        dto.setConfirmPassword(null);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenBothPasswordsAreNull_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword(null);
        dto.setConfirmPassword(null);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenFieldDoesNotExist_shouldReturnFalse() {
        when(annotation.passwordField()).thenReturn("nonExistentField");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword("password");
        dto.setConfirmPassword("password");

        var result = validator.isValid(dto, context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenPasswordsAreEmpty_shouldReturnTrue() {
        when(annotation.passwordField()).thenReturn("password");
        when(annotation.confirmField()).thenReturn("confirmPassword");

        validator.initialize(annotation);

        var dto = new TestDTO();
        dto.setPassword("");
        dto.setConfirmPassword("");

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Data
    static class TestDTO {
        private String password;
        private String confirmPassword;
    }
}
