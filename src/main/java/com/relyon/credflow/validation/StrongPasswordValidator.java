package com.relyon.credflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$";
    @Override public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return false;
        return value.matches(REGEX);
    }
}