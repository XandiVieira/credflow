package com.relyon.credflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int min;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;

    @Override
    public void initialize(StrongPassword ann) {
        this.min = ann.min();
        this.requireUppercase = ann.requireUppercase();
        this.requireLowercase = ann.requireLowercase();
        this.requireDigit = ann.requireDigit();
        this.requireSpecial = ann.requireSpecial();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true;
        if (value.length() < min) return false;
        if (requireUppercase && !value.chars().anyMatch(Character::isUpperCase)) return false;
        if (requireLowercase && !value.chars().anyMatch(Character::isLowerCase)) return false;
        if (requireDigit && !value.chars().anyMatch(Character::isDigit)) return false;
        if (requireSpecial && value.chars().noneMatch(c -> "!@#$%^&*()[]{}-_+=|\\:;\"'<>,.?/`~".indexOf(c) >= 0))
            return false;
        return true;
    }
}