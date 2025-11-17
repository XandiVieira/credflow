package com.relyon.credflow.model.descriptionmapping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DescriptionMappingRequestDTO {

    @NotBlank(message = "Original description is required")
    @Size(max = 500, message = "Original description cannot exceed 500 characters")
    private String originalDescription;

    @NotBlank(message = "Simplified description is required")
    @Size(max = 500, message = "Simplified description cannot exceed 500 characters")
    private String simplifiedDescription;

    @NotNull(message = "Category id is required")
    private Long categoryId;
}