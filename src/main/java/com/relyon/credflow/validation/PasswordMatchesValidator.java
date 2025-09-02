package com.relyon.credflow.validation;

import com.relyon.credflow.model.user.UserRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserRequestDTO> {
    @Override
    public boolean isValid(UserRequestDTO dto, ConstraintValidatorContext ctx) {
        if (dto == null) return true;
        var p = dto.getPassword();
        var c = dto.getConfirmPassword();
        if (p == null || c == null || p.isBlank() || c.isBlank()) return true;
        return p.equals(c);
    }
}