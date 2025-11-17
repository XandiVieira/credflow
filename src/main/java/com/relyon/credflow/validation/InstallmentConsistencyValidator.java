package com.relyon.credflow.validation;

import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class InstallmentConsistencyValidator implements ConstraintValidator<InstallmentConsistency, TransactionRequestDTO> {

    @Override
    public boolean isValid(TransactionRequestDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        var type = dto.getTransactionType();
        var current = dto.getCurrentInstallment();
        var total = dto.getTotalInstallments();

        if (type != TransactionType.INSTALLMENT) {
            return true;
        }

        if (current == null || total == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Both current and total installments are required for INSTALLMENT type")
                    .addConstraintViolation();
            return false;
        }

        if (current > total) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Current installment (" + current + ") cannot exceed total installments (" + total + ")")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
