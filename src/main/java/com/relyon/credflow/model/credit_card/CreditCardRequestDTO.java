package com.relyon.credflow.model.credit_card;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreditCardRequestDTO {

    @NotBlank(message = "Nickname is required")
    @Size(min = 1, max = 100, message = "Nickname must be between 1 and 100 characters")
    private String nickname;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    private String brand;

    @NotBlank(message = "Tier is required")
    @Size(max = 50, message = "Tier cannot exceed 50 characters")
    private String tier;

    @NotBlank(message = "Issuer is required")
    @Size(max = 100, message = "Issuer cannot exceed 100 characters")
    private String issuer;

    @NotBlank(message = "Last four digits is required")
    @Pattern(regexp = "^\\d{4}$", message = "Last four digits must be exactly 4 numeric characters")
    private String lastFourDigits;

    @NotNull(message = "Closing day is required")
    @Min(value = 1, message = "Closing day must be between 1 and 31")
    @Max(value = 31, message = "Closing day must be between 1 and 31")
    private Integer closingDay;

    @NotNull(message = "Due day is required")
    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    private Integer dueDay;

    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.00", message = "Credit limit must be non-negative")
    private BigDecimal creditLimit;

    @NotNull(message = "Holder ID is required")
    private Long holderId;

}
