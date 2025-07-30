package com.relyon.credflow.model.descriptionmapping;

import lombok.Data;

@Data
public class DescriptionMappingResponseDTO {
    private Long id;
    private String originalDescription;
    private String simplifiedDescription;
    private String normalizedDescription;
    private String category;
    private Long accountId;
}