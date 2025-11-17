package com.relyon.credflow.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InstallmentConsistencyValidator.class)
@Documented
public @interface InstallmentConsistency {
    String message() default "Current installment must not exceed total installments";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
