package com.relyon.credflow.model.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Account response")
@Data
public class AccountResponseDTO {
    @Schema(example = "1") private Long id;
    @Schema(example = "Main Account") private String name;
    @Schema(example = "Primary expenses") private String description;
}