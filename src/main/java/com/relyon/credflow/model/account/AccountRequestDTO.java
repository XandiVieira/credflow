package com.relyon.credflow.model.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Account update/create payload")
@Data
public class AccountRequestDTO {
    @Schema(example = "Main Account", description = "Display name")
    private String name;
    @Schema(example = "Primary expenses", description = "Optional description")
    private String description;
}