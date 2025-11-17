package com.relyon.credflow.model.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Account update/create payload")
@Data
public class AccountRequestDTO {
    @Schema(example = "Main Account", description = "Display name")
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
    private String name;

    @Schema(example = "Primary expenses", description = "Optional description")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}