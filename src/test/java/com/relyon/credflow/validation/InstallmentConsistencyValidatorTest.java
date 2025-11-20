package com.relyon.credflow.validation;

import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentConsistencyValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    private InstallmentConsistencyValidator validator;

    @BeforeEach
    void setup() {
        validator = new InstallmentConsistencyValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @Test
    void isValid_whenDTOIsNull_shouldReturnTrue() {
        var result = validator.isValid(null, context);
        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenTypeIsNotInstallment_shouldReturnTrue() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.ONE_TIME);
        dto.setCurrentInstallment(5);
        dto.setTotalInstallments(3);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
        verifyNoInteractions(context);
    }

    @Test
    void isValid_whenTypeIsRecurring_shouldReturnTrue() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.RECURRING);
        dto.setCurrentInstallment(null);
        dto.setTotalInstallments(null);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenTypeIsInstallmentAndCurrentIsNull_shouldReturnFalse() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(null);
        dto.setTotalInstallments(12);

        var result = validator.isValid(dto, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Both current and total installments are required for INSTALLMENT type"
        );
    }

    @Test
    void isValid_whenTypeIsInstallmentAndTotalIsNull_shouldReturnFalse() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(3);
        dto.setTotalInstallments(null);

        var result = validator.isValid(dto, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Both current and total installments are required for INSTALLMENT type"
        );
    }

    @Test
    void isValid_whenCurrentExceedsTotal_shouldReturnFalse() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(13);
        dto.setTotalInstallments(12);

        var result = validator.isValid(dto, context);

        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Current installment (13) cannot exceed total installments (12)"
        );
    }

    @Test
    void isValid_whenCurrentEqualsTotal_shouldReturnTrue() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(12);
        dto.setTotalInstallments(12);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
        verifyNoInteractions(context);
    }

    @Test
    void isValid_whenCurrentLessThanTotal_shouldReturnTrue() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(5);
        dto.setTotalInstallments(12);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
        verifyNoInteractions(context);
    }

    @Test
    void isValid_whenCurrentIsOne_shouldReturnTrue() {
        var dto = new TransactionRequestDTO();
        dto.setTransactionType(TransactionType.INSTALLMENT);
        dto.setCurrentInstallment(1);
        dto.setTotalInstallments(12);

        var result = validator.isValid(dto, context);

        assertThat(result).isTrue();
    }
}
