package com.relyon.credflow.model.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private Long defaultResponsible;
}