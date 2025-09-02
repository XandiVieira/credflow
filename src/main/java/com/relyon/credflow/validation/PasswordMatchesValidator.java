package com.relyon.credflow.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmField;

    @Override
    public void initialize(PasswordMatches ann) {
        this.passwordField = ann.passwordField();
        this.confirmField = ann.confirmField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext ctx) {
        if (value == null) return true;
        try {
            Field p = value.getClass().getDeclaredField(passwordField);
            Field c = value.getClass().getDeclaredField(confirmField);
            p.setAccessible(true);
            c.setAccessible(true);
            Object pv = p.get(value);
            Object cv = c.get(value);
            if (pv == null || cv == null) return true;
            return pv.equals(cv);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }
}