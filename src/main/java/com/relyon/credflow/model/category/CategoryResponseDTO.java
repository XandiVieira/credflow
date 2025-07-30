package com.relyon.credflow.model.category;

import lombok.Data;

@Data
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String defaultResponsible;
}