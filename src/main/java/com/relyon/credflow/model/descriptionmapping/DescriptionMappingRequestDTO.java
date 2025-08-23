package com.relyon.credflow.model.descriptionmapping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DescriptionMappingRequestDTO {

    @NotBlank(message = "Original description is required")
    private String originalDescription;

    @NotBlank(message = "Simplified description is required")
    private String simplifiedDescription;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}