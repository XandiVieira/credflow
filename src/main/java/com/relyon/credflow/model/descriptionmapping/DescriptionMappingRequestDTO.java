package com.relyon.credflow.model.descriptionmapping;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DescriptionMappingRequestDTO {

    @NotBlank(message = "Original description is required")
    private String originalDescription;

    @NotBlank(message = "Simplified description is required")
    private String simplifiedDescription;

    @NotBlank(message = "Category is required")
    private String category;
}