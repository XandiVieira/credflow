package com.relyon.credflow.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StrongPasswordValidatorTest {

    @Mock
    private StrongPassword annotation;

    @Mock
    private ConstraintValidatorContext context;

    private StrongPasswordValidator validator;

    @BeforeEach
    void setup() {
        validator = new StrongPasswordValidator();
    }

    @Test
    void isValid_whenPasswordIsNull_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid(null, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenPasswordTooShort_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("short", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenPasswordMeetsMinLength_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("password", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenRequireUppercaseAndMissing_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("password123", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenRequireUppercaseAndPresent_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("Password123", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenRequireLowercaseAndMissing_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("PASSWORD123", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenRequireLowercaseAndPresent_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("password123", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenRequireDigitAndMissing_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("Password", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenRequireDigitAndPresent_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecial()).thenReturn(false);

        validator.initialize(annotation);
        var result = validator.isValid("Password123", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenRequireSpecialAndMissing_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("Password123", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenRequireSpecialAndPresent_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("Password123!", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenAllRequirementsMet_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("Password123!", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenAllRequirementsNotMet_shouldReturnFalse() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(true);
        when(annotation.requireLowercase()).thenReturn(true);
        when(annotation.requireDigit()).thenReturn(true);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("password", context);

        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenSpecialCharacterIsAtSymbol_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("password@", context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenSpecialCharacterIsHashtag_shouldReturnTrue() {
        when(annotation.min()).thenReturn(8);
        when(annotation.requireUppercase()).thenReturn(false);
        when(annotation.requireLowercase()).thenReturn(false);
        when(annotation.requireDigit()).thenReturn(false);
        when(annotation.requireSpecial()).thenReturn(true);

        validator.initialize(annotation);
        var result = validator.isValid("password#", context);

        assertThat(result).isTrue();
    }
}
